package cl.acis.app.model

data class Technician(val id: String = "", val name: String = "", val email: String = "", val active: Boolean = true)
data class Site(val id: String = "", val name: String = "", val address: String = "", val client: String = "", val active: Boolean = true)
data class Catalog(val technicians: List<Technician> = emptyList(), val sites: List<Site> = emptyList())
class CatalogConflictException(val field: String) : Exception("Ya existe este registro")

object CatalogRules {
    fun technician(value: Technician): Map<String, String> = buildMap {
        if (value.name.trim().length < 2) put("name", "Ingresa un nombre de al menos 2 caracteres")
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.email.trim()))
            put("email", "Ingresa un correo válido")
    }

    fun site(value: Site): Map<String, String> = buildMap {
        if (value.name.trim().length < 2) put("name", "Ingresa el nombre del local")
        if (value.address.trim().length < 5) put("address", "Ingresa una dirección de al menos 5 caracteres")
        if (value.client.trim().length < 2) put("client", "Ingresa el nombre del cliente o empresa")
    }

    fun selection(order: Order, catalog: Catalog): Map<String, String> = buildMap {
        if (catalog.sites.none { it.id == order.siteId && it.active })
            put("site", "Selecciona un local activo")
        if (catalog.technicians.none { it.id == order.technicianId && it.active })
            put("technician", "Selecciona un técnico activo")
    }

    // Una orden conserva una copia de los datos de la visita, aunque el registro cambie después.
    fun snapshot(order: Order, catalog: Catalog): Order {
        require(selection(order, catalog).isEmpty())
        val site = catalog.sites.first { it.id == order.siteId }
        val technician = catalog.technicians.first { it.id == order.technicianId }
        return order.copy(site = site.name, address = site.address, siteClient = site.client, technician = technician.name)
    }
}
