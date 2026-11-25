package cl.acis.app.data.remote

import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OrderApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClient.create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getOrdersUsesExistingEndpointAndParsesStageAndRemoteId() = runBlocking {
        server.enqueue(MockResponse().setBody("[${orderJson(id = 42, stage = "ACTIVE")}]"))

        val response = api.getOrders()
        val request = server.takeRequest()

        assertTrue(response.isSuccessful)
        val remote = response.body()!!.single().toRemoteOrder(localId = 7)
        assertEquals("GET", request.method)
        assertEquals("/api/ordenes", request.path)
        assertEquals(42L, remote.remoteId)
        assertEquals(7L, remote.order.id)
        assertEquals(Stage.ACTIVE, remote.order.stage)
        assertEquals(2, remote.evidenceCount)
        assertTrue(remote.order.evidencePhotos.isEmpty())
    }

    @Test
    fun createSerializesOnlyContractFieldsAndDoesNotSendLocalPhotoPaths() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody(orderJson(id = 70)))
        val local = Order(
            id = 15,
            site = "Local de prueba",
            address = "Calle de prueba",
            equipment = "Equipo",
            technician = "Técnico",
            problem = "Falla de prueba",
            photos = listOf("/private/photo-one.jpg", "/private/photo-two.jpg")
        )

        val response = api.createOrder(local.toCreateRequestDto(CLIENT_ORDER_ID))
        val request = server.takeRequest()
        val json = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/api/ordenes", request.path)
        assertEquals(201, response.code())
        val body = Gson().fromJson(json, OrderRequestDto::class.java)
        assertNull(body.id)
        assertEquals(CLIENT_ORDER_ID, body.clientOrderId)
        assertTrue(json.contains("\"evidenceCount\":2"))
        assertTrue(json.contains("\"stage\":\"ASSIGNED\""))
        assertFalse(json.contains("photo"))
        assertFalse(json.contains("/private/"))
    }

    @Test
    fun updateUsesExplicitRemoteIdWithoutReplacingLocalId() = runBlocking {
        server.enqueue(MockResponse().setBody(orderJson(id = 91)))
        val local = Order(id = 15, site = "Local")

        val response = api.updateOrder(91, local.toUpdateRequestDto(CLIENT_ORDER_ID).copy(id = 91))
        val request = server.takeRequest()
        val body = Gson().fromJson(request.body.readUtf8(), OrderRequestDto::class.java)

        assertEquals("PUT", request.method)
        assertEquals("/api/ordenes/91", request.path)
        assertEquals(91L, body.id)
        assertEquals(15L, local.id)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun httpErrorIsReturnedWithoutBeingReportedAsSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val response = api.getOrder(404)

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun networkDisconnectThrowsIOException() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(IOException::class.java) {
            runBlocking { api.getOrders() }
        }
    }

    @Test
    fun updateMapperRejectsBlankIdempotencyKey() {
        assertThrows(IllegalArgumentException::class.java) {
            Order(id = 15).toUpdateRequestDto("")
        }
    }

    private fun orderJson(id: Long, stage: String = "ASSIGNED") = """
        {
          "id":$id,"clientOrderId":"$CLIENT_ORDER_ID","site":"Local","address":"Calle","equipment":"Equipo",
          "technician":"Técnico","priority":"Normal","type":"Preventiva",
          "problem":"Falla de prueba","stage":"$stage","diagnosis":"",
          "work":"","materials":"","location":"","arrival":0,"departure":0,
          "customer":"","feedback":"","customerConfirmed":false,"evidenceCount":2
        }
    """.trimIndent()

    private companion object {
        const val CLIENT_ORDER_ID = "a5796a8e-8497-4cc2-bdbf-44f1ab736c71"
    }

}
