package Reservas

import java.util.*

/**
 * Gestor singleton para almacenamiento temporal de solicitudes de reserva.
 * 
 * Maneja el ciclo de vida de las solicitudes:
 * - PENDIENTE: Recién creada, esperando revisión del administrador
 * - APROBADA: Administrador aprobó la solicitud
 * - RECHAZADA: Administrador rechazó la solicitud con una razón
 * 
 * NOTA: Este es almacenamiento TEMPORAL en memoria. Los datos se pierden al cerrar la app.
 * Debe ser reemplazado por base de datos (MongoDB) para persistencia real.
 * 
 * TODO: Reemplazar con integración de MongoDB
 * TODO: Implementar sincronización con backend
 * TODO: Agregar notificaciones push cuando cambia el estado
 */
object SolicitudesManager {
    
    private val solicitudes = mutableListOf<SolicitudReserva>()
    
    /**
     * Agrega una nueva solicitud de reserva.
     * Por defecto se crea con estado PENDIENTE.
     * 
     * @param solicitud La solicitud a agregar
     */
    fun agregarSolicitud(solicitud: SolicitudReserva) {
        solicitudes.add(solicitud)
    }
    
    /**
     * Obtiene todas las solicitudes pendientes (no aprobadas ni rechazadas).
     * 
     * @return Lista de solicitudes con estado PENDIENTE
     */
    fun obtenerSolicitudesPendientes(): List<SolicitudReserva> {
        return solicitudes.filter { it.estado == EstadoSolicitud.PENDIENTE }
    }
    
    /**
     * Obtiene todas las solicitudes independientemente del estado.
     * 
     * @return Lista completa de solicitudes
     */
    fun obtenerTodasLasSolicitudes(): List<SolicitudReserva> {
        return solicitudes.toList()
    }
    
    /**
     * Aprueba una solicitud cambiando su estado a APROBADA.
     * 
     * @param solicitud La solicitud a aprobar
     */
    fun aprobarSolicitud(solicitud: SolicitudReserva) {
        val originalIndex = solicitudes.indexOf(solicitud)
        if (originalIndex != -1) {
            solicitudes[originalIndex] = solicitud.copy(
                estado = EstadoSolicitud.APROBADA,
                fechaRespuesta = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Rechaza una solicitud cambiando su estado a RECHAZADA y guardando la razón.
     * 
     * @param solicitud La solicitud a rechazar
     * @param razon Razón del rechazo proporcionada por el administrador
     */
    fun rechazarSolicitud(solicitud: SolicitudReserva, razon: String) {
        val originalIndex = solicitudes.indexOf(solicitud)
        if (originalIndex != -1) {
            solicitudes[originalIndex] = solicitud.copy(
                estado = EstadoSolicitud.RECHAZADA,
                razonRechazo = razon,
                fechaRespuesta = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Limpia todas las solicitudes (útil para testing).
     */
    fun limpiar() {
        solicitudes.clear()
    }
}

/**
 * Enum que representa los posibles estados de una solicitud.
 */
enum class EstadoSolicitud {
    PENDIENTE,   // Esperando revisión del administrador
    APROBADA,    // Aprobada por el administrador
    RECHAZADA    // Rechazada por el administrador
}

/**
 * Clase de datos que representa una solicitud de reserva.
 * 
 * @property espacio Nombre del espacio solicitado
 * @property residente Nombre del residente que solicita
 * @property fecha Fecha de la reserva solicitada
 * @property horaInicio Hora de inicio de la reserva
 * @property horaFin Hora de finalización de la reserva
 * @property cantidad Cantidad de personas
 * @property observaciones Comentarios adicionales del residente
 * @property estado Estado actual de la solicitud
 * @property razonRechazo Razón del rechazo (solo si estado es RECHAZADA)
 * @property fechaCreacion Timestamp de cuándo se creó la solicitud
 * @property fechaRespuesta Timestamp de cuándo se aprobó/rechazó (null si pendiente)
 */
data class SolicitudReserva(
    val espacio: String,
    val residente: String,
    val fecha: Date,
    val horaInicio: Date,
    val horaFin: Date,
    val cantidad: Int,
    val observaciones: String = "",
    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    val razonRechazo: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaRespuesta: Long? = null
)
