package cl.acis.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.acis.app.data.remote.toCreateRequestDto
import cl.acis.app.data.remote.toUpdateRequestDto
import cl.acis.app.model.Order
import cl.acis.app.model.Rules
import cl.acis.app.model.Stage
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface OrderRepository {
    suspend fun all(): List<Order>
    suspend fun read(): OrderReadResult = OrderReadResult(all())
    suspend fun save(order: Order): Long
    suspend fun delete(id: Long)
}

/** SQLite remains the local source of truth. No network work is started here. */
class LocalOrderRepository(
    context: Context,
    databaseName: String = "acis.db"
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION), OrderRepository, OrderSyncStore {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                payload TEXT NOT NULL,
                client_order_id TEXT NOT NULL UNIQUE,
                remote_order_id INTEGER UNIQUE,
                sync_eligible INTEGER NOT NULL DEFAULT 1
            )""".trimIndent()
        )
        createOutbox(db)
    }

    /** Existing rows receive local metadata only and are intentionally not queued for upload. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE orders ADD COLUMN client_order_id TEXT")
            db.execSQL("ALTER TABLE orders ADD COLUMN remote_order_id INTEGER")
            db.execSQL("ALTER TABLE orders ADD COLUMN sync_eligible INTEGER NOT NULL DEFAULT 0")
            val orderIds = mutableListOf<Long>()
            db.rawQuery("SELECT id FROM orders", null).use { cursor ->
                while (cursor.moveToNext()) orderIds += cursor.getLong(0)
            }
            orderIds.forEach { orderId ->
                db.execSQL(
                    "UPDATE orders SET client_order_id=? WHERE id=?",
                    arrayOf<Any>(UUID.randomUUID().toString(), orderId)
                )
            }
            db.execSQL("CREATE UNIQUE INDEX index_orders_client_order_id ON orders(client_order_id)")
            db.execSQL("CREATE UNIQUE INDEX index_orders_remote_order_id ON orders(remote_order_id)")
            createOutbox(db)
        }
    }

    private fun createOutbox(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE order_sync_outbox (
                sequence INTEGER PRIMARY KEY AUTOINCREMENT,
                local_order_id INTEGER NOT NULL,
                kind TEXT NOT NULL CHECK(kind IN ('CREATE','UPDATE')),
                request_json TEXT NOT NULL,
                FOREIGN KEY(local_order_id) REFERENCES orders(id) ON DELETE CASCADE
            )""".trimIndent()
        )
    }

    override suspend fun all(): List<Order> = read().orders

    override suspend fun read(): OrderReadResult = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT id,payload FROM orders ORDER BY id DESC", null).use { cursor ->
            readOrderRows(sequence {
                while (cursor.moveToNext()) yield(cursor.getLong(0) to cursor.getString(1))
            })
        }
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        check(writableDatabase.delete("orders", "id=?", arrayOf(id.toString())) == 1) {
            "Orden no disponible"
        }
    }

    override suspend fun save(order: Order): Long = withContext(Dispatchers.IO) {
        require(Rules.photoLimit(order).isEmpty()) { "Máximo 10 fotografías" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val payload = encodeOrder(order)
            val isNew = order.id == 0L
            val id: Long
            val metadata: OrderSyncMetadata
            val kind: PendingSyncKind
            var shouldEnqueue = isNew

            if (isNew) {
                val clientOrderId = UUID.randomUUID().toString()
                val values = ContentValues().apply {
                    put("payload", payload)
                    put("client_order_id", clientOrderId)
                    put("sync_eligible", 1)
                }
                id = db.insertOrThrow("orders", null, values)
                metadata = OrderSyncMetadata(clientOrderId, null, true)
                kind = PendingSyncKind.CREATE
            } else {
                metadata = readMetadata(db, order.id) ?: error("Orden no disponible")
                val oldPayload = db.rawQuery(
                    "SELECT payload FROM orders WHERE id=?",
                    arrayOf(order.id.toString())
                ).use { cursor ->
                    check(cursor.moveToFirst()) { "Orden no disponible" }
                    cursor.getString(0)
                }
                val values = ContentValues().apply { put("payload", payload) }
                check(db.update("orders", values, "id=?", arrayOf(order.id.toString())) == 1) {
                    "Orden no disponible"
                }
                id = order.id
                kind = PendingSyncKind.UPDATE
                shouldEnqueue = metadata.eligibleForSync && oldPayload != payload
            }

            if (metadata.eligibleForSync && shouldEnqueue) {
                val request = if (kind == PendingSyncKind.CREATE) {
                    order.toCreateRequestDto(metadata.clientOrderId)
                } else {
                    order.toUpdateRequestDto(metadata.clientOrderId)
                }
                enqueue(db, id, kind, Gson().toJson(request))
            }
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun nextPendingSync(): PendingOrderSync? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            """SELECT q.sequence, q.local_order_id, o.client_order_id, o.remote_order_id,
                q.kind, q.request_json FROM order_sync_outbox q
                JOIN orders o ON o.id=q.local_order_id ORDER BY q.sequence LIMIT 1""".trimIndent(),
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else PendingOrderSync(
                sequence = cursor.getLong(0),
                localOrderId = cursor.getLong(1),
                clientOrderId = cursor.getString(2),
                remoteOrderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                kind = PendingSyncKind.valueOf(cursor.getString(4)),
                requestJson = cursor.getString(5)
            )
        }
    }

    override suspend fun completePendingSync(pending: PendingOrderSync, remoteOrderId: Long) = withContext(Dispatchers.IO) {
        require(remoteOrderId > 0) { "El ID remoto debe ser positivo" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT sequence FROM order_sync_outbox ORDER BY sequence LIMIT 1", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) == pending.sequence) {
                    "Las operaciones deben confirmarse en el orden de la cola"
                }
            }
            val metadata = readMetadata(db, pending.localOrderId) ?: error("Orden local no disponible")
            check(metadata.clientOrderId == pending.clientOrderId) {
                "La clave local de idempotencia no coincide"
            }
            when (pending.kind) {
                PendingSyncKind.CREATE -> {
                    check(metadata.remoteOrderId == null || metadata.remoteOrderId == remoteOrderId) {
                        "La orden ya está asociada a otro ID remoto"
                    }
                    val values = ContentValues().apply { put("remote_order_id", remoteOrderId) }
                    check(db.update("orders", values, "id=?", arrayOf(pending.localOrderId.toString())) == 1)
                }
                PendingSyncKind.UPDATE -> check(metadata.remoteOrderId == remoteOrderId) {
                    "La respuesta no corresponde al ID remoto asociado"
                }
            }
            check(db.delete("order_sync_outbox", "sequence=?", arrayOf(pending.sequence.toString())) == 1) {
                "La operación pendiente ya no está disponible"
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun pendingSyncCount(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM order_sync_outbox", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    suspend fun syncMetadata(localOrderId: Long): OrderSyncMetadata? = withContext(Dispatchers.IO) {
        readMetadata(readableDatabase, localOrderId)
    }

    private fun readMetadata(db: SQLiteDatabase, localOrderId: Long): OrderSyncMetadata? =
        db.rawQuery(
            "SELECT client_order_id, remote_order_id, sync_eligible FROM orders WHERE id=?",
            arrayOf(localOrderId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else OrderSyncMetadata(
                clientOrderId = cursor.getString(0),
                remoteOrderId = if (cursor.isNull(1)) null else cursor.getLong(1),
                eligibleForSync = cursor.getInt(2) == 1
            )
        }

    private fun enqueue(db: SQLiteDatabase, id: Long, kind: PendingSyncKind, requestJson: String) {
        val values = ContentValues().apply {
            put("local_order_id", id)
            put("kind", kind.name)
            put("request_json", requestJson)
        }
        db.insertOrThrow("order_sync_outbox", null, values)
    }

    private fun encodeOrder(order: Order): String = JSONObject().apply {
        put("customerConfirmed", order.customerConfirmed)
        put("site", order.site); put("address", order.address); put("equipment", order.equipment)
        put("technician", order.technician); put("priority", order.priority); put("type", order.type)
        put("problem", order.problem); put("stage", order.stage.name); put("diagnosis", order.diagnosis)
        put("work", order.work); put("materials", order.materials)
        put("photo", order.evidencePhotos.firstOrNull().orEmpty())
        put("photos", JSONArray(order.evidencePhotos)); put("location", order.location)
        put("arrival", order.arrival); put("departure", order.departure)
        put("customer", order.customer); put("feedback", order.feedback)
    }.toString()

    private companion object {
        const val DATABASE_VERSION = 2
    }
}
