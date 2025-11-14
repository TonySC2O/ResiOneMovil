package Reportes

import android.net.Uri
import java.util.Date

/**
 * Gestor singleton para almacenamiento temporal de reportes en memoria.
 * 
 * Esta clase mantiene una lista compartida de reportes que puede ser accedida
 * desde cualquier Activity de la aplicación. Los reportes se mantienen en memoria
 * durante la sesión de la app.
 * 
 * NOTA: Este es un almacenamiento TEMPORAL para pruebas. Los datos se perderán
 * al cerrar la aplicación. Debe ser reemplazado por MongoDB para persistencia real.
 * 
 * TODO: Reemplazar con integración de MongoDB
 * TODO: Implementar sincronización con backend
 */
object ReportesManager {
    
    // Lista en memoria de todos los reportes creados
    private val reportes = mutableListOf<ReporteData>()
    
    // Contador para generar números de seguimiento únicos
    private var contadorSeguimiento = 1
    
    /**
     * Genera un número de seguimiento único para un nuevo reporte.
     * Formato: INC-YYYYMMDD-XXXX
     * Ejemplo: INC-20251114-0001
     * 
     * @return Número de seguimiento único
     */
    fun generarNumeroSeguimiento(): String {
        val fecha = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val numero = String.format("%04d", contadorSeguimiento)
        contadorSeguimiento++
        return "INC-$fecha-$numero"
    }
    
    /**
     * Agrega un nuevo reporte a la lista.
     * Por defecto, los reportes se crean con estado PENDIENTE y sin técnico asignado.
     * 
     * @param reporte El reporte a agregar
     */
    fun agregarReporte(reporte: ReporteData) {
        reportes.add(reporte)
    }
    
    /**
     * Obtiene todos los reportes almacenados.
     * 
     * @return Lista de todos los reportes (copia para evitar modificaciones externas)
     */
    fun obtenerReportes(): List<ReporteData> {
        return reportes.toList()
    }
    
    /**
     * Actualiza el estado de un reporte existente.
     * 
     * @param index Índice del reporte en la lista
     * @param nuevoEstado Nuevo estado a asignar
     */
    fun actualizarEstado(index: Int, nuevoEstado: ReporteEstado) {
        if (index in reportes.indices) {
            reportes[index] = reportes[index].copy(estado = nuevoEstado)
        }
    }
    
    /**
     * Asigna un técnico a un reporte y registra la fecha/hora de asignación.
     * 
     * @param index Índice del reporte en la lista
     * @param tecnico Nombre del técnico asignado
     */
    fun asignarTecnico(index: Int, tecnico: String) {
        if (index in reportes.indices) {
            reportes[index] = reportes[index].copy(
                tecnicoAsignado = tecnico,
                fechaAsignacionTecnico = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Limpia todos los reportes (útil para testing).
     */
    fun limpiar() {
        reportes.clear()
    }
}

/**
 * Clase de datos que representa un reporte completo.
 * 
 * @property numeroSeguimiento Número único de seguimiento del reporte
 * @property tipo Tipo de incidencia reportada (Eléctrica, Sanitaria, Ruido, Accesos, Limpieza, Infraestructura)
 * @property descripcion Descripción detallada del problema
 * @property prioridad Nivel de prioridad (Baja, Media, Alta)
 * @property fecha Fecha del incidente (formato Date)
 * @property archivosMultimedia Lista de URIs de archivos adjuntos (imágenes/videos)
 * @property creador Nombre del usuario que creó el reporte
 * @property estado Estado actual del reporte (PENDIENTE, ANALISIS, RESUELTO)
 * @property tecnicoAsignado Nombre del técnico asignado (null si no hay técnico)
 * @property fechaAsignacionTecnico Timestamp de cuándo se asignó el técnico (null si no hay técnico asignado)
 * @property fechaCreacion Timestamp de cuándo se creó el reporte en el sistema
 */
data class ReporteData(
    val numeroSeguimiento: String,
    val tipo: String,
    val descripcion: String,
    val prioridad: String,
    val fecha: Date,
    val archivosMultimedia: List<Uri>,
    val creador: String,
    val estado: ReporteEstado = ReporteEstado.PENDIENTE,
    val tecnicoAsignado: String? = null,
    val fechaAsignacionTecnico: Long? = null,
    val fechaCreacion: Long = System.currentTimeMillis()
)
