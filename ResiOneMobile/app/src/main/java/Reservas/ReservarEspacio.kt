package Reservas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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
import api.CrearReservaRequest
import api.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity principal para gestionar reservas de espacios.
 * 
 * Funcionalidades principales:
 * - Visualización de calendario mensual con días coloreados según estado de reservas
 * - Creación de nuevas reservas con validación de datos y detección de conflictos
 * - Visualización, edición y eliminación de reservas existentes
 * - Sistema de permisos: solo el creador o UsuarioAdmin pueden modificar/eliminar
 * - Simulación de cambio de usuario para testing
 * - Estos ultimos dos sistemas se deben conectar al login cuando esté hecho, y se tiene que borrar el botón de cambio de usuario y
 *   toda logica implementada para eso.
 * - De igual manera, se están usando los nombres para pruebas, se tiene que cambiar la comparación a los ID de los usuarios en vez de sus nombres
 *   una vez ya esté implementado el login.
 * - También hay que agregar lo de que en vez de guardarlo de una vez mande un correo al admin para confirmar la reserva y tal
 *   pero no me dio la mente para eso.
 */
class ReservarEspacio : BaseActivity() {

    // ============ COMPONENTES DE UI ============
    private lateinit var etFecha: EditText              // Campo de texto para mostrar fecha seleccionada
    private lateinit var etHoraInicio: EditText         // Campo de texto para hora de inicio
    private lateinit var etHoraFinal: EditText          // Campo de texto para hora de finalización
    private lateinit var etCantidad: EditText           // Campo de texto para cantidad de personas
    private lateinit var spinnerEspacio: Spinner        // Selector dropdown de espacios disponibles
    private lateinit var btnEnviar: Button              // Botón para enviar/crear nueva reserva
    private lateinit var calendarRecycler: RecyclerView // RecyclerView que muestra el calendario mensual
    private lateinit var adapter: CalendarMonthAdapter  // Adapter para manejar días del calendario

    // ============ DATOS DE SESIÓN Y ALMACENAMIENTO ============
    // Lista en memoria de todas las reservas (no persiste entre sesiones)
    // NOTA: Reemplazar con Room DB o MongoDB cuando se integre persistencia real
    private val inMemoryReservas = mutableListOf<ReservaLight>()
    
    // ============ FORMATOS DE FECHA ============
    private val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())  // Para mostrar fechas al usuario
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())           // Para mostrar horas
    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())       // Para comparar fechas internamente
    
    // Calendario que mantiene el mes actual visualizado (para navegación prev/next)
    private var currentMonthCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reservar_espacio)
        
        // ============ CONFIGURACIÓN DE TOOLBAR ============
        // Configurar MaterialToolbar como ActionBar de la actividad para habilitar menú de opciones
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Configurar edge-to-edge display y ajustar padding para barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ============ INICIALIZACIÓN DE VISTAS ============
        // Vincular componentes del layout XML con variables de la clase
        spinnerEspacio = findViewById(R.id.spinner_espacio)
        etFecha = findViewById(R.id.et_fecha)
        val btnPickDate = findViewById<ImageButton>(R.id.btn_pick_date)
        etHoraInicio = findViewById(R.id.et_horaInicio)
        val horaInicioPick = findViewById<ImageButton>(R.id.horaInicioPick)
        etHoraFinal = findViewById(R.id.et_HoraFinal)
        val horaFinalPick = findViewById<ImageButton>(R.id.HoraFinalPick)
        etCantidad = findViewById(R.id.editTextNumber)
        btnEnviar = findViewById(R.id.btn_enviar)
        calendarRecycler = findViewById(R.id.calendarRecycler)

        // ============ CONFIGURACIÓN DEL SELECTOR DE FECHA ============
        // Botón que abre DatePickerDialog para seleccionar fecha de reserva
        // Validación: No permite fechas anteriores a hoy
        btnPickDate.setOnClickListener {
            val c = Calendar.getInstance()
            val minDate = Calendar.getInstance()
            minDate.set(Calendar.HOUR_OF_DAY, 0)
            minDate.set(Calendar.MINUTE, 0)
            minDate.set(Calendar.SECOND, 0)

            val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)

                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)

                if (selectedDate.before(today)) {
                    Toast.makeText(this, "No se pueden reservar fechas anteriores a hoy", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                etFecha.setText(dateFormat.format(selectedDate.time))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

            dpd.datePicker.minDate = minDate.timeInMillis
            dpd.show()
        }

        // ============ CONFIGURACIÓN DEL SELECTOR DE HORA DE INICIO ============
        // Botón que abre TimePickerDialog para seleccionar hora de inicio
        // Validación: Si la fecha es hoy, no permite horas pasadas
        horaInicioPick.setOnClickListener {
            val c = Calendar.getInstance()
            val currentHour = c.get(Calendar.HOUR_OF_DAY)
            val currentMinute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this, { _, hourOfDay, minute ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)

                val now = Calendar.getInstance()

                val etFechaText = etFecha.text.toString()
                if (etFechaText.isNotEmpty()) {
                    val parts = etFechaText.split(" / ")
                    if (parts.size == 3) {
                        val day = parts[0].toIntOrNull() ?: 0
                        val month = parts[1].toIntOrNull() ?: 0
                        val year = parts[2].toIntOrNull() ?: 0

                        val todayDay = now.get(Calendar.DAY_OF_MONTH)
                        val todayMonth = now.get(Calendar.MONTH) + 1
                        val todayYear = now.get(Calendar.YEAR)

                        if (day == todayDay && month == todayMonth && year == todayYear) {
                            if (selectedDateTime.before(now)) {
                                Toast.makeText(this, "La hora de inicio no puede ser anterior a la hora actual", Toast.LENGTH_SHORT).show()
                                return@TimePickerDialog
                            }
                        }
                    }
                }

                etHoraInicio.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, currentHour, currentMinute, true)
            tpd.show()
        }

        // ============ CONFIGURACIÓN DEL SELECTOR DE HORA FINAL ============
        // Botón que abre TimePickerDialog para seleccionar hora de finalización
        // Validaciones:
        // 1. Requiere que hora de inicio esté ya seleccionada
        // 2. Hora final debe ser posterior a hora de inicio
        // 3. Si la fecha es hoy, no permite horas pasadas
        horaFinalPick.setOnClickListener {
            val c = Calendar.getInstance()
            val currentHour = c.get(Calendar.HOUR_OF_DAY)
            val currentMinute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this, { _, hourOfDay, minute ->
                val horaInicioText = etHoraInicio.text.toString()
                if (horaInicioText.isNotEmpty()) {
                    val inicParts = horaInicioText.split(":")
                    if (inicParts.size == 2) {
                        val inicHour = inicParts[0].toIntOrNull() ?: 0
                        val inicMinute = inicParts[1].toIntOrNull() ?: 0

                        if (hourOfDay < inicHour || (hourOfDay == inicHour && minute <= inicMinute)) {
                            Toast.makeText(this, "La hora de finalización debe ser posterior a la hora de inicio", Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }
                    }
                } else {
                    Toast.makeText(this, "Por favor selecciona la hora de inicio primero", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)

                val now = Calendar.getInstance()

                val etFechaText = etFecha.text.toString()
                if (etFechaText.isNotEmpty()) {
                    val parts = etFechaText.split(" / ")
                    if (parts.size == 3) {
                        val day = parts[0].toIntOrNull() ?: 0
                        val month = parts[1].toIntOrNull() ?: 0
                        val year = parts[2].toIntOrNull() ?: 0

                        val todayDay = now.get(Calendar.DAY_OF_MONTH)
                        val todayMonth = now.get(Calendar.MONTH) + 1
                        val todayYear = now.get(Calendar.YEAR)

                        if (day == todayDay && month == todayMonth && year == todayYear) {
                            if (selectedDateTime.before(now)) {
                                Toast.makeText(this, "La hora de finalización no puede ser anterior a la hora actual", Toast.LENGTH_SHORT).show()
                                return@TimePickerDialog
                            }
                        }
                    }
                }

                etHoraFinal.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, currentHour, currentMinute, true)
            tpd.show()
        }

        // ============ CONFIGURACIÓN DEL CALENDARIO ============
        // Inicializar adapter del calendario con grid de 7 columnas (días de la semana)
        // Al hacer click en un día, se muestra diálogo con las reservas de ese día
        adapter = CalendarMonthAdapter(generateMonthDays(currentMonthCalendar)) { day ->
            showReservationsForDay(day)  // Callback al hacer click en un día
        }
        calendarRecycler.layoutManager = GridLayoutManager(this, 7)  // 7 columnas = 1 semana
        calendarRecycler.adapter = adapter

        // ============ CONFIGURACIÓN DE NAVEGACIÓN DE MESES ============
        val monthLabel = findViewById<TextView>(R.id.month_label)
        val prev = findViewById<ImageButton>(R.id.prev_month)
        val next = findViewById<ImageButton>(R.id.next_month)

        // Función auxiliar para actualizar el label del mes (ej: "NOVIEMBRE 2025")
        fun refreshMonthLabel() {
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            monthLabel.text = fmt.format(currentMonthCalendar.time).toUpperCase(Locale.getDefault())
        }

        refreshMonthLabel()  // Mostrar mes actual al inicio

        // Botón para retroceder un mes
        prev.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            adapter.updateDays(generateMonthDays(currentMonthCalendar))
            refreshMonthLabel()
        }

        // Botón para avanzar un mes
        next.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            adapter.updateDays(generateMonthDays(currentMonthCalendar))
            refreshMonthLabel()
        }

        // ============ CONFIGURACIÓN DEL BOTÓN ENVIAR RESERVA ============
        // Botón que crea una nueva reserva después de validar todos los campos
        // y verificar que no existan conflictos de horario
        btnEnviar.setOnClickListener {
            val espacio = spinnerEspacio.selectedItem?.toString()?.trim() ?: ""
            val fecha = etFecha.text.toString().trim()
            val horaInicio = etHoraInicio.text.toString().trim()
            val horaFinal = etHoraFinal.text.toString().trim()
            val cantidadStr = etCantidad.text.toString().trim()

            // Validación 1: Espacio no vacío
            if (espacio.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona un espacio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación 2: Fecha no vacía
            if (fecha.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona una fecha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación 3: Hora de inicio no vacía
            if (horaInicio.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona la hora de inicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación 4: Hora de final no vacía
            if (horaFinal.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona la hora de finalización", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación 5: Cantidad no vacía y mayor a 0
            if (cantidadStr.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa la cantidad de personas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cantidad = cantidadStr.toIntOrNull() ?: 0
            if (cantidad <= 0) {
                Toast.makeText(this, "La cantidad de personas debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse fecha into Date
            val fechaDate = try {
                dateFormat.parse(fecha)
            } catch (e: Exception) { null }

            if (fechaDate == null) {
                Toast.makeText(this, "Fecha inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Todas las validaciones pasaron — preparar la reserva con el creador actual. IMPORTANTE CAMBIARLO A BASE DE DATOS EN MONGODB
            val reserva = ReservaLight("", espacio, fechaDate, horaInicio, horaFinal, cantidad, currentUser)  // ID temporal, se asignará al guardar en backend

            // ============ DETECCIÓN DE CONFLICTOS DE HORARIO ============
            // Verificar si existe solapamiento con otras reservas en el mismo día y espacio
            // Función auxiliar: convierte tiempo "HH:mm" a minutos desde medianoche
            fun timeToMinutes(t: String): Int {
                val parts = t.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                return h * 60 + m
            }

            val newStart = timeToMinutes(horaInicio)
            val newEnd = timeToMinutes(horaFinal)
            val fechaKey = keyFormat.format(fechaDate)

            // Filtrar solo reservas confirmadas del mismo día y espacio
            // Las solicitudes pendientes NO bloquean el calendario hasta ser aprobadas
            val reservasConfirmadas = ReservasConfirmadasManager.obtenerReservas()
                .filter { keyFormat.format(it.fecha) == fechaKey && it.espacio == espacio }

            // Detectar conflictos únicamente con reservas confirmadas
            val conflict = reservasConfirmadas.any { existing ->
                val exStart = timeToMinutes(existing.horaInicio)
                val exEnd = timeToMinutes(existing.horaFinal)
                !(newEnd <= exStart || newStart >= exEnd)
            }

            if (conflict) {
                Toast.makeText(this, "Conflicto: ya existe una reserva confirmada en ese espacio con horas solapadas", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Crear las fechas completas con hora para la solicitud
            val calInicio = Calendar.getInstance()
            calInicio.time = fechaDate
            val horaInicioParts = horaInicio.split(":")
            calInicio.set(Calendar.HOUR_OF_DAY, horaInicioParts[0].toInt())
            calInicio.set(Calendar.MINUTE, horaInicioParts[1].toInt())
            
            val calFin = Calendar.getInstance()
            calFin.time = fechaDate
            val horaFinalParts = horaFinal.split(":")
            calFin.set(Calendar.HOUR_OF_DAY, horaFinalParts[0].toInt())
            calFin.set(Calendar.MINUTE, horaFinalParts[1].toInt())

            // Crear solicitud en el backend
            crearSolicitudEnBackend(espacio, fechaDate, horaInicio, horaFinal, cantidad)
        }

        // ============ CONFIGURACIÓN DEL BOTÓN DE CAMBIO DE USUARIO (SIMULACIÓN) ============
        // Configurar botón de simulación de cambio de usuario heredado de BaseActivity
        // NOTA: Este botón es SOLO para testing y debe ser removido en producción
        setupUserSwitchButton(R.id.btn_switch_user)
        
        // Cargar reservas aprobadas desde el backend al iniciar
        cargarReservasDesdeBackend()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar reservas cada vez que se vuelve a la pantalla
        cargarReservasDesdeBackend()
    }
    
    /**
     * Crea una solicitud de reserva en el backend usando la API
     */
    private fun crearSolicitudEnBackend(
        espacio: String, 
        fecha: Date, 
        horaInicio: String, 
        horaFinal: String, 
        cantidad: Int
    ) {
        lifecycleScope.launch {
            try {
                // Convertir fecha a formato ISO (YYYY-MM-DD)
                val fechaISO = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fecha)
                
                val request = CrearReservaRequest(
                    zona = espacio,
                    fecha = fechaISO,
                    horaInicio = horaInicio,
                    horaFin = horaFinal,
                    numeroPersonas = cantidad,
                    creador = "USER_ID_${currentUser}",  // TODO: Reemplazar con ID real del usuario cuando exista login
                    residente = currentUser,
                    correoResidente = "$currentUser@resione.com"
                )
                
                val response = RetrofitClient.reservasApi.crearReserva(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@ReservarEspacio,
                        "✓ Solicitud enviada. Esperando aprobación del administrador",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Limpiar campos del formulario
                    etFecha.text.clear()
                    etHoraInicio.text.clear()
                    etHoraFinal.text.clear()
                    etCantidad.text.clear()
                    
                    // Refrescar calendario
                    cargarReservasDesdeBackend()
                    
                } else {
                    Toast.makeText(
                        this@ReservarEspacio,
                        "Error al crear solicitud: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@ReservarEspacio,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Carga todas las reservas aprobadas desde el backend y actualiza el calendario
     */
    private fun cargarReservasDesdeBackend() {
        lifecycleScope.launch {
            try {
                // Obtener solo reservas aprobadas para mostrar en el calendario
                val response = RetrofitClient.reservasApi.obtenerReservasAprobadas()
                
                if (response.isSuccessful && response.body() != null) {
                    val reservasBackend = response.body()!!
                    
                    // Limpiar manager local y agregar reservas del backend
                    ReservasConfirmadasManager.limpiar()
                    
                    reservasBackend.forEach { reservaBackend ->
                        try {
                            // Convertir fecha ISO a Date
                            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(reservaBackend.fecha)
                            
                            if (fecha != null) {
                                val reservaLocal = ReservaLight(
                                    id = reservaBackend.id,      // Preservar el _id de MongoDB
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
                    adapter.updateDays(generateMonthDays(currentMonthCalendar))
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Muestra un diálogo con la lista de reservas confirmadas para un día específico.
     * Solo muestra reservas aprobadas cargadas desde MongoDB.
     * Al seleccionar una reserva de la lista, abre el diálogo de detalles.
     * 
     * @param day El día del calendario seleccionado por el usuario
     */
    private fun showReservationsForDay(day: CalendarDay) {
        val key = keyFormat.format(day.date)
        
        // Obtener reservas confirmadas desde el backend
        val reservasConfirmadas = ReservasConfirmadasManager.obtenerReservas()
            .filter { keyFormat.format(it.fecha) == key }
        
        if (reservasConfirmadas.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin reservas")
                .setMessage("No hay reservas confirmadas para este día")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Crear lista de labels para reservas confirmadas
        val labels = reservasConfirmadas.map { 
            "✓ ${it.horaInicio}-${it.horaFinal} • ${it.espacio} • ${it.cantidad}p (Confirmada)" 
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Reservas: ${day.dayNumber}")
            .setItems(labels) { _, which ->
                val selected = reservasConfirmadas[which]
                showReservationDetailDialog(selected)
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * Muestra diálogo con detalles completos de una reserva.
     * Incluye información del creador, espacio, horario y cantidad de personas.
     * 
     * Sistema de permisos:
     * - Solo el creador de la reserva puede Editar/Cancelar
     * - UsuarioAdmin puede Editar/Cancelar cualquier reserva
     * - Otros usuarios solo ven el botón Cerrar
     * 
     * @param reserva La reserva a mostrar en el diálogo
     */
    private fun showReservationDetailDialog(reserva: ReservaLight) {
        val details = StringBuilder()
        details.append("Reserva hecha por: ${reserva.creador}\n")
        details.append("Espacio: ${reserva.espacio}\n")
        details.append("Hora de reserva: ${reserva.horaInicio} - ${reserva.horaFinal}\n")
        details.append("Cantidad de personas: ${reserva.cantidad}\n")

        // Verificar permisos: solo el creador o UsuarioAdmin pueden modificar
        val canModify = (currentUser == reserva.creador || currentUser == "UsuarioAdmin")

        val builder = AlertDialog.Builder(this)
            .setTitle("Detalle de reserva")
            .setMessage(details.toString())

        // Mostrar botones Editar/Cancelar solo si el usuario tiene permisos
        if (canModify) {
            builder.setPositiveButton("Editar") { _, _ -> openEditDialog(reserva) }
                .setNegativeButton("Cancelar") { _, _ -> processCancellation(reserva) }
        }

        builder.setNeutralButton("Cerrar", null)
            .show()
    }

    /**
     * Procesa la cancelación de una reserva aplicando políticas de tiempo.
     * 
     * Políticas de cancelación:
     * - ≥ 24 horas antes: Cancelación exitosa sin penalización
     * - < 24 horas pero > 2 horas antes: Cancelación tardía (se registra)
     * - ≤ 2 horas antes: Cancelación no permitida
     * 
     * TODO: Implementar envío de código SMS para validar la cancelación
     * TODO: Implementar envío de correo electrónico para notificar cancelación tardía
     * 
     * @param reserva La reserva a cancelar
     */
    private fun processCancellation(reserva: ReservaLight) {
        // Obtener fecha y hora actual
        val now = Calendar.getInstance()
        
        // Construir fecha y hora de la reserva
        val reservaDateTime = Calendar.getInstance().apply {
            time = reserva.fecha
            // Parsear hora de inicio para obtener hora y minutos
            val timeParts = reserva.horaInicio.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calcular diferencia en horas
        val diffInMillis = reservaDateTime.timeInMillis - now.timeInMillis
        val diffInHours = diffInMillis / (1000 * 60 * 60.0)

        when {
            // Caso 1: Menos de 2 horas - NO PERMITIR cancelación
            diffInHours <= 2 -> {
                AlertDialog.Builder(this)
                    .setTitle("Cancelación no permitida")
                    .setMessage("No se puede cancelar una reserva con menos de 2 horas de anticipación.\n\nTiempo restante: ${String.format("%.1f", diffInHours)} horas")
                    .setPositiveButton("Entendido", null)
                    .show()
            }
            
            // Caso 2: Entre 2 y 24 horas - Cancelación TARDÍA
            diffInHours < 24 -> {
                AlertDialog.Builder(this)
                    .setTitle("Confirmar cancelación tardía")
                    .setMessage("⚠️ ADVERTENCIA: Cancelación tardía\n\n" +
                            "Estás cancelando con menos de 24 horas de anticipación (${String.format("%.1f", diffInHours)} horas).\n\n" +
                            "Esta acción quedará registrada como cancelación tardía.\n\n" +
                            "¿Deseas continuar?")
                    .setPositiveButton("Sí, cancelar") { _, _ ->
                        // TODO: Aquí se debe enviar código SMS para validar la cancelación
                        
                        // Eliminar reserva del backend
                        lifecycleScope.launch {
                            try {
                                val response = RetrofitClient.reservasApi.eliminarReserva(reserva.id)
                                if (response.isSuccessful) {
                                    // Mostrar advertencia de cancelación tardía
                                    // TODO: Aquí se debe enviar correo electrónico notificando cancelación tardía
                                    Toast.makeText(
                                        this@ReservarEspacio, 
                                        "⚠️ Reserva cancelada (TARDÍA). Se ha registrado esta cancelación.", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                    
                                    // Recargar desde backend
                                    cargarReservasDesdeBackend()
                                } else {
                                    Toast.makeText(
                                        this@ReservarEspacio,
                                        "Error al cancelar reserva",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    this@ReservarEspacio,
                                    "Error de conexión: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("No, mantener reserva", null)
                    .show()
            }
            
            // Caso 3: 24 horas o más - Cancelación NORMAL (sin penalización)
            else -> {
                AlertDialog.Builder(this)
                    .setTitle("Confirmar cancelación")
                    .setMessage("¿Estás seguro de que deseas cancelar esta reserva?\n\n" +
                            "Tiempo de anticipación: ${String.format("%.1f", diffInHours)} horas\n" +
                            "Sin penalización.")
                    .setPositiveButton("Sí, cancelar") { _, _ ->
                        // TODO: Aquí se debe enviar código SMS para validar la cancelación
                        
                        // Eliminar reserva del backend
                        lifecycleScope.launch {
                            try {
                                val response = RetrofitClient.reservasApi.eliminarReserva(reserva.id)
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        this@ReservarEspacio,
                                        "✓ Reserva cancelada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Recargar desde backend
                                    cargarReservasDesdeBackend()
                                } else {
                                    Toast.makeText(
                                        this@ReservarEspacio,
                                        "Error al cancelar reserva",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    this@ReservarEspacio,
                                    "Error de conexión: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }

    /**
     * Abre diálogo de edición para modificar una reserva existente.
     * 
     * Permite editar:
     * - Espacio reservado (dropdown)
     * - Hora de inicio (formato: hh-mm, validado estrictamente)
     * - Hora de finalización (formato: hh-mm, validado estrictamente)
     * - Cantidad de personas (número)
     * 
     * Validaciones al guardar:
     * 1. Formato de tiempo debe ser exactamente "hh-mm" (00-23 para horas, 00-59 para minutos)
     * 2. Verifica conflictos con otras reservas del mismo día y espacio
     * 3. Excluye la reserva actual del check de conflictos (permite modificar sin conflicto consigo misma)
     * 
     * @param reserva La reserva a editar
     */
    private fun openEditDialog(reserva: ReservaLight) {
        val ctx = this
        // Crear layout vertical para el formulario de edición
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // ============ CREAR CAMPOS DEL FORMULARIO ============
        // Spinner para seleccionar espacio (carga opciones desde recursos)
        val espacioSpinner = Spinner(ctx)
        val adapterArr = ArrayAdapter.createFromResource(ctx, R.array.espacios_array, android.R.layout.simple_spinner_item)
        adapterArr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        espacioSpinner.adapter = adapterArr
        val selectedIndex = (0 until adapterArr.count).firstOrNull { adapterArr.getItem(it) == reserva.espacio } ?: 0
        espacioSpinner.setSelection(selectedIndex)

        val horaInicioInput = EditText(ctx).apply { setText(reserva.horaInicio.replace(":","-")) }
        val horaFinalInput = EditText(ctx).apply { setText(reserva.horaFinal.replace(":","-")) }
        val cantidadInput = EditText(ctx).apply { setText(reserva.cantidad.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER }

        layout.addView(TextView(ctx).apply { text = "Espacio" })
        layout.addView(espacioSpinner)
        layout.addView(TextView(ctx).apply { text = "Hora inicio (hh-mm)" })
        layout.addView(horaInicioInput)
        layout.addView(TextView(ctx).apply { text = "Hora final (hh-mm)" })
        layout.addView(horaFinalInput)
        layout.addView(TextView(ctx).apply { text = "Cantidad" })
        layout.addView(cantidadInput)

        AlertDialog.Builder(ctx)
            .setTitle("Editar reserva")
            .setView(layout)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newEspacio = espacioSpinner.selectedItem?.toString() ?: reserva.espacio
                val newHoraInicio = horaInicioInput.text.toString().trim()
                val newHoraFinal = horaFinalInput.text.toString().trim()
                val newCant = cantidadInput.text.toString().toIntOrNull() ?: reserva.cantidad

                // ============ VALIDACIÓN ESTRICTA DE FORMATO DE TIEMPO ============
                // Regex que valida formato exacto: hh-mm
                // - Horas: 00-23 (acepta [01]\d para 00-19, o 2[0-3] para 20-23)
                // - Minutos: 00-59 (acepta [0-5]\d)
                val timePattern = Regex("^([01]\\d|2[0-3])\\-([0-5]\\d)$")
                if (!timePattern.matches(newHoraInicio) || !timePattern.matches(newHoraFinal)) {
                    Toast.makeText(ctx, "Formato inválido: use hh-mm (ej. 09-30). Horas 00-23, minutos 00-59", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                fun normalize(t: String) = t.replace('-', ':')

                fun timeToMinutes(t: String): Int {
                    val norm = t.replace('-', ':')
                    val parts = norm.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    return h * 60 + m
                }

                val newStart = timeToMinutes(newHoraInicio)
                val newEnd = timeToMinutes(newHoraFinal)
                val fechaKey = keyFormat.format(reserva.fecha)

                // Filtrar reservas del mismo día y espacio, EXCLUYENDO la reserva actual
                // (it !== reserva) permite que la reserva editada no entre en conflicto consigo misma
                val reservasConfirmadas = ReservasConfirmadasManager.obtenerReservas()
                val sameDaySameSpace = reservasConfirmadas.filter { keyFormat.format(it.fecha) == fechaKey && it.espacio == newEspacio && it !== reserva }

                // Verificar solapamiento con otras reservas
                val conflict = sameDaySameSpace.any { existing ->
                    val exStart = timeToMinutes(existing.horaInicio)
                    val exEnd = timeToMinutes(existing.horaFinal)
                    !(newEnd <= exStart || newStart >= exEnd)
                }

                if (conflict) {
                    Toast.makeText(ctx, "Conflicto: la modificación produce solapamiento", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // ============ APLICAR CAMBIOS EN BACKEND ============
                // Normalizar tiempos de "hh-mm" a "HH:mm" para almacenamiento consistente
                val saveInicio = normalize(newHoraInicio)  // Convierte "09-30" a "09:30"
                val saveFinal = normalize(newHoraFinal)
                
                // Llamar al backend para actualizar la reserva en MongoDB
                lifecycleScope.launch {
                    try {
                        // Convertir fecha a formato ISO
                        val fechaISO = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(reserva.fecha)
                        
                        val request = ActualizarReservaRequest(
                            zona = newEspacio,
                            fecha = fechaISO,
                            horaInicio = saveInicio,
                            horaFin = saveFinal,
                            numeroPersonas = newCant
                        )
                        
                        val response = RetrofitClient.reservasApi.actualizarReserva(reserva.id, request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(ctx, "✓ Reserva actualizada exitosamente", Toast.LENGTH_SHORT).show()
                            
                            // Recargar reservas desde el backend
                            cargarReservasDesdeBackend()
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                            Toast.makeText(ctx, "Error al actualizar: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(ctx, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Genera lista de días para mostrar en el calendario mensual.
     * 
     * Crea un grid de 42 días (6 semanas x 7 días) que incluye:
     * - Días del mes actual
     * - Días finales del mes anterior (para completar primera semana)
     * - Días iniciales del mes siguiente (para completar última semana)
     * 
     * Cada día incluye:
     * - Fecha completa
     * - Número del día (para mostrar)
     * - Flag indicando si pertenece al mes actual (para aplicar dimming)
     * - Estado (NONE, PENDING, COMPLETED) basado en reservas existentes
     * 
     * @param monthCal Calendario configurado al mes que se quiere generar
     * @return Lista de 42 CalendarDay para mostrar en el RecyclerView
     */
    private fun generateMonthDays(monthCal: Calendar): List<CalendarDay> {
        val cal = monthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun

        // start from Sunday of the week containing the 1st
        val start = cal.clone() as Calendar
        start.add(Calendar.DAY_OF_MONTH, -(firstDayOfWeek - 1))

        val days = mutableListOf<CalendarDay>()
        val today = Calendar.getInstance()

        for (i in 0 until 42) { // 6 weeks
            val d = start.clone() as Calendar
            d.add(Calendar.DAY_OF_MONTH, i)
            val inMonth = d.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
            val status = determineStatus(d.time, today.time)
            days.add(CalendarDay(d.time, String.format("%02d", d.get(Calendar.DAY_OF_MONTH)), inMonth, status))
        }

        return days
    }

    /**
     * Determina el estado visual de un día en el calendario basado en sus reservas y solicitudes.
     * 
     * Lógica de colores:
     * - COMPLETED (verde): Fechas pasadas que tienen reservas confirmadas
     * - PENDING (azul): Fecha actual o futura que tiene reservas confirmadas o solicitudes pendientes
     * - NONE (sin color): Días sin reservas ni solicitudes
     * 
     * @param date Fecha del día a evaluar
     * @param today Fecha actual para comparación
     * @return Estado del día (NONE, PENDING, COMPLETED)
     */
    private fun determineStatus(date: Date, today: Date): ReservaStatus {
        val key = keyFormat.format(date)
        val hasReservaConfirmada = ReservasConfirmadasManager.obtenerReservas()
            .any { keyFormat.format(it.fecha) == key }
        
        // Solo mostrar estado basado en reservas aprobadas del backend
        return if (!date.after(today) && !isSameDay(date, today)) {
            if (hasReservaConfirmada) ReservaStatus.COMPLETED else ReservaStatus.NONE
        } else {
            if (hasReservaConfirmada) ReservaStatus.PENDING else ReservaStatus.NONE
        }
    }

    /**
     * Compara si dos fechas corresponden al mismo día calendario.
     * Ignora horas, minutos y segundos.
     * 
     * @param a Primera fecha
     * @param b Segunda fecha
     * @return true si ambas fechas son el mismo día (año, mes y día iguales)
     */
    private fun isSameDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
                && ca.get(Calendar.DAY_OF_MONTH) == cb.get(Calendar.DAY_OF_MONTH)
    }
}

/**
 * Clase de datos ligera para representar una reserva en memoria.
 * 
 * NOTA: Esta clase es temporal para demostración. Eventualmente será reemplazada por datos en MongoDB.
 * 
 * @property espacio Nombre del espacio reservado (ej: "Sala de juntas", "Cancha deportiva")
 * @property fecha Fecha de la reserva
 * @property horaInicio Hora de inicio en formato "HH:mm" (ej: "09:30")
 * @property horaFinal Hora de finalización en formato "HH:mm" (ej: "11:00")
 * @property cantidad Número de personas para la reserva
 * @property creador Nombre del usuario que creó la reserva (para control de permisos)
 */
data class ReservaLight(
    val id: String,              // MongoDB _id para actualizar en backend
    val espacio: String,
    val fecha: Date,
    val horaInicio: String,
    val horaFinal: String,
    val cantidad: Int,
    val creador: String
)
