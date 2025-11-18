package com.example.resionemobile.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query


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
    @DELETE("posts/{id}")
    fun deletePost(@Path("id") id: String): Call<GenericPostResponse>


    // ------------------------
    // Pagos y Finanzas
    // ------------------------

    @POST("finanzas/cuotas")
    fun crearCuota(@Body body: CrearCuotaRequest): Call<CrearCuotaResponse>

    @GET("finanzas/cuotas")
    fun obtenerCuotas(): Call<List<Cuota>>

    @Multipart
    @POST("finanzas/pagos")
    fun registrarPago(
        @Part("cuotaId") cuotaId: String,
        @Part("residenteId") residenteId: String,
        @Part("nombreResidente") nombreResidente: String,
        @Part("unidadHabitacional") unidadHabitacional: String,
        @Part("fechaPago") fechaPago: String,
        @Part("metodoPago") metodoPago: String,
        @Part comprobantePDF: MultipartBody.Part?
    ): Call<Pago>

    @GET("finanzas/pagos/historial")
    fun historialPagos(
        @Query("residenteId") residenteId: String? = null
    ): Call<List<Pago>>

    @POST("finanzas/facturas")
    fun emitirFactura(@Body body: Map<String, String>): Call<FacturaResponse>


    // ------------------------
    // Seguridad y Acceso
    // ------------------------

    @POST("seguridad/entrada")
    fun registrarEntrada(@Body body: EntradaRequest): Call<EntradaResponse>

    @POST("seguridad/salida")
    fun registrarSalida(@Body body: SalidaRequest): Call<GenericResponse>

    @GET("seguridad/bitacora")
    fun obtenerBitacora(): Call<BitacoraResponse>
}
