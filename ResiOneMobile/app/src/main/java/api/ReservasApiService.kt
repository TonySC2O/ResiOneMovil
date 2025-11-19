package api

import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz para las peticiones HTTP relacionadas con Reservas
 * Endpoints del backend: http://localhost:5050/api/reservas
 */
interface ReservasApiService {
    
    /**
     * Crear una nueva reserva (solicitud)
     * POST /api/reservas
     */
    @POST("reservas")
    suspend fun crearReserva(
        @Body request: CrearReservaRequest
    ): Response<CrearReservaResponse>
    
    /**
     * Obtener todas las reservas
     * GET /api/reservas
     * Opcional: filtrar por estado con query param ?estado=pendiente
     */
    @GET("reservas")
    suspend fun obtenerReservas(
        @Query("estado") estado: String? = null
    ): Response<List<ReservaBackend>>
    
    /**
     * Obtener solo reservas pendientes
     * GET /api/reservas?estado=pendiente
     */
    @GET("reservas")
    suspend fun obtenerReservasPendientes(
        @Query("estado") estado: String = "pendiente"
    ): Response<List<ReservaBackend>>
    
    /**
     * Obtener solo reservas aprobadas
     * GET /api/reservas?estado=aprobada
     */
    @GET("reservas")
    suspend fun obtenerReservasAprobadas(
        @Query("estado") estado: String = "aprobada"
    ): Response<List<ReservaBackend>>
    
    /**
     * Actualizar una reserva (aprobar/rechazar o editar)
     * PUT /api/reservas/:id
     */
    @PUT("reservas/{id}")
    suspend fun actualizarReserva(
        @Path("id") id: String,
        @Body request: ActualizarReservaRequest
    ): Response<CrearReservaResponse>
    
    /**
     * Eliminar una reserva
     * DELETE /api/reservas/:id
     */
    @DELETE("reservas/{id}")
    suspend fun eliminarReserva(
        @Path("id") id: String
    ): Response<Map<String, String>>
}
