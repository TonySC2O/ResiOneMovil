package api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz para las peticiones HTTP relacionadas con Reportes
 * Endpoints del backend: http://localhost:5050/api/reportes
 */
interface ReportesApiService {
    
    /**
     * Crear un nuevo reporte con archivos adjuntos (multipart)
     * POST /api/reportes
     */
    @Multipart
    @POST("reportes")
    suspend fun crearReporteConArchivos(
        @Part("tipo") tipo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("nivelPrioridad") nivelPrioridad: RequestBody,
        @Part("correoResidente") correoResidente: RequestBody,
        @Part archivos: List<MultipartBody.Part>
    ): Response<CrearReporteResponse>
    
    /**
     * Crear un nuevo reporte sin archivos (JSON)
     * POST /api/reportes
     */
    @POST("reportes")
    suspend fun crearReporte(
        @Body request: CrearReporteRequest
    ): Response<CrearReporteResponse>
    
    /**
     * Obtener todos los reportes
     * GET /api/reportes
     */
    @GET("reportes")
    suspend fun obtenerReportes(): Response<List<ReporteBackend>>
    
    /**
     * Cambiar el estado de un reporte
     * PUT /api/reportes/:id
     */
    @PUT("reportes/{id}")
    suspend fun cambiarEstado(
        @Path("id") id: String,
        @Body request: CambiarEstadoRequest
    ): Response<CrearReporteResponse>
}
