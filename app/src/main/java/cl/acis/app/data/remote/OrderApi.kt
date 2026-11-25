package cl.acis.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Contrato HTTP disponible actualmente en acis-microservicio. */
interface OrderApi {
    @GET("api/ordenes")
    suspend fun getOrders(): Response<List<OrderResponseDto>>

    @GET("api/ordenes/{id}")
    suspend fun getOrder(@Path("id") remoteId: Long): Response<OrderResponseDto>

    @POST("api/ordenes")
    suspend fun createOrder(@Body request: OrderRequestDto): Response<OrderResponseDto>

    @PUT("api/ordenes/{id}")
    suspend fun updateOrder(
        @Path("id") remoteId: Long,
        @Body request: OrderRequestDto
    ): Response<OrderResponseDto>
}
