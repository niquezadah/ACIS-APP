package cl.acis.app.data

import android.database.sqlite.SQLiteDatabase
import cl.acis.app.data.remote.OrderRequestDto
import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OrderSyncPersistenceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "sync-test-${UUID.randomUUID()}.db"
    private lateinit var repository: LocalOrderRepository

    @Before
    fun openRepository() {
        repository = LocalOrderRepository(context, databaseName)
    }

    @After
    fun closeAndDeleteOnlyTestDatabase() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun newOrdersGetPersistentClientIdAndOrderedPhotoFreeSnapshots() = runBlocking {
        val source = Order(
            site = "Local ficticio",
            address = "Calle de prueba",
            equipment = "Equipo ficticio",
            technician = "Técnico",
            problem = "Falla ficticia",
            photos = listOf("/private/test-one.jpg", "/private/test-two.jpg")
        )
        val localId = repository.save(source)
        val metadata = repository.syncMetadata(localId)!!
        val create = repository.nextPendingSync()!!
        val createRequest = Gson().fromJson(create.requestJson, OrderRequestDto::class.java)

        assertTrue(metadata.eligibleForSync)
        assertEquals(metadata.clientOrderId, UUID.fromString(metadata.clientOrderId).toString())
        assertEquals(localId, create.localOrderId)
        assertEquals(metadata.clientOrderId, create.clientOrderId)
        assertEquals(PendingSyncKind.CREATE, create.kind)
        assertEquals(metadata.clientOrderId, createRequest.clientOrderId)
        assertEquals(Stage.ASSIGNED, createRequest.stage)
        assertEquals(2, createRequest.evidenceCount)
        assertFalse(create.requestJson.contains("photo"))
        assertFalse(create.requestJson.contains("/private/"))

        repository.save(source.copy(id = localId, stage = Stage.ACCEPTED))
        assertEquals(2, repository.pendingSyncCount())
        repository.completePendingSync(create, remoteOrderId = 905)

        val update = repository.nextPendingSync()!!
        val updateRequest = Gson().fromJson(update.requestJson, OrderRequestDto::class.java)
        assertEquals(PendingSyncKind.UPDATE, update.kind)
        assertEquals(905L, update.remoteOrderId)
        assertEquals(Stage.ACCEPTED, updateRequest.stage)
        assertNull(updateRequest.id)
        assertEquals(905L, repository.syncMetadata(localId)!!.remoteOrderId)
    }

    @Test
    fun versionOneMigrationPreservesExistingRowsAndDoesNotQueueThem() = runBlocking {
        repository.close()
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val legacyPayload = """
            {"site":"Antiguo","address":"Calle","equipment":"Equipo","technician":"Ana",
             "priority":"Normal","type":"Preventiva","problem":"Falla antigua","stage":"DONE",
             "diagnosis":"Causa antigua","work":"Trabajo antiguo","materials":"","photo":"private.jpg",
             "photos":["private.jpg"],"location":"","arrival":1,"departure":2,"customer":"Cliente",
             "feedback":"","customerConfirmed":false}
        """.trimIndent()
        val secondLegacyPayload = """
            {"site":"Segundo local","address":"Pasaje 22","equipment":"Refrigerador","technician":"Luis",
             "priority":"Alta","type":"Correctiva","problem":"Falla del motor","stage":"ACCEPTED",
             "diagnosis":"","work":"","materials":"Repuesto ficticio","photo":"","photos":[],
             "location":"-33.4,-70.6","arrival":171,"departure":0,"customer":"","feedback":"","customerConfirmed":false}
        """.trimIndent()
        val oldDb = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            oldDb.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)")
            oldDb.execSQL("INSERT INTO orders(id,payload) VALUES(41,?)", arrayOf(legacyPayload))
            oldDb.execSQL("INSERT INTO orders(id,payload) VALUES(12,?)", arrayOf(secondLegacyPayload))
            oldDb.version = 1
        } finally {
            oldDb.close()
        }

        repository = LocalOrderRepository(context, databaseName)
        val migratedOrders = repository.all().associateBy { it.id }
        val loaded = migratedOrders[41L]!!
        val secondLoaded = migratedOrders[12L]!!
        val metadata = repository.syncMetadata(41)!!

        assertEquals(41L, loaded.id)
        assertEquals("Antiguo", loaded.site)
        assertEquals("Calle", loaded.address)
        assertEquals("Equipo", loaded.equipment)
        assertEquals(Stage.DONE, loaded.stage)
        assertEquals(listOf("private.jpg"), loaded.evidencePhotos)
        assertEquals(12L, secondLoaded.id)
        assertEquals("Segundo local", secondLoaded.site)
        assertEquals("Pasaje 22", secondLoaded.address)
        assertEquals(Stage.ACCEPTED, secondLoaded.stage)
        assertEquals("-33.4,-70.6", secondLoaded.location)
        assertFalse(metadata.eligibleForSync)
        assertNull(metadata.remoteOrderId)
        assertEquals(metadata.clientOrderId, UUID.fromString(metadata.clientOrderId).toString())
        assertEquals(0, repository.pendingSyncCount())

        repository.readableDatabase.rawQuery("SELECT id,payload FROM orders", null).use { cursor ->
            val migratedPayloads = mutableMapOf<Long, String>()
            while (cursor.moveToNext()) migratedPayloads[cursor.getLong(0)] = cursor.getString(1)
            assertEquals(mapOf(41L to legacyPayload, 12L to secondLegacyPayload), migratedPayloads)
        }
        repository.save(loaded.copy(site = "Cambio local solamente"))
        assertEquals(0, repository.pendingSyncCount())
        assertEquals("Segundo local", repository.all().single { it.id == 12L }.site)
    }
}
