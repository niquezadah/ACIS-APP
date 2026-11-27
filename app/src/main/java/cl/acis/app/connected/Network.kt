package cl.acis.app.connected

import cl.acis.app.BuildConfig
import cl.acis.app.data.remote.*
import cl.acis.app.model.*
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Profile(val id:String,val email:String,val name:String,val role:String,val scope:String)
data class Session(val token:String,val expires:Long,val profile:Profile)
data class Credentials(val email:String="",val password:String="",val name:String="",val recovery:String="")
data class Recovery(val recovery:String)
data class Access(val role:String,val scope:String)
interface AccountsApi {
    @POST("accounts/login") suspend fun login(@Body c:Credentials):Session
    @POST("accounts/register") suspend fun register(@Body c:Credentials):Recovery
    @POST("accounts/recover") suspend fun recover(@Body c:Credentials):Recovery
    @GET("accounts/me") suspend fun me():Profile
    @PATCH("accounts/me") suspend fun edit(@Body c:Credentials):Profile
    @POST("accounts/logout") suspend fun logout()
    @GET("accounts") suspend fun users():List<Profile>
    @PUT("accounts/{id}/access") suspend fun access(@Path("id") id:String,@Body a:Access)
}
data class Entry(val id:String,val kind:String,val name:String,val address:String="",val client:String="",val email:String="",val active:Boolean=true)
data class Plan(val site:String,val technician:String,val scheduled:String,val latitude:Double?,val longitude:Double?,val slaHours:Int)
data class Write(val operation:String,val revision:Long,val plan:Plan,val order:OrderRequestDto,val photos:List<String>)
data class Visit(val order:OrderResponseDto,val plan:Plan,val revision:Long,val created:Long,val photos:List<String>)
data class Equipment(val id:String,val site:String,val name:String,val location:String,val active:Boolean,val lastService:Long)
data class Event(val name:String,val action:String,val at:Long)
interface FieldApi {
    @GET("field/orders/{id}/history") suspend fun history(@Path("id") id:Long):List<Event>
    @GET("field/equipment") suspend fun equipment(@Query("site") site:String):List<Equipment>
    @PUT("field/equipment/{id}") suspend fun saveEquipment(@Path("id") id:String,@Body equipment:Equipment)
    @GET("field/catalog") suspend fun catalog():List<Entry>
    @PUT("field/catalog/{id}") suspend fun entry(@Path("id") id:String,@Body item:Entry)
    @GET("field/orders") suspend fun orders():List<Visit>
    @POST("field/orders") suspend fun create(@Body request:Write):Visit
    @PUT("field/orders/{id}") suspend fun update(@Path("id") id:Long,@Body request:Write):Visit
    @DELETE("field/orders/{id}") suspend fun delete(@Path("id") id:Long,@Query("revision") rev:Long)
    @PUT("field/orders/{id}/photos/{hash}") suspend fun upload(@Path("id") id:Long,@Path("hash") hash:String,@Body image:RequestBody)
    @Streaming @GET("field/orders/{id}/photos/{hash}") suspend fun download(@Path("id") id:Long,@Path("hash") hash:String):ResponseBody
}
class Network(token:String="",fieldUrl:String=BuildConfig.FIELD_URL,accountsUrl:String=BuildConfig.ACCOUNTS_URL) {
    private val client=OkHttpClient.Builder().addInterceptor { chain ->
        val request=chain.request().newBuilder()
        if(token.isNotEmpty()) request.header("Authorization","Bearer "+token)
        chain.proceed(request.build())
    }.followRedirects(false).build()
    private fun retrofit(url:String)=Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create()).build()
    val accounts:AccountsApi=retrofit(accountsUrl).create(AccountsApi::class.java)
    val field:FieldApi=retrofit(fieldUrl).create(FieldApi::class.java)
}
