package cl.acis.app.data.remote

import cl.acis.app.data.LocalOrderRepository
import cl.acis.app.model.Order
import cl.acis.app.model.Stage
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OrderSyncCoordinatorTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "coordinator-test-${UUID.randomUUID()}.db"
    private lateinit var repository: LocalOrderRepository
    private lateinit var server: MockWebServer
    private lateinit var coordinator: OrderSyncCoordinator

    @Before
    fun setUp() {
        repository = LocalOrderRepository(context, databaseName)
        server = MockWebServer().also { it.start() }
        coordinator = OrderSyncCoordinator(repository, ApiClient.create(server.url("/").toString()))
    }

    @After
    fun tearDown() {
        server.shutdown()
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun disconnectedCreateRemainsQueuedAndRetryUsesSameIdempotencyKey() = runBlocking {
        val localId = repository.save(sampleOrder())
        val clientOrderId = repository.syncMetadata(localId)!!.clientOrderId
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.enqueue(MockResponse().setResponseCode(201).setBody(orderJson(730, clientOrderId)))

        assertEquals(OrderSyncResult.NetworkUnavailable(0), coordinator.syncPending())
        assertEquals(1, repository.pendingSyncCount())
        assertNull(repository.syncMetadata(localId)!!.remoteOrderId)
        val firstAttempt = server.takeRequest()
        val firstBody = firstAttempt.body.readUtf8()

        assertEquals(OrderSyncResult.Complete(1), coordinator.syncPending())
        assertEquals(0, repository.pendingSyncCount())
        assertEquals(730L, repository.syncMetadata(localId)!!.remoteOrderId)
        val retry = server.takeRequest()
        val retryBody = retry.body.readUtf8()
        assertEquals(firstAttempt.method, retry.method)
        assertEquals(firstAttempt.path, retry.path)
        assertEquals(firstBody, retryBody)
        assertTrue(firstBody.contains("clientOrderId"))
    }

    @Test
    fun failedHttpUpdateStaysQueuedThenOrderedRetryUsesRemoteId() = runBlocking {
        val localId = repository.save(sampleOrder())
        val clientOrderId = repository.syncMetadata(localId)!!.clientOrderId
        server.enqueue(MockResponse().setResponseCode(201).setBody(orderJson(731, clientOrderId)))
        assertEquals(OrderSyncResult.Complete(1), coordinator.syncPending())
        assertEquals("POST", server.takeRequest().method)

        repository.save(sampleOrder().copy(id = localId, stage = Stage.ACCEPTED))
        server.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))
        assertEquals(OrderSyncResult.HttpFailure(0, 409), coordinator.syncPending())
        assertEquals(1, repository.pendingSyncCount())
        val rejectedAttempt = server.takeRequest()
        assertEquals("PUT", rejectedAttempt.method)
        assertEquals("/api/ordenes/731", rejectedAttempt.path)

        server.enqueue(MockResponse().setBody(orderJson(731, clientOrderId, "ACCEPTED")))
        assertEquals(OrderSyncResult.Complete(1), coordinator.syncPending())
        val retry = server.takeRequest()
        assertEquals("PUT", retry.method)
        val body = retry.body.readUtf8()
        assertTrue(body.contains("\"id\":731"))
        assertTrue(body.contains("\"stage\":\"ACCEPTED\""))
        assertFalse(body.contains("photo"))
        assertFalse(body.contains("/private/"))
        assertEquals(0, repository.pendingSyncCount())
    }

    private fun sampleOrder() = Order(
        site = "Local ficticio",
        address = "Calle de prueba",
        equipment = "Equipo ficticio",
        technician = "Técnico",
        problem = "Falla ficticia",
        photos = listOf("/private/photo.jpg")
    )

    private fun orderJson(id: Long, clientOrderId: String, stage: String = "ASSIGNED") = """
        {
          "id":$id,"clientOrderId":"$clientOrderId","site":"Local ficticio",
          "address":"Calle de prueba","equipment":"Equipo ficticio","technician":"Técnico",
          "priority":"Normal","type":"Preventiva","problem":"Falla ficticia","stage":"$stage",
          "diagnosis":"","work":"","materials":"","location":"","arrival":0,"departure":0,
          "customer":"","feedback":"","customerConfirmed":false,"evidenceCount":1
        }
    """.trimIndent()
}
