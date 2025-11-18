package com.example.resionemobile.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.DELETE



interface ApiService {
    @POST("registro")
    fun registro(@Body body: RegistroRequest): Call<GenericResponse>

    @POST("login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @PUT("editar")
    fun editar(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    // --- COMUNICADOS (ajustadas a tu API actual) ---
    @GET("comunicados/feed")
    fun getComunicados(): Call<ComunicadoListResponse>

    @POST("comunicados/crear")
    fun crearComunicado(@Body body: CrearComunicadoRequest): Call<ComunicadoResponse>

    @PUT("comunicados/editar/{id}")
    fun editarComunicado(@Path("id") id: String, @Body body: EditarComunicadoRequest): Call<ComunicadoResponse>

    @DELETE("comunicados/eliminar/{id}")
    fun eliminarComunicado(@Path("id") id: String): Call<GenericResponse>
}
