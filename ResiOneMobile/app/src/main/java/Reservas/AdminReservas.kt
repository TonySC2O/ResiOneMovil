package Reservas

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import api.ActualizarReservaRequest
import api.ReservaBackend
import api.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity exclusiva para administradores para gestionar solicitudes de reservas.
 * 
 * Funcionalidades principales:
 * - Visualización de calendario mensual idéntico a ReservarEspacio
 * - Lista de solicitudes pendientes ordenadas cronológicamente
 * - Aprobación o rechazo de solicitudes
 * - Al aprobar: bloquea el espacio en el calendario
 * - Al rechazar: solicita razón del rechazo
 * 
 * Restricciones:
 * - Solo accesible por UsuarioAdmin
 * - Opción de menú solo visible para administradores
 * 
 * TODO: Conectar con sistema de autenticación real
 * TODO: Implementar notificaciones al residente sobre aprobación/rechazo
 * TODO: Integrar con base de datos para persistencia
 */
class AdminReservas : BaseActivity() {

    // ============ COMPONENTES DE UI ============
    private lateinit var calendarRecycler: RecyclerView
    private lateinit var adapter: CalendarMonthAdapter
    private lateinit var solicitudesContainer: LinearLayout
    
    // ============ FORMATOS DE FECHA ============
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Calendario para navegación de meses
    private var currentMonthCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_reservas)
        
        // ============ CONFIGURACIÓN DE TOOLBAR ============
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ============ INICIALIZACIÓN DE VISTAS ============
        calendarRecycler = findViewById(R.id.calendarRecycler)
        solicitudesContainer = findViewById(R.id.solicitudes_container)
        
        // ============ CONFIGURACIÓN DEL CALENDARIO ============
        setupCalendar()
        
        // ============ CARGAR DATOS DESDE BACKEND ============
        cargarReservasAprobadasDesdeBackend()
        loadSolicitudesPendientes()
        
        // ============ CONFIGURACIÓN DE NAVEGACIÓN DE MESES ============
        val prevMonthBtn = findViewById<ImageButton>(R.id.prev_month)
        val nextMonthBtn = findViewById<ImageButton>(R.id.next_month)
        
        prevMonthBtn.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            updateMonthLabel()
            generateMonthDays()
        }
        
        nextMonthBtn.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            updateMonthLabel()
            generateMonthDays()
        }
        
        // ============ CONFIGURACIÓN DEL BOTÓN DE CAMBIO DE USUARIO ============
        setupUserSwitchButton(R.id.btn_switch_user) {
            // Recargar datos cuando cambia el usuario
            cargarReservasAprobadasDesdeBackend()
            loadSolicitudesPendientes()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar solicitudes y calendario al volver a la pantalla
        loadSolicitudesPendientes()
        generateMonthDays()
    }
    
    /**
     * Configura el RecyclerView del calendario con GridLayoutManager de 7 columnas.
     */
    private fun setupCalendar() {
        calendarRecycler.layoutManager = GridLayoutManager(this, 7)
        
        adapter = CalendarMonthAdapter(
            emptyList(),
            onDayClick = { day ->
                // Mostrar solicitudes para este día
                showSolicitudesForDay(day.date)
            }
        )
        
        calendarRecycler.adapter = adapter
        
        updateMonthLabel()
        generateMonthDays()
    }
    
    /**
     * Actualiza la etiqueta del mes actual en el calendario.
     */
    private fun updateMonthLabel() {
        val monthLabel = findViewById<TextView>(R.id.month_label)
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthLabel.text = monthFormat.format(currentMonthCalendar.time).uppercase()
    }
    
    /**
     * Genera los 42 días (6 semanas) para mostrar en el calendario mensual.
     * Colorea los días según el estado de las reservas.
     */
    private fun generateMonthDays() {
        val cal = currentMonthCalendar.clone() as Calendar
        val currentMonth = cal.get(Calendar.MONTH)
        
        // Ir al primer día del mes
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        // Retroceder al primer día de la semana (domingo)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1))
        
        val days = mutableListOf<CalendarDay>()
        
        // Generar 42 días (6 semanas completas)
        for (i in 0 until 42) {
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthOfDay = cal.get(Calendar.MONTH)
            val isCurrentMonth = monthOfDay == currentMonth
            
            val dateKey = keyFormat.format(cal.time)
            
            // Verificar si hay reservas confirmadas para este día
            val hasReservasConfirmadas = ReservasConfirmadasManager.obtenerReservas()
                .any { keyFormat.format(it.fecha) == dateKey }
            
            // Determinar estado del día (solo mostrar confirmadas)
            val status = when {
                hasReservasConfirmadas -> ReservaStatus.COMPLETED
                else -> ReservaStatus.NONE
            }
            
            days.add(CalendarDay(
                date = cal.time,
                dayNumber = String.format("%02d", dayOfMonth),
                inMonth = isCurrentMonth,
                status = status
            ))
            
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        adapter.updateDays(days)
    }
    
    /**
     * Carga reservas aprobadas desde el backend y actualiza el gestor local
     */
    private fun cargarReservasAprobadasDesdeBackend() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reservasApi.obtenerReservasAprobadas()
                
                if (response.isSuccessful && response.body() != null) {
                    val reservasBackend = response.body()!!
                    
                    // Limpiar y actualizar gestor local
                    ReservasConfirmadasManager.limpiar()
                    
                    reservasBackend.forEach { reservaBackend ->
                        try {
                            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(reservaBackend.fecha)
                            
                            if (fecha != null) {
                                val reservaLocal = ReservaLight(
                                    id = reservaBackend.id,
                                    espacio = reservaBackend.zona,
                                    fecha = fecha,
                                    horaInicio = reservaBackend.horaInicio,
                                    horaFinal = reservaBackend.horaFin,
                                    cantidad = reservaBackend.numeroPersonas,
                                    creador = reservaBackend.residente
                                )
                                ReservasConfirmadasManager.agregarReserva(reservaLocal)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // Actualizar calendario
                    generateMonthDays()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Carga y muestra todas las solicitudes pendientes desde el backend.
     */
    private fun loadSolicitudesPendientes() {
        solicitudesContainer.removeAllViews()
        
        // Mostrar mensaje de carga
        val loadingView = TextView(this).apply {
            text = "Cargando solicitudes..."
            textSize = 16f
            setPadding(16, 32, 16, 32)
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.GRAY)
        }
        solicitudesContainer.addView(loadingView)
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reservasApi.obtenerReservasPendientes()
                
                if (response.isSuccessful && response.body() != null) {
                    val solicitudes = response.body()!!
                        .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                    
                    solicitudesContainer.removeAllViews()
                    
                    if (solicitudes.isEmpty()) {
                        val emptyView = TextView(this@AdminReservas).apply {
                            text = "No hay solicitudes pendientes"
                            textSize = 16f
                            setPadding(16, 32, 16, 32)
                            gravity = android.view.Gravity.CENTER
                            setTextColor(android.graphics.Color.GRAY)
                        }
                        solicitudesContainer.addView(emptyView)
                        return@launch
                    }
                    
                    mostrarSolicitudes(solicitudes)
                }
            } catch (e: Exception) {
                solicitudesContainer.removeAllViews()
                val errorView = TextView(this@AdminReservas).apply {
                    text = "Error al cargar solicitudes: ${e.message}"
                    textSize = 16f
                    setPadding(16, 32, 16, 32)
                    gravity = android.view.Gravity.CENTER
                    setTextColor(android.graphics.Color.RED)
                }
                solicitudesContainer.addView(errorView)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Muestra la lista de solicitudes en la interfaz
     */
    private fun mostrarSolicitudes(solicitudes: List<ReservaBackend>) {
        solicitudes.forEachIndexed { index, solicitud ->
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_solicitud_reserva, solicitudesContainer, false)
            
            // Configurar vistas del item
            val tvEspacio = itemView.findViewById<TextView>(R.id.tv_solicitud_espacio)
            val tvResidente = itemView.findViewById<TextView>(R.id.tv_solicitud_residente)
            val tvFecha = itemView.findViewById<TextView>(R.id.tv_solicitud_fecha)
            val tvHora = itemView.findViewById<TextView>(R.id.tv_solicitud_hora)
            val tvCantidad = itemView.findViewById<TextView>(R.id.tv_solicitud_cantidad)
            val tvObservaciones = itemView.findViewById<TextView>(R.id.tv_solicitud_observaciones)
            val btnAprobar = itemView.findViewById<Button>(R.id.btn_aprobar)
            val btnRechazar = itemView.findViewById<Button>(R.id.btn_rechazar)
            
            // Llenar datos desde backend
            tvEspacio.text = solicitud.zona
            tvResidente.text = "Residente: ${solicitud.residente}"
            
            // Parsear fecha
            try {
                val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(solicitud.fecha)
                if (fecha != null) {
                    tvFecha.text = "Fecha: ${dateFormat.format(fecha)}"
                }
            } catch (e: Exception) {
                tvFecha.text = "Fecha: ${solicitud.fecha}"
            }
            
            tvHora.text = "Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}"
            tvCantidad.text = "Cantidad: ${solicitud.numeroPersonas} personas"
            
            // Mostrar comentarios (generados automáticamente)
            if (solicitud.comentarios.isNotBlank()) {
                tvObservaciones.visibility = android.view.View.VISIBLE
                tvObservaciones.text = solicitud.comentarios
            } else {
                tvObservaciones.visibility = android.view.View.GONE
            }
            
            // Configurar botones
            btnAprobar.setOnClickListener {
                aprobarSolicitudBackend(solicitud)
            }
            
            btnRechazar.setOnClickListener {
                rechazarSolicitudBackend(solicitud)
            }
            
            solicitudesContainer.addView(itemView)
        }
    }
    
    /**
     * Muestra un diálogo con las reservas confirmadas para un día específico.
     */
    private fun showSolicitudesForDay(date: Date) {
        val dateKey = keyFormat.format(date)
        val reservasDelDia = ReservasConfirmadasManager.obtenerReservas()
            .filter { keyFormat.format(it.fecha) == dateKey }
            .sortedBy { it.horaInicio }
        
        if (reservasDelDia.isEmpty()) {
            Toast.makeText(this, "No hay reservas confirmadas para este día", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = reservasDelDia.map { 
            "${it.espacio} - ${it.horaInicio} a ${it.horaFinal} (${it.creador})"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Reservas confirmadas para ${dateFormat.format(date)}")
            .setItems(items, null)
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * Aprueba una solicitud de reserva usando la API del backend
     */
    private fun aprobarSolicitudBackend(solicitud: ReservaBackend) {
        val detalles = StringBuilder()
        detalles.append("Residente: ${solicitud.residente}\n")
        detalles.append("Espacio: ${solicitud.zona}\n")
        detalles.append("Fecha: ${solicitud.fecha}\n")
        detalles.append("Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}\n")
        detalles.append("Cantidad: ${solicitud.numeroPersonas} personas\n")
        
        AlertDialog.Builder(this)
            .setTitle("Aprobar solicitud")
            .setMessage("${detalles}\n¿Deseas aprobar esta solicitud?")
            .setPositiveButton("Aprobar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val request = ActualizarReservaRequest(estado = "aprobada")
                        val response = RetrofitClient.reservasApi.actualizarReserva(solicitud.id, request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AdminReservas,
                                "✓ Solicitud aprobada y espacio bloqueado",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Recargar solicitudes y calendario desde backend
                            loadSolicitudesPendientes()
                            cargarReservasAprobadasDesdeBackend()
                        } else {
                            Toast.makeText(
                                this@AdminReservas,
                                "Error al aprobar: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AdminReservas,
                            "Error de conexión: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Rechaza una solicitud de reserva usando la API del backend
     */
    private fun rechazarSolicitudBackend(solicitud: ReservaBackend) {
        val detalles = StringBuilder()
        detalles.append("Residente: ${solicitud.residente}\n")
        detalles.append("Espacio: ${solicitud.zona}\n")
        detalles.append("Fecha: ${solicitud.fecha}\n")
        detalles.append("Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}\n")
        detalles.append("Cantidad: ${solicitud.numeroPersonas} personas\n")
        
        val inputEditText = EditText(this).apply {
            hint = "Razón del rechazo"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Rechazar solicitud")
            .setMessage("${detalles}\n\nIngresa la razón del rechazo:")
            .setView(inputEditText)
            .setPositiveButton("Rechazar") { _, _ ->
                val razon = inputEditText.text.toString().trim()
                
                if (razon.isEmpty()) {
                    Toast.makeText(this, "Debes ingresar una razón para rechazar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                lifecycleScope.launch {
                    try {
                        // Eliminar la reserva directamente del backend
                        val response = RetrofitClient.reservasApi.eliminarReserva(solicitud.id)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AdminReservas,
                                "✓ Solicitud rechazada y eliminada: $razon",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Recargar solicitudes
                            loadSolicitudesPendientes()
                        } else {
                            Toast.makeText(
                                this@AdminReservas,
                                "Error al rechazar: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AdminReservas,
                            "Error de conexión: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Verifica si una solicitud tiene conflictos con reservas ya confirmadas
     * Y con otras solicitudes que ya fueron aprobadas en esta sesión.
     * Retorna mensaje de error o null si no hay conflictos.
     * 
     * Conflicto = mismo espacio + mismo día + solapamiento de horarios
     */
    private fun verificarConflictoReservas(solicitud: SolicitudReserva): ReservaLight? {
        val dateKey = keyFormat.format(solicitud.fecha)
        
        // Obtener TODAS las reservas confirmadas del día y espacio
        val reservasDelDia = ReservasConfirmadasManager.obtenerReservas().filter { 
            keyFormat.format(it.fecha) == dateKey && it.espacio == solicitud.espacio
        }
        
        if (reservasDelDia.isEmpty()) return null
        
        // Extraer solo las horas y minutos para comparación
        val calSolicitudInicio = Calendar.getInstance().apply { time = solicitud.horaInicio }
        val solicitudHoraInicio = calSolicitudInicio.get(Calendar.HOUR_OF_DAY) * 60 + calSolicitudInicio.get(Calendar.MINUTE)
        
        val calSolicitudFin = Calendar.getInstance().apply { time = solicitud.horaFin }
        val solicitudHoraFin = calSolicitudFin.get(Calendar.HOUR_OF_DAY) * 60 + calSolicitudFin.get(Calendar.MINUTE)
        
        for (reserva in reservasDelDia) {
            // Parsear las horas de la reserva confirmada (formato "HH:mm")
            val horaParts = reserva.horaInicio.split(":")
            val reservaHoraInicio = horaParts[0].toInt() * 60 + horaParts[1].toInt()
            
            val horaFinParts = reserva.horaFinal.split(":")
            val reservaHoraFin = horaFinParts[0].toInt() * 60 + horaFinParts[1].toInt()
            
            // Verificar solapamiento de horarios (comparando minutos desde medianoche)
            val haySolapamiento = solicitudHoraInicio < reservaHoraFin && solicitudHoraFin > reservaHoraInicio
            
            if (haySolapamiento) {
                return reserva
            }
        }
        
        return null
    }
}
