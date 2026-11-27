
package cl.acis.app.model

enum class Stage(val label: String) {
 ASSIGNED("Asignada"),
 ACCEPTED("Aceptada"),
 ACTIVE("En atención"),
 DONE("Por confirmar"),
 CLOSED("Cerrada")
}
