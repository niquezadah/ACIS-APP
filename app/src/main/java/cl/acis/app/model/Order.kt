
package cl.acis.app.model

data class Order(
 val id: Long = 0,
 val scheduled:String = java.time.LocalDate.now().toString(),
 val latitude:String = "",
 val longitude:String = "",
 val slaHours:Int = 24,
 val created:Long = 0,
 val serverRevision:Long = 0,
 val creationKey:String = "",
 val siteId: String? = null,
 val technicianId: String? = null,
 val siteClient: String = "",
 val site: String = "",
 val address: String = "",
 val equipment: String = "",
 val technician: String = "",
 val priority: String = "Normal",
 val type: String = "Preventiva",
 val problem: String = "",
 val stage: Stage = Stage.ASSIGNED,
 val diagnosis: String = "",
 val work: String = "",
 val materials: String = "",
 val photo: String = "",
 val location: String = "",
 val arrival: Long = 0,
 val departure: Long = 0,
 val customer: String = "",
 val feedback: String = "",
 val photos: List<String> = emptyList(),
 val customerConfirmed: Boolean = false
) : java.io.Serializable {
 val evidencePhotos: List<String>
  get() = if (photos.isNotEmpty()) photos
  else listOf(photo).filter { it.isNotBlank() }

 fun withPhotos(paths: List<String>) =
  copy(photo = paths.firstOrNull().orEmpty(), photos = paths.toList())
}
