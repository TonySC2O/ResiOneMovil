package api

import com.google.gson.annotations.SerializedName

/**
 * Modelos de datos para comunicación con el backend de Reservas
 */

/**
 * Request para crear una nueva reserva (solicitud)
 * Se envía al endpoint POST /api/reservas
 */
data class CrearReservaRequest(
    val zona: String,                    // Nombre del espacio (ej: "Sala de juntas")
    val fecha: String,                   // Fecha en formato ISO (YYYY-MM-DD)
    val horaInicio: String,              // Hora de inicio (HH:mm)
    val horaFin: String,                 // Hora de fin (HH:mm)
    val numeroPersonas: Int,             // Cantidad de personas
    val creador: String,                 // ID del usuario que crea la reserva
    val residente: String,               // Nombre del residente
    val correoResidente: String          // Email del residente
)

/**
 * Response al crear una reserva
 * Recibida del endpoint POST /api/reservas
 */
data class CrearReservaResponse(
    val mensaje: String,
    val reserva: ReservaBackend
)

/**
 * Modelo de reserva como lo devuelve el backend
 * Compatible con el modelo MongoDB
 */
data class ReservaBackend(
    @SerializedName("_id")
    val id: String,
    val zona: String,                    // Nombre del espacio
    val fecha: String,                   // Fecha ISO
    val horaInicio: String,              // Hora inicio (HH:mm)
    val horaFin: String,                 // Hora fin (HH:mm)
    val numeroPersonas: Int,             // Cantidad de personas
    val comentarios: String,             // Comentario generado automáticamente
    val estado: String,                  // "pendiente", "aprobada", "rechazada"
    val creador: String,                 // ID del creador
    val residente: String,               // Nombre del residente
    val correoResidente: String,         // Email del residente
    val razonRechazo: String = "",       // Razón del rechazo (si aplica)
    val createdAt: String? = null,       // Timestamp de creación
    val updatedAt: String? = null        // Timestamp de actualización
)

/**
 * Request para actualizar una reserva (aprobar/rechazar o editar datos)
 * Se envía al endpoint PUT /api/reservas/:id
 */
data class ActualizarReservaRequest(
    val estado: String? = null,          // "aprobada" o "rechazada" (para workflow de aprobación)
    val razonRechazo: String? = null,    // Razón del rechazo (requerido si estado es "rechazada")
    val zona: String? = null,            // Para editar el espacio reservado
    val fecha: String? = null,           // Para editar la fecha (formato ISO)
    val horaInicio: String? = null,      // Para editar hora de inicio
    val horaFin: String? = null,         // Para editar hora de fin
    val numeroPersonas: Int? = null      // Para editar cantidad de personas
)
