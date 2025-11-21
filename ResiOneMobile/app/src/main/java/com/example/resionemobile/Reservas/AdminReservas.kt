package com.example.resionemobile.Reservas

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.api.ReservaBackend
import com.example.resionemobile.api.AprobarReservaRequest
import com.example.resionemobile.api.AprobarReservaResponse
import com.example.resionemobile.api.RechazarReservaRequest
import com.example.resionemobile.api.RechazarReservaResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
 * - Solo accesible por administradores
 * - Opción de menú solo visible para administradores
 * 
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
    
    // Lista de reservas cargadas desde MongoDB
    private val reservasDesdeApi = mutableListOf<ReservaBackend>()

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
        
        // ============ CARGAR RESERVAS DESDE API ============
        cargarReservasDesdeApi()
        
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
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar reservas desde API al volver a la pantalla
        cargarReservasDesdeApi()
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
            
            // Verificar si hay reservas aprobadas para este día
            val hasReservasAprobadas = reservasDesdeApi.any { 
                it.estado == "aprobada" && keyFormat.format(parseDate(it.fecha)) == dateKey 
            }
            
            // Determinar estado del día (solo mostrar aprobadas en verde)
            val status = when {
                hasReservasAprobadas -> ReservaStatus.COMPLETED
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
     * Carga y muestra todas las solicitudes pendientes desde la API ordenadas cronológicamente.
     */
    private fun loadSolicitudesPendientes() {
        solicitudesContainer.removeAllViews()
        
        val solicitudes = reservasDesdeApi
            .filter { it.estado == "pendiente" }
            .sortedWith(compareBy({ parseDate(it.fecha) }, { it.horaInicio }))
        
        if (solicitudes.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No hay solicitudes pendientes"
                textSize = 16f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.GRAY)
            }
            solicitudesContainer.addView(emptyView)
            return
        }
        
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
            
            // Llenar datos
            tvEspacio.text = solicitud.zona
            tvResidente.text = "Residente: ${solicitud.residenteNombre} (${solicitud.residenteApartamento})"
            tvFecha.text = "Fecha: ${formatearFecha(solicitud.fecha)}"
            tvHora.text = "Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}"
            tvCantidad.text = "Cantidad: ${solicitud.numeroPersonas} personas"
            
            if (solicitud.comentarios.isNotBlank()) {
                tvObservaciones.visibility = android.view.View.VISIBLE
                tvObservaciones.text = "Observaciones: ${solicitud.comentarios}"
            } else {
                tvObservaciones.visibility = android.view.View.GONE
            }
            
            // Configurar botones
            btnAprobar.setOnClickListener {
                aprobarSolicitud(solicitud)
            }
            
            btnRechazar.setOnClickListener {
                rechazarSolicitud(solicitud)
            }
            
            solicitudesContainer.addView(itemView)
        }
    }
    
    /**
     * Muestra un diálogo con las reservas aprobadas para un día específico.
     */
    private fun showSolicitudesForDay(date: Date) {
        val dateKey = keyFormat.format(date)
        val reservasDelDia = reservasDesdeApi
            .filter { it.estado == "aprobada" && keyFormat.format(parseDate(it.fecha)) == dateKey }
            .sortedBy { it.horaInicio }
        
        if (reservasDelDia.isEmpty()) {
            Toast.makeText(this, "No hay reservas aprobadas para este día", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = reservasDelDia.map { 
            "${it.zona} - ${it.horaInicio} a ${it.horaFin} (${it.residenteNombre})"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Reservas confirmadas para ${dateFormat.format(date)}")
            .setItems(items, null)
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * Aprueba una solicitud vía API y bloquea el espacio en el calendario.
     * Cambia el estado de la reserva a "aprobada".
     * 
     * TODO: Enviar notificación por correo al residente
     */
    private fun aprobarSolicitud(solicitud: ReservaBackend) {
        val detalles = StringBuilder()
        detalles.append("Residente: ${solicitud.residenteNombre}\n")
        detalles.append("Apartamento: ${solicitud.residenteApartamento}\n")
        detalles.append("Espacio: ${solicitud.zona}\n")
        detalles.append("Fecha: ${formatearFecha(solicitud.fecha)}\n")
        detalles.append("Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}\n")
        detalles.append("Cantidad: ${solicitud.numeroPersonas} personas\n")
        if (solicitud.comentarios.isNotBlank()) {
            detalles.append("Observaciones: ${solicitud.comentarios}\n")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Aprobar solicitud")
            .setMessage("${detalles}\n¿Deseas aprobar esta solicitud?")
            .setPositiveButton("Aprobar") { _, _ ->
                // Verificar conflictos localmente antes de enviar al backend
                val conflicto = verificarConflictoReservas(solicitud)
                if (conflicto != null) {
                    Toast.makeText(
                        this,
                        " Conflicto detectado: Ya existe una reserva aprobada para ${conflicto.zona} el ${formatearFecha(conflicto.fecha)} de ${conflicto.horaInicio} a ${conflicto.horaFin}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }
                
                // Si no hay conflicto local, enviar al backend (que hará verificación definitiva)
                aprobarSolicitudEnApi(solicitud.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Rechaza una solicitud solicitando la razón del rechazo.
     * ELIMINA el documento de la base de datos.
     * 
     * TODO: Enviar notificación por correo al residente con la razón
     */
    private fun rechazarSolicitud(solicitud: ReservaBackend) {
        // Validar que tengamos información del administrador
        if (currentUser?.correo.isNullOrBlank()) {
            Toast.makeText(this, "Error: Información del administrador no disponible", Toast.LENGTH_LONG).show()
            return
        }
        
        val detalles = StringBuilder()
        detalles.append("Residente: ${solicitud.residenteNombre}\n")
        detalles.append("Apartamento: ${solicitud.residenteApartamento}\n")
        detalles.append("Espacio: ${solicitud.zona}\n")
        detalles.append("Fecha: ${formatearFecha(solicitud.fecha)}\n")
        detalles.append("Horario: ${solicitud.horaInicio} - ${solicitud.horaFin}\n")
        detalles.append("Cantidad: ${solicitud.numeroPersonas} personas\n")
        if (solicitud.comentarios.isNotBlank()) {
            detalles.append("Observaciones: ${solicitud.comentarios}")
        }
        
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
                
                rechazarSolicitudEnApi(solicitud.id, razon)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    // ============ VALIDACIONES ============

    /**
     * Verifica si una solicitud tiene conflictos con reservas ya aprobadas.
     * Esta es una verificación PREVIA en el cliente para dar feedback inmediato.
     * El backend hará la verificación definitiva al aprobar.
     * 
     * @param solicitud La solicitud a verificar
     * @return La reserva con la que hay conflicto, o null si no hay conflictos
     */
    private fun verificarConflictoReservas(solicitud: ReservaBackend): ReservaBackend? {
        val dateKey = keyFormat.format(parseDate(solicitud.fecha))
        
        // Obtener SOLO las reservas APROBADAS del mismo día y espacio
        val reservasAprobadas = reservasDesdeApi.filter { 
            it.estado == "aprobada" && 
            keyFormat.format(parseDate(it.fecha)) == dateKey && 
            it.zona == solicitud.zona &&
            it.id != solicitud.id  // Excluir la solicitud misma
        }
        
        if (reservasAprobadas.isEmpty()) return null
        
        // Convertir horas a minutos para comparación
        fun timeToMinutes(time: String): Int {
            val parts = time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        
        val solicitudInicio = timeToMinutes(solicitud.horaInicio)
        val solicitudFin = timeToMinutes(solicitud.horaFin)
        
        // Buscar solapamiento
        for (reserva in reservasAprobadas) {
            val reservaInicio = timeToMinutes(reserva.horaInicio)
            val reservaFin = timeToMinutes(reserva.horaFin)
            
            // Hay solapamiento si: inicio1 < fin2 && fin1 > inicio2
            val haySolapamiento = solicitudInicio < reservaFin && solicitudFin > reservaInicio
            
            if (haySolapamiento) {
                return reserva
            }
        }
        
        return null
    }

    // ============ FUNCIONES DE API ============

    /**
     * Carga todas las reservas desde MongoDB y actualiza las vistas.
     */
    private fun cargarReservasDesdeApi() {
        // Cargar todas las reservas (tanto pendientes como aprobadas)
        RetrofitClient.api.obtenerReservas().enqueue(object : Callback<List<ReservaBackend>> {
            override fun onResponse(call: Call<List<ReservaBackend>>, response: Response<List<ReservaBackend>>) {
                if (response.isSuccessful) {
                    val todasLasReservas = response.body() ?: emptyList()
                    
                    // Log para depuración
                    android.util.Log.d("AdminReservas", "Total reservas recibidas: ${todasLasReservas.size}")
                    todasLasReservas.forEach { reserva ->
                        android.util.Log.d("AdminReservas", "Reserva: ${reserva.id} - Estado: ${reserva.estado} - Zona: ${reserva.zona}")
                    }
                    
                    // Filtrar solo reservas válidas (pendiente o aprobada)
                    reservasDesdeApi.clear()
                    reservasDesdeApi.addAll(todasLasReservas.filter { 
                        it.estado == "pendiente" || it.estado == "aprobada" 
                    })
                    
                    android.util.Log.d("AdminReservas", "Reservas válidas (pendiente/aprobada): ${reservasDesdeApi.size}")
                    android.util.Log.d("AdminReservas", "Reservas pendientes: ${reservasDesdeApi.count { it.estado == "pendiente" }}")
                    android.util.Log.d("AdminReservas", "Reservas aprobadas: ${reservasDesdeApi.count { it.estado == "aprobada" }}")
                    
                    loadSolicitudesPendientes()
                    generateMonthDays()
                } else {
                    Toast.makeText(this@AdminReservas, "Error al cargar reservas: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ReservaBackend>>, t: Throwable) {
                Toast.makeText(this@AdminReservas, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Aprueba una solicitud vía API.
     */
    private fun aprobarSolicitudEnApi(reservaId: String) {
        val request = AprobarReservaRequest(
            administradorCorreo = currentUser?.correo ?: ""
        )

        RetrofitClient.api.aprobarReserva(reservaId, request).enqueue(object : Callback<AprobarReservaResponse> {
            override fun onResponse(call: Call<AprobarReservaResponse>, response: Response<AprobarReservaResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminReservas, "✓ Solicitud aprobada y espacio bloqueado", Toast.LENGTH_LONG).show()
                    
                    // TODO: Enviar notificación por correo al residente
                    
                    cargarReservasDesdeApi()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@AdminReservas, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AprobarReservaResponse>, t: Throwable) {
                Toast.makeText(this@AdminReservas, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Rechaza una solicitud vía API (ELIMINA el documento de MongoDB).
     */
    private fun rechazarSolicitudEnApi(reservaId: String, razon: String) {
        val adminCorreo = currentUser?.correo
        if (adminCorreo.isNullOrBlank()) {
            Toast.makeText(this@AdminReservas, "Error: Correo del administrador no disponible", Toast.LENGTH_LONG).show()
            return
        }
        
        val request = RechazarReservaRequest(
            razonRechazo = razon,
            administradorCorreo = adminCorreo
        )

        android.util.Log.d("AdminReservas", "Rechazando reserva: $reservaId con razón: $razon por admin: $adminCorreo")

        RetrofitClient.api.rechazarReserva(reservaId, request).enqueue(object : Callback<RechazarReservaResponse> {
            override fun onResponse(call: Call<RechazarReservaResponse>, response: Response<RechazarReservaResponse>) {
                if (response.isSuccessful) {
                    android.util.Log.d("AdminReservas", "Solicitud rechazada exitosamente")
                    Toast.makeText(this@AdminReservas, "✓ Solicitud rechazada: $razon", Toast.LENGTH_LONG).show()
                    
                    // TODO: Enviar notificación por correo al residente con la razón
                    
                    cargarReservasDesdeApi()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    android.util.Log.e("AdminReservas", "Error al rechazar: ${response.code()} - $errorBody")
                    Toast.makeText(this@AdminReservas, "Error al rechazar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RechazarReservaResponse>, t: Throwable) {
                android.util.Log.e("AdminReservas", "Error de conexión al rechazar", t)
                Toast.makeText(this@AdminReservas, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Parsea una fecha en formato ISO 8601 a Date.
     */
    private fun parseDate(isoDate: String): Date {
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            formato.timeZone = TimeZone.getTimeZone("UTC")
            formato.parse(isoDate) ?: Date()
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoDate) ?: Date()
            } catch (e2: Exception) {
                Date()
            }
        }
    }

    /**
     * Formatea una fecha ISO para mostrarla al usuario.
     */
    private fun formatearFecha(isoDate: String): String {
        val date = parseDate(isoDate)
        return dateFormat.format(date)
    }
}
