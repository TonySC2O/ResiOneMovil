package Reservas

import java.util.*

/**
 * Gestor singleton para almacenamiento temporal de reservas confirmadas.
 * 
 * Estas son reservas que han sido aprobadas por el administrador.
 * Bloquean los espacios en el calendario y no permiten solicitudes solapadas.
 * 
 * NOTA: Este es almacenamiento TEMPORAL en memoria. Los datos se pierden al cerrar la app.
 * Debe ser reemplazado por base de datos (MongoDB) para persistencia real.
 * 
 * TODO: Reemplazar con integración de MongoDB
 * TODO: Implementar sincronización con backend
 */
object ReservasConfirmadasManager {
    
    private val reservas = mutableListOf<ReservaLight>()
    
    /**
     * Agrega una nueva reserva confirmada al sistema.
     * 
     * @param reserva La reserva a agregar
     */
    fun agregarReserva(reserva: ReservaLight) {
        reservas.add(reserva)
    }
    
    /**
     * Obtiene todas las reservas confirmadas.
     * 
     * @return Lista de reservas confirmadas
     */
    fun obtenerReservas(): List<ReservaLight> {
        return reservas.toList()
    }
    
    /**
     * Elimina una reserva confirmada del sistema.
     * 
     * @param reserva La reserva a eliminar
     * @return true si se eliminó exitosamente, false si no se encontró
     */
    fun eliminarReserva(reserva: ReservaLight): Boolean {
        return reservas.remove(reserva)
    }
    
    /**
     * Limpia todas las reservas (útil para testing).
     */
    fun limpiar() {
        reservas.clear()
    }
}
