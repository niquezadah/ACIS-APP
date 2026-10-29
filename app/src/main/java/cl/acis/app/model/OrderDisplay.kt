package cl.acis.app.model

/** Primero las órdenes abiertas más urgentes; las cerradas quedan al final. */
fun ordersByPriority(orders: List<Order>): List<Order> = orders.sortedWith(
    compareBy<Order> { it.stage == Stage.CLOSED }
        .thenBy { order ->
            if (order.stage == Stage.CLOSED) 0 else when (order.priority) {
                "Urgente" -> 0
                "Alta" -> 1
                else -> 2
            }
        }
        .thenByDescending { it.id }
)

/** Copia solo latitud y longitud, sin el texto de precisión del GPS. */
fun coordinatesForMaps(location: String): String? {
    val pair = location.substringBefore('·').split(',').map { it.trim() }
    if (pair.size != 2) return null
    val latitude = pair[0].toDoubleOrNull() ?: return null
    val longitude = pair[1].toDoubleOrNull() ?: return null
    if (!latitude.isFinite() || !longitude.isFinite()) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return "${pair[0]}, ${pair[1]}"
}
