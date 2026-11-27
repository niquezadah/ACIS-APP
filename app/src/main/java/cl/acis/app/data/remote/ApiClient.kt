package cl.acis.app.data.remote

import cl.acis.app.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    /** Permite pasar otra URL en pruebas; producción/desarrollo lee la propiedad Gradle local. */
    fun create(baseUrl: String = BuildConfig.ACIS_API_BASE_URL): OrderApi {
        require(baseUrl.isNotBlank()) {
            "Configura ACIS_API_BASE_URL en las propiedades locales de Gradle"
        }
        require(baseUrl.endsWith('/')) { "ACIS_API_BASE_URL debe terminar en /" }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrderApi::class.java)
    }
}
