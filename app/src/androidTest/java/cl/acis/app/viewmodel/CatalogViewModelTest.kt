package cl.acis.app.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.acis.app.data.*
import cl.acis.app.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val technician = Technician("t1", "Ana Soto", "ana@example.com")
    private val site = Site("s1", "Centro", "Calle Uno 123", "ACIS")
    private inner class CatalogFake : CatalogRepository {
        var data = Catalog(listOf(technician), listOf(site))
        var failRead = false
        var failSave = false
        var saves = 0
        override suspend fun read(): Catalog { check(!failRead); return data }
        override suspend fun saveTechnician(value: Technician) { check(!failSave); saves++; data = data.copy(technicians = listOf(value)) }
        override suspend fun saveSite(value: Site) { check(!failSave); saves++; data = data.copy(sites = listOf(value)) }
    }
    private class OrdersFake : OrderRepository {
        var saved: Order? = null
        override suspend fun all() = emptyList<Order>()
        override suspend fun save(order: Order): Long { saved = order.copy(id = 1); return 1 }
        override suspend fun delete(id: Long) = error("No corresponde")
    }

    private fun onMain(test: (ViewModelStore) -> Unit) {
        val store = ViewModelStore()
        try { instrumentation.runOnMainSync { test(store) } }
        finally { instrumentation.runOnMainSync { store.clear() } }
    }

    @Test fun onlyJefaturaCanWriteAndInvalidFormsStayOpen() = onMain { store ->
        val repo = CatalogFake()
        var role = "Técnico"
        val vm = CatalogViewModel(repo) { role }; store.put("catalog", vm)
        vm.saveTechnician(technician) { fail("Perfil incorrecto") }
        assertEquals(0, repo.saves)
        role = "Cliente"
        vm.saveSite(site) { fail("Perfil incorrecto") }
        assertEquals(0, repo.saves)
        role = "Jefatura"
        vm.saveTechnician(Technician()) { fail("Formulario inválido") }
        assertEquals(setOf("name", "email"), vm.state.value.errors.keys)
        var navigated = false
        vm.saveTechnician(technician.copy(active = false)) { navigated = true }
        assertTrue(navigated); assertEquals(1, repo.saves)
        assertFalse(vm.state.value.catalog.technicians.single().active)
    }

    @Test fun readFailureKeepsLastCatalogAndCanBeRetried() = onMain { store ->
        val repo = CatalogFake()
        val vm = CatalogViewModel(repo) { "Jefatura" }; store.put("catalog", vm)
        val original = vm.state.value.catalog
        repo.failRead = true; vm.refresh()
        assertNotNull(vm.state.value.readError)
        assertEquals(original, vm.state.value.catalog)
        repo.failRead = false; vm.refresh()
        assertNull(vm.state.value.readError)
    }

    @Test fun writeFailureDoesNotNavigateOrChangeStoredData() = onMain { store ->
        val repo = CatalogFake()
        val vm = CatalogViewModel(repo) { "Jefatura" }; store.put("catalog", vm)
        repo.failSave = true
        vm.saveSite(site.copy(name = "Cambio")) { fail("No debe cerrar el formulario") }
        assertEquals("Centro", repo.data.sites.single().name)
        assertNotNull(vm.state.value.message)
        assertFalse(vm.state.value.busy)
    }

    @Test fun orderCreationRequiresRoleAndFreshActiveIds() = onMain { store ->
        val catalog = CatalogFake()
        val repo = OrdersFake()
        val vm = AcisViewModel(repo, EvidenceStore(instrumentation.targetContext), catalog); store.put("order", vm)
        vm.edit(Order(siteId = "s1", technicianId = "t1", site = "Texto inventado", technician = "Otro técnico", equipment = "Equipo uno", problem = "Mantención mensual"))
        vm.saveNew { fail("Solo Jefatura") }; assertNull(repo.saved)
        vm.role("Jefatura")
        catalog.data = catalog.data.copy(technicians = listOf(technician.copy(active = false)))
        vm.saveNew { fail("Técnico inactivo") }
        assertTrue(vm.state.value.errors.containsKey("technician")); assertNull(repo.saved)
        catalog.data = Catalog(listOf(technician), listOf(site))
        vm.saveNew {}
        assertEquals("Ana Soto", repo.saved?.technician)
        assertEquals("Calle Uno 123", repo.saved?.address)
        assertEquals("ACIS", repo.saved?.siteClient)
    }
}
