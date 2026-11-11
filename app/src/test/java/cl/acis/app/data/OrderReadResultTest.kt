package cl.acis.app.data

import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import java.io.IOException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OrderReadResultTest {
    private fun payload(stage: String = "DONE") = JSONObject("""
        {
          "site":"Local de prueba","address":"Calle de prueba","equipment":"Equipo",
          "technician":"Ana","priority":"Alta","type":"Correctiva","problem":"Revisión",
          "stage":"$stage","diagnosis":"Diagnóstico","work":"Trabajo realizado",
          "materials":"Materiales","photo":"antigua.jpg","location":"Ubicación ficticia",
          "arrival":100,"departure":200,"customer":"Cliente","feedback":"Observaciones"
        }
    """.trimIndent())

    @Test fun malformedRowsDoNotBlockValidRowsAndInputIsUnchanged() {
        val rows = listOf(
            4L to payload().toString(),
            3L to "{invalid",
            2L to payload("ESTADO_DESCONOCIDO").toString(),
            1L to payload("CLOSED").toString()
        )
        val original = rows.toList()
        val result = readOrderRows(rows.asSequence())
        assertEquals(listOf(4L, 1L), result.orders.map { it.id })
        assertEquals(listOf(3L, 2L), result.unreadableOrderIds)
        assertEquals(original, rows)
    }

    @Test fun legacyOrderKeepsEveryFieldAndDefaultsConfirmationToFalse() {
        val order = readOrderRows(sequenceOf(1L to payload("CLOSED").toString())).orders.single()
        assertEquals(Order(
            id = 1, site = "Local de prueba", address = "Calle de prueba", equipment = "Equipo",
            technician = "Ana", priority = "Alta", type = "Correctiva", problem = "Revisión",
            stage = Stage.CLOSED, diagnosis = "Diagnóstico", work = "Trabajo realizado",
            materials = "Materiales", photo = "antigua.jpg", location = "Ubicación ficticia",
            arrival = 100, departure = 200, customer = "Cliente", feedback = "Observaciones",
            photos = listOf("antigua.jpg"), customerConfirmed = false
        ), order)
    }

    @Test fun modernOrderKeepsTenPhotosAndConfirmation() {
        val photos = (1..10).map { "foto_$it.jpg" }
        val json = payload().put("photos", photos).put("customerConfirmed", true)
        val order = readOrderRows(sequenceOf(7L to json.toString())).orders.single()
        assertEquals(photos, order.evidencePhotos)
        assertTrue(order.customerConfirmed)
        assertEquals(Stage.DONE, order.stage)
        assertEquals("Ubicación ficticia", order.location)
        assertEquals(100L, order.arrival)
        assertEquals(200L, order.departure)
    }

    @Test fun explicitFalseConfirmationRemainsFalse() {
        val json = payload().put("customerConfirmed", false)
        assertFalse(readOrderRows(sequenceOf(1L to json.toString())).orders.single().customerConfirmed)
    }

    @Test fun emptyPhotoReferencesRemainEmpty() {
        val json = payload().put("photos", emptyList<String>()).put("photo", "")
        assertTrue(readOrderRows(sequenceOf(1L to json.toString())).orders.single().evidencePhotos.isEmpty())
    }

    @Test fun missingRequiredFieldIsReportedInsteadOfInventingAnOrder() {
        val json = payload().apply { remove("site") }
        val result = readOrderRows(sequenceOf(9L to json.toString()))
        assertTrue(result.orders.isEmpty())
        assertEquals(listOf(9L), result.unreadableOrderIds)
    }

    @Test fun emptyDatabaseAndEntirelyUnreadableDatabaseAreDifferent() {
        assertEquals(OrderReadResult(emptyList()), readOrderRows(emptySequence()))
        val unreadable = readOrderRows(sequenceOf(1L to "not JSON"))
        assertTrue(unreadable.orders.isEmpty())
        assertEquals(listOf(1L), unreadable.unreadableOrderIds)
    }

    @Test(expected = IOException::class)
    fun failureWhileReadingRowsPropagatesAsAReadFailure() {
        val rows = sequence {
            yield(1L to payload().toString())
            throw IOException("Fallo de lectura simulado")
        }
        readOrderRows(rows)
    }
}
