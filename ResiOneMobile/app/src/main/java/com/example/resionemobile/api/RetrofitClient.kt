package com.example.resionemobile.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // --- AJUSTA ESTA CONSTANTE SEGÚN TU ENTORNO ---
    // Emulador Android: "http://10.0.2.2:5050/api/"
    // Dispositivo físico: "http://<TU_IP_LOCAL>:5050/api/"
    private const val BASE_URL = "http://10.0.2.2:5050/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}