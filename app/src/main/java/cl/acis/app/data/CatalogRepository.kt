package cl.acis.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteOpenHelper
import cl.acis.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

interface CatalogRepository {
    suspend fun read(): Catalog
    suspend fun saveTechnician(value: Technician)
    suspend fun saveSite(value: Site)
}

// Comparte la conexión y la versión de base de datos con las órdenes.
class LocalCatalogRepository(private val database: SQLiteOpenHelper) : CatalogRepository {
    override suspend fun read(): Catalog = withContext(Dispatchers.IO) { readCatalog(database.readableDatabase) }

    override suspend fun saveTechnician(value: Technician) = withContext(Dispatchers.IO) {
        require(CatalogRules.technician(value).isEmpty())
        val values = ContentValues().apply {
            put("name", value.name.trim())
            put("email", value.email.trim().lowercase(Locale.ROOT))
            put("active", if (value.active) 1 else 0)
        }
        try { save("technicians", value.id, values) }
        catch (_: SQLiteConstraintException) { throw CatalogConflictException("email") }
    }

    override suspend fun saveSite(value: Site) = withContext(Dispatchers.IO) {
        require(CatalogRules.site(value).isEmpty())
        val values = ContentValues().apply {
            put("name", value.name.trim()); put("address", value.address.trim())
            put("client", value.client.trim()); put("active", if (value.active) 1 else 0)
        }
        try { save("sites", value.id, values) }
        catch (_: SQLiteConstraintException) { throw CatalogConflictException("name") }
    }

    private fun save(table: String, id: String, values: ContentValues) {
        val db = database.writableDatabase
        if (id.isBlank()) {
            values.put("id", UUID.randomUUID().toString())
            db.insertOrThrow(table, null, values)
        } else check(db.update(table, values, "id=?", arrayOf(id)) == 1) { "Registro no disponible" }
    }
}

internal fun createCatalogTables(db: SQLiteDatabase) {
    db.execSQL("CREATE TABLE technicians (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL COLLATE NOCASE UNIQUE, active INTEGER NOT NULL CHECK(active IN (0,1)))")
    db.execSQL("CREATE TABLE sites (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL COLLATE NOCASE, address TEXT NOT NULL COLLATE NOCASE, client TEXT NOT NULL, active INTEGER NOT NULL CHECK(active IN (0,1)), UNIQUE(name,address))")
}

internal fun readCatalog(db: SQLiteDatabase): Catalog {
    val technicians = db.rawQuery("SELECT id,name,email,active FROM technicians ORDER BY name COLLATE NOCASE,id", null).use { c ->
        buildList { while (c.moveToNext()) add(Technician(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1)) }
    }
    val sites = db.rawQuery("SELECT id,name,address,client,active FROM sites ORDER BY name COLLATE NOCASE,id", null).use { c ->
        buildList { while (c.moveToNext()) add(Site(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4) == 1)) }
    }
    return Catalog(technicians, sites)
}
