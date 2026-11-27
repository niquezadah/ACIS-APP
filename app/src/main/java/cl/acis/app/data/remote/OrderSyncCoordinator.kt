package cl.acis.app.data.remote

import cl.acis.app.data.OrderSyncStore
import cl.acis.app.data.PendingSyncKind
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException

sealed interface OrderSyncResult {
    data class Complete(val operations: Int) : OrderSyncResult
    data class NetworkUnavailable(val completed: Int) : OrderSyncResult
    data class HttpFailure(val completed: Int, val statusCode: Int) : OrderSyncResult
    data class InvalidResponse(val completed: Int, val reason: String) : OrderSyncResult
}

/** Explicitly invoked sync runner. It is not wired into Application, ViewModel, or UI. */
class OrderSyncCoordinator(
    private val store: OrderSyncStore,
    private val api: OrderApi,
    private val gson: Gson = Gson()
) {
    suspend fun syncPending(maxOperations: Int = DEFAULT_BATCH_SIZE): OrderSyncResult {
        require(maxOperations > 0) { "El tamaño del lote debe ser positivo" }
        var completed = 0
        while (completed < maxOperations) {
            val pending = store.nextPendingSync() ?: return OrderSyncResult.Complete(completed)
            val request = try {
                gson.fromJson(pending.requestJson, OrderRequestDto::class.java)
            } catch (exception: JsonParseException) {
                return OrderSyncResult.InvalidResponse(completed, "La solicitud local pendiente no se puede leer")
            }
            if (request.clientOrderId != pending.clientOrderId) {
                return OrderSyncResult.InvalidResponse(completed, "La clave de orden pendiente no coincide")
            }

            val response = try {
                when (pending.kind) {
                    PendingSyncKind.CREATE -> api.createOrder(request.copy(id = null))
                    PendingSyncKind.UPDATE -> {
                        val remoteId = pending.remoteOrderId
                            ?: return OrderSyncResult.InvalidResponse(completed, "Falta crear la orden remota antes de actualizar")
                        api.updateOrder(remoteId, request.copy(id = remoteId))
                    }
                }
            } catch (_: IOException) {
                // Keep the durable queue entry. The same body/key will be retried later.
                return OrderSyncResult.NetworkUnavailable(completed)
            } catch (_: JsonParseException) {
                return OrderSyncResult.InvalidResponse(completed, "La respuesta del servidor no es JSON válido")
            }

            if (!response.isSuccessful) {
                return OrderSyncResult.HttpFailure(completed, response.code())
            }
            val remote = response.body()
                ?: return OrderSyncResult.InvalidResponse(completed, "La respuesta del servidor está vacía")
            if (remote.clientOrderId != pending.clientOrderId) {
                return OrderSyncResult.InvalidResponse(completed, "La respuesta pertenece a otra orden local")
            }
            if (pending.remoteOrderId != null && remote.id != pending.remoteOrderId) {
                return OrderSyncResult.InvalidResponse(completed, "La respuesta cambió el ID remoto asociado")
            }

            store.completePendingSync(pending, remote.id)
            completed++
        }
        return OrderSyncResult.Complete(completed)
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 100
    }
}
