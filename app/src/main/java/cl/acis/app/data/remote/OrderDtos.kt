package cl.acis.app.data.remote

import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import com.google.gson.annotations.SerializedName

/** Request REST: deliberadamente no contiene `photo` ni `photos` locales. */
data class OrderRequestDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("site") val site: String,
    @SerializedName("address") val address: String,
    @SerializedName("equipment") val equipment: String,
    @SerializedName("technician") val technician: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("type") val type: String,
    @SerializedName("problem") val problem: String,
    @SerializedName("stage") val stage: Stage,
    @SerializedName("diagnosis") val diagnosis: String,
    @SerializedName("work") val work: String,
    @SerializedName("materials") val materials: String,
    @SerializedName("location") val location: String,
    @SerializedName("arrival") val arrival: Long,
    @SerializedName("departure") val departure: Long,
    @SerializedName("customer") val customer: String,
    @SerializedName("feedback") val feedback: String,
    @SerializedName("customerConfirmed") val customerConfirmed: Boolean,
    @SerializedName("evidenceCount") val evidenceCount: Int
)

data class OrderResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("site") val site: String,
    @SerializedName("address") val address: String,
    @SerializedName("equipment") val equipment: String,
    @SerializedName("technician") val technician: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("type") val type: String,
    @SerializedName("problem") val problem: String,
    @SerializedName("stage") val stage: Stage,
    @SerializedName("diagnosis") val diagnosis: String,
    @SerializedName("work") val work: String,
    @SerializedName("materials") val materials: String,
    @SerializedName("location") val location: String,
    @SerializedName("arrival") val arrival: Long,
    @SerializedName("departure") val departure: Long,
    @SerializedName("customer") val customer: String,
    @SerializedName("feedback") val feedback: String,
    @SerializedName("customerConfirmed") val customerConfirmed: Boolean,
    @SerializedName("evidenceCount") val evidenceCount: Int
)

/** La identidad remota queda separada del ID autoincremental local de SQLite. */
data class RemoteOrder(
    val remoteId: Long,
    val order: Order,
    val evidenceCount: Int
)
