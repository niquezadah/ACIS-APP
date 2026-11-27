package cl.acis.app.connected

import androidx.compose.material3.*
import androidx.compose.runtime.*
import cl.acis.app.model.Order
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

@Composable fun WeatherNotice(o:Order) {
    val lat=o.latitude.toDoubleOrNull() ?: return
    val lon=o.longitude.toDoubleOrNull() ?: return
    if(lat !in -90.0..90.0 || lon !in -180.0..180.0) return
    val date=runCatching { LocalDate.parse(o.scheduled) }.getOrNull() ?: return
    var text by remember(lat,lon,date){mutableStateOf("")}
    LaunchedEffect(lat,lon,date) {
        if(date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusDays(15))){text="Pronóstico disponible para los próximos 16 días.";return@LaunchedEffect}
        kotlinx.coroutines.delay(600)
        text=try {
            val daily=weather.day(lat,lon,date.toString(),date.toString()).daily
            val rain=requireNotNull(daily.rain_sum.first());val wind=requireNotNull(daily.wind_speed_10m_max.first())
            forecastText(rain,wind)
        }catch(e:CancellationException){throw e}catch(e:Exception){"Pronóstico no disponible."}
    }
    if(text.isNotEmpty()) Text(text,style=MaterialTheme.typography.bodyMedium)
}
