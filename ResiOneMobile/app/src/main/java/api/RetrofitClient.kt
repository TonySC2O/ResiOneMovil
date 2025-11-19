package api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit singleton para comunicación con el backend
 * 
 * IMPORTANTE: Para testing en Android Emulator:
 * - Use 10.0.2.2 en lugar de localhost (el emulador mapea 10.0.2.2 a localhost de tu PC)
 * - Para dispositivo físico, usa la IP local de tu PC (ej: 192.168.1.X)
 */
object RetrofitClient {
    
    // Cambia esta URL según tu entorno:
    // - Android Emulator: "http://10.0.2.2:5050/api/"
    // - Dispositivo físico: "http://TU_IP_LOCAL:5050/api/"
    private const val BASE_URL = "http://10.0.2.2:5050/api/"
    
    /**
     * Cliente HTTP con logging para debugging
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Instancia de Retrofit
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Servicio API de Reportes
     */
    val reportesApi: ReportesApiService by lazy {
        retrofit.create(ReportesApiService::class.java)
    }
    
    /**
     * Servicio API de Reservas
     */
    val reservasApi: ReservasApiService by lazy {
        retrofit.create(ReservasApiService::class.java)
    }
}
