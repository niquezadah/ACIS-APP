
package cl.acis.app.model

object Rules {
 const val MAX_PHOTOS = 10

 fun photoLimit(o: Order): Map<String, String> =
  if (o.evidencePhotos.size > MAX_PHOTOS)
   mapOf("photo" to "Puedes agregar hasta 10 fotografías por orden")
  else emptyMap()

 fun creation(o: Order): Map<String, String> = buildMap {
  putAll(planningErrors(o))
  if (o.site.isBlank())
   put("site", "Ingresa el nombre del local")
  if (o.address.isBlank())
   put("address", "Ingresa la dirección de la visita")
  if (o.equipment.isBlank())
   put("equipment", "Identifica el equipo que se revisará")
  if (o.technician.isBlank())
   put("technician", "Indica el técnico asignado")
  if (o.problem.trim().length < 5)
   put("problem", "Describe el motivo con al menos 5 caracteres")
 }

 fun closing(o: Order): Map<String, String> = buildMap {
  if (o.diagnosis.trim().length < 5)
   put("diagnosis", "Explica el diagnóstico (mínimo 5 caracteres)")
  if (o.work.trim().length < 5)
   put("work", "Describe el trabajo realizado (mínimo 5 caracteres)")

  putAll(photoLimit(o))

  if (o.evidencePhotos.isEmpty())
   put("photo", "Agrega una fotografía como evidencia")
 }

 fun confirmation(
  o: Order,
  accepted: Boolean
 ): Map<String, String> = buildMap {
  if (o.customer.isBlank())
   put("customer", "Ingresa el nombre de quien confirma")

  if (!accepted)
   put("accepted", "Debes confirmar tu conformidad con el trabajo")
 }

 fun canMove(from: Stage, to: Stage) = when (from) {
  Stage.ASSIGNED -> to == Stage.ACCEPTED
  Stage.ACCEPTED -> to == Stage.ACTIVE
  Stage.ACTIVE -> to == Stage.DONE
  Stage.DONE -> to == Stage.CLOSED
  Stage.CLOSED -> false
 }
}
