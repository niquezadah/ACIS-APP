package cl.acis.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.acis.app.model.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private suspend fun scenario(test: suspend (LocalOrderRepository, LocalCatalogRepository, String) -> Unit) {
        val name = "catalog_${UUID.randomUUID()}.db"
        val orders = LocalOrderRepository(context, name)
        try { test(orders, LocalCatalogRepository(orders), name) }
        finally { orders.close(); context.deleteDatabase(name) }
    }

    @Test fun versionOneMigrationPreservesRawRowsAndAllowsNewCatalogs() = runBlocking {
        val name = "migration_${UUID.randomUUID()}.db"
        val original = "{registro antiguo sin modificar"
        val old = object : SQLiteOpenHelper(context, name, null, 1) {
            override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL)") }
            override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = error("No corresponde")
        }
        old.writableDatabase.insertOrThrow("orders", null, ContentValues().apply { put("id", 25); put("payload", original) })
        old.close()
        try {
            LocalOrderRepository(context, name).use { repo ->
                assertEquals(3, repo.readableDatabase.version)
                repo.readableDatabase.rawQuery("SELECT id,payload FROM orders", null).use { c ->
                    assertTrue(c.moveToFirst()); assertEquals(25L, c.getLong(0)); assertEquals(original, c.getString(1))
                }
                assertEquals(listOf(25L), repo.read().unreadableOrderIds)
                val catalogs = LocalCatalogRepository(repo)
                assertEquals(Catalog(), catalogs.read())
                catalogs.saveTechnician(Technician(name = "Ana Soto", email = "ana@example.com"))
                assertEquals(1, catalogs.read().technicians.size)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun recordsSurviveReopeningAndCanBeEditedWithoutChangingId() = runBlocking { scenario { orders, catalog, name ->
        catalog.saveTechnician(Technician(name = " Ana Soto ", email = " ANA@EXAMPLE.COM "))
        catalog.saveSite(Site(name = " Centro ", address = " Calle Uno 123 ", client = " ACIS "))
        val first = catalog.read()
        assertEquals("ana@example.com", first.technicians.single().email)
        assertEquals("Centro", first.sites.single().name)
        catalog.saveTechnician(first.technicians.single().copy(name = "Ana Pérez", active = false))
        catalog.saveSite(first.sites.single().copy(active = false))
        orders.close()
        LocalOrderRepository(context, name).use { reopened ->
            val loaded = LocalCatalogRepository(reopened).read()
            assertEquals(first.technicians.single().id, loaded.technicians.single().id)
            assertEquals("Ana Pérez", loaded.technicians.single().name)
            assertFalse(loaded.technicians.single().active)
            assertFalse(loaded.sites.single().active)
        }
    } }

    @Test fun duplicateRecordsAreRejectedAndExistingDataIsRetained() = runBlocking { scenario { _, catalog, _ ->
        catalog.saveTechnician(Technician(name = "Ana", email = "ana@example.com"))
        catalog.saveSite(Site(name = "Centro", address = "Calle Uno 123", client = "ACIS"))
        try { catalog.saveTechnician(Technician(name = "Otra Ana", email = "ANA@example.com")); fail("Correo duplicado") }
        catch (e: CatalogConflictException) { assertEquals("email", e.field) }
        try { catalog.saveSite(Site(name = "centro", address = "calle uno 123", client = "Otra empresa")); fail("Local duplicado") }
        catch (e: CatalogConflictException) { assertEquals("name", e.field) }
        assertEquals(1, catalog.read().technicians.size)
        assertEquals(1, catalog.read().sites.size)
    } }

    @Test fun orderSnapshotAndLegacyOrdersSurviveCatalogChanges() = runBlocking { scenario { orders, catalog, name ->
        val legacyId = orders.save(Order(site = "Local antiguo", technician = "Técnico antiguo"))
        catalog.saveTechnician(Technician(name = "Ana Soto", email = "ana@example.com"))
        catalog.saveSite(Site(name = "Centro", address = "Calle Uno 123", client = "ACIS"))
        val current = catalog.read()
        val draft = Order(siteId = current.sites.single().id, technicianId = current.technicians.single().id)
        val snapshot = CatalogRules.snapshot(draft, current)
        val id = orders.save(snapshot)
        catalog.saveSite(current.sites.single().copy(address = "Calle Dos 456", active = false))
        catalog.saveTechnician(current.technicians.single().copy(name = "Nombre nuevo", active = false))
        try { orders.save(snapshot); fail("No debe asignarse un registro inactivo") }
        catch (_: IllegalArgumentException) { }
        orders.close()
        LocalOrderRepository(context, name).use { reopened ->
            assertEquals(snapshot.copy(id = id), reopened.all().first { it.id == id })
            val legacy = reopened.all().first { it.id == legacyId }
            assertNull(legacy.siteId); assertNull(legacy.technicianId)
            assertEquals("Local antiguo", legacy.site)
        }
    } }
}
