package cl.acis.app.connected

import cl.acis.app.model.*

private fun cell(value:String):String {
    val safe=if(value.trimStart().firstOrNull() in listOf('=','+','-','@')) "'"+value else value
    return "\""+safe.replace("\"","\"\"")+"\""
}
internal fun report(orders:List<Order>,now:Long):String=buildString {
    appendLine("OT;Local;Equipo;Técnico;Fecha prevista;Estado;Minutos de atención;Plazo")
    for(o in orders){
        val duration=if(o.arrival>0 && o.departure>=o.arrival) ((o.departure-o.arrival)/60000).toString() else ""
        appendLine(listOf(o.id.toString(),o.site,o.equipment,o.technician,o.scheduled,o.stage.label,duration,serviceDeadline(o,now)).joinToString(";"){cell(it)})
    }
}
