package cl.acis.app.model

import java.time.LocalDate

fun planningErrors(order:Order):Map<String,String> = buildMap {
    if(runCatching {LocalDate.parse(order.scheduled)}.isFailure) put("scheduled","Usa una fecha válida en formato AAAA-MM-DD.")
    if(order.slaHours !in 1..8760) put("sla","Ingresa entre 1 y 8760 horas.")
    if(order.latitude.isNotBlank() || order.longitude.isNotBlank()){
        val lat=order.latitude.toDoubleOrNull();val lon=order.longitude.toDoubleOrNull()
        if(lat==null || lon==null || lat !in -90.0..90.0 || lon !in -180.0..180.0) put("coordinates","Revisa la latitud y la longitud del local.")
    }
}
fun serviceDeadline(order:Order,now:Long):String {
    if(order.created<=0) return "Sin fecha de creación"
    val elapsed=(order.arrival.takeIf {it>0} ?: now)-order.created
    val state=if(elapsed>order.slaHours*3600000L) "Fuera de plazo" else if(order.arrival>0) "Cumplido" else "Dentro de plazo"
    return state+" · "+order.slaHours+" h desde la creación hasta la llegada"
}
