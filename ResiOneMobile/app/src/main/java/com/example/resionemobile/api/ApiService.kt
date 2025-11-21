package com.example.resionemobile.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query


interface ApiService {
    @POST("registro")
    fun registro(@Body body: RegistroRequest): Call<GenericResponse>

    @POST("login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @PUT("editar")
    fun editar(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("comunicados/feed")
    fun getComunicados(): Call<ComunicadoListResponse>

    @POST("comunicados/crear")
    fun crearComunicado(@Body body: CrearComunicadoRequest): Call<ComunicadoResponse>

    @PUT("comunicados/editar/{id}")
    fun editarComunicado(@Path("id") id: String, @Body body: EditarComunicadoRequest): Call<ComunicadoResponse>

    @DELETE("comunicados/eliminar/{id}")
    fun eliminarComunicado(@Path("id") id: String): Call<GenericResponse>

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

    @POST("seguridad/entrada")
    fun registrarEntrada(@Body body: EntradaRequest): Call<EntradaResponse>

    @POST("seguridad/salida")
    fun registrarSalida(@Body body: SalidaRequest): Call<GenericResponse>

    @GET("seguridad/bitacora")
    fun obtenerBitacora(): Call<BitacoraResponse>

    @Multipart
    @POST("mantenimiento/registrar")
    fun registrarMantenimiento(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part fotosAntes: List<MultipartBody.Part>,
        @Part fotosDespues: List<MultipartBody.Part>
    ): Call<MantenimientoResponse>

    @GET("mantenimiento/historial")
    fun obtenerHistorialMante(): Call<HistorialManteResponse>

    // ===== REPORTES =====
    
    @POST("reportes/")
    fun crearReporte(@Body body: CrearReporteRequest): Call<CrearReporteResponse>

    @Multipart
    @POST("reportes/")
    fun crearReporteConArchivos(
        @Part("tipo") tipo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("nivelPrioridad") nivelPrioridad: RequestBody,
        @Part("fecha") fecha: RequestBody,
        @Part("residenteCorreo") residenteCorreo: RequestBody,
        @Part("residenteNombre") residenteNombre: RequestBody,
        @Part("residenteApartamento") residenteApartamento: RequestBody,
        @Part("residenteIdentificacion") residenteIdentificacion: RequestBody,
        @Part archivos: List<MultipartBody.Part>
    ): Call<CrearReporteResponse>

    @GET("reportes/")
    fun obtenerReportes(): Call<ReportesListResponse>

    @PUT("reportes/{id}")
    fun cambiarEstadoReporte(
        @Path("id") id: String,
        @Body body: CambiarEstadoRequest
    ): Call<GenericResponse>

    // ===== RESERVAS =====
    
    @GET("reservas/")
    fun obtenerReservas(
        @Query("estado") estado: String? = null,
        @Query("residenteCorreo") residenteCorreo: String? = null
    ): Call<List<ReservaBackend>>

    @POST("reservas/")
    fun crearReserva(@Body body: CrearReservaRequest): Call<CrearReservaResponse>

    @PUT("reservas/{id}/aprobar")
    fun aprobarReserva(
        @Path("id") id: String,
        @Body body: AprobarReservaRequest
    ): Call<AprobarReservaResponse>

    @DELETE("reservas/{id}/rechazar")
    fun rechazarReserva(
        @Path("id") id: String,
        @Body body: RechazarReservaRequest
    ): Call<RechazarReservaResponse>

    @PUT("reservas/{id}")
    fun actualizarReserva(
        @Path("id") id: String,
        @Body body: ActualizarReservaRequest
    ): Call<ActualizarReservaResponse>

    @DELETE("reservas/{id}")
    fun eliminarReserva(@Path("id") id: String): Call<GenericResponse>
}