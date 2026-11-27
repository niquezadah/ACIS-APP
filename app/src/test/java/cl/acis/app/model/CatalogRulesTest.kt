package cl.acis.app.model

import org.junit.Assert.*
import org.junit.Test

class CatalogRulesTest {
    private val technician = Technician("t1", "Ana Soto", "ana@example.com")
    private val site = Site("s1", "Sucursal Centro", "Calle Uno 123", "Empresa Uno")
    private val catalog = Catalog(listOf(technician), listOf(site))

    @Test fun technicianNeedsNameAndValidEmail() {
        assertEquals(setOf("name", "email"), CatalogRules.technician(Technician(name = " ", email = "a@b")).keys)
        assertTrue(CatalogRules.technician(technician.copy(name = " Ana ", email = " ana@example.com ")).isEmpty())
        assertTrue(CatalogRules.technician(technician.copy(email = "ana @example.com")).containsKey("email"))
    }

    @Test fun siteRequiresClientAndAddress() {
        assertEquals(setOf("name", "address", "client"), CatalogRules.site(Site()).keys)
        assertTrue(CatalogRules.site(site).isEmpty())
    }

    @Test fun namesAloneDoNotIdentifySelection() {
        val order = Order(site = site.name, technician = technician.name)
        assertEquals(setOf("site", "technician"), CatalogRules.selection(order, catalog).keys)
    }

    @Test fun inactiveAndMissingRecordsCannotBeAssigned() {
        val order = Order(siteId = "s1", technicianId = "t1")
        assertTrue(CatalogRules.selection(order, catalog).isEmpty())
        assertEquals(setOf("site", "technician"), CatalogRules.selection(order,
            Catalog(listOf(technician.copy(active = false)), listOf(site.copy(active = false)))).keys)
        assertEquals(2, CatalogRules.selection(order, Catalog()).size)
    }

    @Test fun snapshotUsesStoredDataAndSurvivesCatalogEdits() {
        val order = CatalogRules.snapshot(Order(siteId = "s1", technicianId = "t1", site = "Inventado", address = "Otra dirección"), catalog)
        assertEquals(site.name, order.site)
        assertEquals(site.address, order.address)
        assertEquals(site.client, order.siteClient)
        assertEquals(technician.name, order.technician)
        val changed = catalog.copy(sites = listOf(site.copy(address = "Dirección nueva")))
        assertEquals(site.address, order.address)
        assertNotEquals(order.address, CatalogRules.snapshot(order, changed).address)
    }

    @Test fun identicalNamesRemainDifferentPeople() {
        val second = technician.copy(id = "t2", email = "otra@example.com")
        val selected = CatalogRules.snapshot(Order(siteId = "s1", technicianId = "t2"), catalog.copy(technicians = listOf(technician, second)))
        assertEquals("t2", selected.technicianId)
    }
}
