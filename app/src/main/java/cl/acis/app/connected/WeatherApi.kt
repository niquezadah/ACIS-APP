package cl.acis.app.connected

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Daily(val rain_sum:List<Double?>,val wind_speed_10m_max:List<Double?>)
data class Forecast(val daily:Daily)
interface Weather {
    @GET("v1/forecast?daily=rain_sum,wind_speed_10m_max&timezone=auto")
    suspend fun day(@Query("latitude") lat:Double,@Query("longitude") lon:Double,@Query("start_date") start:String,@Query("end_date") end:String):Forecast
}
internal fun forecastText(rain:Double,wind:Double):String =
    (if(rain>0 || wind>=40) "Revisa la visita exterior. " else "")+"Lluvia: "+rain+" mm · Viento máximo: "+wind+" km/h. Fuente: Open-Meteo."

internal val weather by lazy {Retrofit.Builder().baseUrl("https://api.open-meteo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(Weather::class.java)}
