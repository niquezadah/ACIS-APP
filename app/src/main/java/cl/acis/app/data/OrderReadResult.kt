package cl.acis.app.data

import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import org.json.JSONException
import org.json.JSONObject

data class OrderReadResult(
    val orders: List<Order>,
    val unreadableOrderIds: List<Long> = emptyList()
)

// Consume las filas sin escribir ni intentar reparar su contenido.
internal fun readOrderRows(rows: Sequence<Pair<Long, String>>): OrderReadResult {
    val orders = mutableListOf<Order>()
    val unreadableIds = mutableListOf<Long>()
    for ((id, payload) in rows) {
        try {
            orders.add(decodeOrder(id, payload))
        } catch (_: JSONException) {
            unreadableIds.add(id)
        } catch (_: IllegalArgumentException) {
            unreadableIds.add(id)
        }
    }
    return OrderReadResult(orders, unreadableIds)
}

private fun decodeOrder(id: Long, payload: String): Order {
    val json = JSONObject(payload)
    val legacyPhoto = json.getString("photo")
    val array = json.optJSONArray("photos")
    val photos = if (array == null) {
        listOf(legacyPhoto).filter { it.isNotBlank() }
    } else {
        (0 until array.length()).map { array.getString(it) }.filter { it.isNotBlank() }
    }
    return Order(
        id = id,
        site = json.getString("site"),
        address = json.getString("address"),
        equipment = json.getString("equipment"),
        technician = json.getString("technician"),
        priority = json.getString("priority"),
        type = json.getString("type"),
        problem = json.getString("problem"),
        stage = Stage.valueOf(json.getString("stage")),
        diagnosis = json.getString("diagnosis"),
        work = json.getString("work"),
        materials = json.getString("materials"),
        photo = legacyPhoto,
        location = json.getString("location"),
        arrival = json.getLong("arrival"),
        departure = json.getLong("departure"),
        customer = json.getString("customer"),
        feedback = json.getString("feedback"),
        photos = photos,
        customerConfirmed = json.optBoolean("customerConfirmed", false)
    )
}
