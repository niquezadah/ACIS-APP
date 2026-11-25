package cl.acis.app.data.remote

import cl.acis.app.BuildConfig
import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Local-only contract check against a running Spring Boot instance configured with temporary H2. */
class OrderApiSpringIntegrationTest {
    @Test
    fun retrofitCreatesRetriesReadsAndUpdatesFictionalOrderAgainstSpring() = runBlocking {
        val baseUrl = BuildConfig.ACIS_API_BASE_URL
        val host = runCatching { URI(baseUrl).host }.getOrNull()
        assumeTrue(
            "La prueba E2E solo se ejecuta contra localhost con H2 temporal",
            host == "127.0.0.1" || host == "localhost"
        )

        val api = ApiClient.create(baseUrl)
        val clientOrderId = UUID.randomUUID().toString()
        val local = Order(
            site = "Local ficticio Retrofit",
            address = "Calle de prueba 123",
            equipment = "Equipo de laboratorio",
            technician = "Técnico ficticio",
            problem = "Falla de integración ficticia"
        )
        val createRequest = local.toCreateRequestDto(clientOrderId)

        val created = api.createOrder(createRequest)
        assertEquals(201, created.code())
        val createdBody = assertNotNullBody(created.body())
        assertEquals(clientOrderId, createdBody.clientOrderId)
        assertEquals("Local ficticio Retrofit", createdBody.site)
        val remoteId = createdBody.id

        // Replaying the same durable idempotency key and snapshot must return the same remote row.
        val retry = api.createOrder(createRequest)
        assertEquals(201, retry.code())
        assertEquals(remoteId, assertNotNullBody(retry.body()).id)

        val fetched = api.getOrder(remoteId)
        assertTrue("GET por ID remoto debe responder 2xx", fetched.isSuccessful)
        assertEquals(clientOrderId, assertNotNullBody(fetched.body()).clientOrderId)

        val listed = api.getOrders()
        assertTrue("GET de órdenes debe responder 2xx", listed.isSuccessful)
        assertEquals(
            "La misma clave idempotente debe corresponder a una sola orden remota",
            1,
            assertNotNullBody(listed.body()).count { it.clientOrderId == clientOrderId }
        )

        val updateRequest = local.copy(stage = Stage.ACCEPTED).toUpdateRequestDto(clientOrderId)
            .copy(id = remoteId)
        val updated = api.updateOrder(remoteId, updateRequest)
        assertTrue("PUT debe responder 2xx", updated.isSuccessful)
        val updatedBody = assertNotNullBody(updated.body())
        assertEquals(remoteId, updatedBody.id)
        assertEquals(clientOrderId, updatedBody.clientOrderId)
        assertEquals(Stage.ACCEPTED, updatedBody.stage)
    }

    private fun <T> assertNotNullBody(body: T?): T {
        assertNotNull("Spring debe devolver un cuerpo JSON", body)
        return body!!
    }
}
