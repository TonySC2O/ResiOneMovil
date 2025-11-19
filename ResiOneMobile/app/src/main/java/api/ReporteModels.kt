package api

import com.google.gson.annotations.SerializedName

/**
 * Modelos de datos para comunicación con el backend de Reportes
 */

/**
 * Request para crear un nuevo reporte
 * Se envía al endpoint POST /api/reportes
 */
data class CrearReporteRequest(
    val tipo: String,                    // Tipo de incidencia
    val descripcion: String,             // Descripción detallada
    val nivelPrioridad: String,          // Baja, Media, Alta
    val archivos: List<String> = emptyList(),  // Array de URLs (vacío por ahora)
    val correoResidente: String          // Email del residente
)

/**
 * Response al crear un reporte
 * Recibida del endpoint POST /api/reportes
 */
data class CrearReporteResponse(
    val mensaje: String,
    val reporte: ReporteBackend
)

/**
 * Modelo de reporte como lo devuelve el backend
 * Compatible con el modelo MongoDB
 */
data class ReporteBackend(
    @SerializedName("_id")
    val id: String,
    val tipo: String,                    // Eléctrica, Sanitaria, Ruido, etc.
    val descripcion: String,             // Descripción del problema
    val nivelPrioridad: String,          // Baja, Media, Alta
    val archivos: List<String> = emptyList(),  // Array de URLs (puede estar vacío)
    val fecha: String,                   // Fecha en formato "YYYY-MM-DD"
    val estado: String,                  // Pendiente, Analisis, Resuelto
    val seguimiento: String,             // Número de seguimiento generado (ej: R98840)
    val comentariosAdmin: String = ""    // Comentarios del admin (opcional)
)

/**
 * Request para cambiar el estado de un reporte
 * Se envía al endpoint PUT /api/reportes/:id
 */
data class CambiarEstadoRequest(
    val nuevoEstado: String,
    val comentarios: String,
    val codigo: String,
    val codigoValido: Boolean,
    val correoResidente: String
)
