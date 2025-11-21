package com.example.resionemobile.Reservas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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
import com.example.resionemobile.api.CrearReservaRequest
import com.example.resionemobile.api.CrearReservaResponse
import com.example.resionemobile.api.ActualizarReservaRequest
import com.example.resionemobile.api.ActualizarReservaResponse
import com.example.resionemobile.api.GenericResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity principal para gestionar reservas de espacios.
 * 
 * Funcionalidades principales:
 * - Visualización de calendario mensual con días coloreados según estado de reservas
 * - Creación de nuevas reservas con validación de datos y detección de conflictos
 * - Visualización, edición y eliminación de reservas existentes
 * - Sistema de permisos: solo el creador o administradores pueden modificar/eliminar
 * - Validación de horarios para evitar conflictos
 * - Identificación de usuarios por correo electrónico
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
    // Lista de reservas cargadas desde MongoDB
    private val reservasDesdeApi = mutableListOf<ReservaBackend>()
    
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

        // ============ CARGAR RESERVAS DESDE API ============
        cargarReservasDesdeApi()

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

            // Filtrar solo reservas APROBADAS del mismo día y espacio desde la API
            // Las solicitudes pendientes NO bloquean el calendario hasta ser aprobadas
            val reservasAprobadas = reservasDesdeApi
                .filter { it.estado == "aprobada" && keyFormat.format(parseDate(it.fecha)) == fechaKey && it.zona == espacio }

            // Detectar conflictos únicamente con reservas aprobadas
            val conflict = reservasAprobadas.any { existing ->
                val exStart = timeToMinutes(existing.horaInicio)
                val exEnd = timeToMinutes(existing.horaFin)
                !(newEnd <= exStart || newStart >= exEnd)
            }

            if (conflict) {
                Toast.makeText(this, "Conflicto: ya existe una reserva confirmada en ese espacio con horas solapadas", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Crear solicitud en el backend
            crearReservaEnApi(espacio, fechaDate, horaInicio, horaFinal, cantidad)
        }
    }

    /**
     * Muestra un diálogo con la lista de reservas desde MongoDB para un día específico.
     * Si no hay reservas, muestra mensaje informativo.
     * Al seleccionar una reserva de la lista, abre el diálogo de detalles.
     * 
     * @param day El día del calendario seleccionado por el usuario
     */
    private fun showReservationsForDay(day: CalendarDay) {
        val key = keyFormat.format(day.date)
        
        // Filtrar reservas del día desde la API
        val reservasDelDia = reservasDesdeApi.filter { 
            keyFormat.format(parseDate(it.fecha)) == key 
        }.sortedBy { it.horaInicio }
        
        if (reservasDelDia.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin reservas")
                .setMessage("No hay reservas ni solicitudes para este día")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Crear labels según el estado
        val labels = reservasDelDia.map { reserva ->
            val icono = if (reserva.estado == "aprobada") "✓" else "⏳"
            val estadoTexto = if (reserva.estado == "aprobada") "Confirmada" else "Pendiente"
            "$icono ${reserva.horaInicio}-${reserva.horaFin} • ${reserva.zona} • ${reserva.numeroPersonas}p ($estadoTexto)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Reservas: ${day.dayNumber}")
            .setItems(labels) { _, which ->
                val selected = reservasDelDia[which]
                showReservationDetailDialog(selected)
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * Muestra diálogo con detalles completos de una reserva desde MongoDB.
     * Incluye información del creador, espacio, horario y cantidad de personas.
     * 
     * Sistema de permisos:
     * - Solo el creador de la reserva puede Editar/Cancelar reservas APROBADAS
     * - Los administradores pueden Editar/Cancelar cualquier reserva APROBADA
     * - Solicitudes PENDIENTES solo se pueden ver (aprobación/rechazo es en AdminReservas)
     * - Otros usuarios solo ven el botón Cerrar
     * 
     * @param reserva La reserva a mostrar en el diálogo
     */
    private fun showReservationDetailDialog(reserva: ReservaBackend) {
        val details = StringBuilder()
        
        if (reserva.estado == "pendiente") {
            details.append("Estado: ⏳ PENDIENTE DE APROBACIÓN\n\n")
        } else {
            details.append("Estado: ✓ APROBADA\n\n")
        }
        
        details.append("Solicitante: ${reserva.residenteNombre}\n")
        details.append("Correo: ${reserva.residenteCorreo}\n")
        details.append("Apartamento: ${reserva.residenteApartamento}\n")
        details.append("Espacio: ${reserva.zona}\n")
        details.append("Fecha: ${formatearFecha(reserva.fecha)}\n")
        details.append("Hora: ${reserva.horaInicio} - ${reserva.horaFin}\n")
        details.append("Cantidad: ${reserva.numeroPersonas} personas\n")
        
        if (reserva.comentarios.isNotBlank()) {
            details.append("Comentarios: ${reserva.comentarios}\n")
        }
        
        if (reserva.estado == "aprobada" && reserva.administradorQueResponde != null) {
            details.append("\nAprobada por: ${reserva.administradorQueResponde}")
        }

        val esElCreador = currentUser?.correo == reserva.residenteCorreo
        val canModify = (esElCreador || esAdministrador) && reserva.estado == "aprobada"
        
        val builder = AlertDialog.Builder(this)
            .setTitle(if (reserva.estado == "pendiente") "Solicitud Pendiente" else "Reserva Confirmada")
            .setMessage(details.toString())

        // Mostrar botones Editar/Cancelar solo si el usuario tiene permisos Y la reserva está aprobada
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
     * @param reserva La reserva a cancelar (desde MongoDB)
     */
    private fun processCancellation(reserva: ReservaBackend) {
        // Obtener fecha y hora actual
        val now = Calendar.getInstance()
        
        // Construir fecha y hora de la reserva
        val reservaDateTime = Calendar.getInstance().apply {
            time = parseDate(reserva.fecha)
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
                        
                        // Eliminar reserva vía API
                        eliminarReservaEnApi(reserva.id, esTardía = true)
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
                        
                        // Eliminar reserva vía API
                        eliminarReservaEnApi(reserva.id, esTardía = false)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }

    /**
     * Abre diálogo de edición para modificar una reserva existente desde MongoDB.
     * 
     * Permite editar:
     * - Espacio reservado (dropdown)
     * - Hora de inicio (formato: hh-mm, validado estrictamente)
     * - Hora de finalización (formato: hh-mm, validado estrictamente)
     * - Cantidad de personas (número)
     * 
     * Validaciones al guardar:
     * 1. Formato de tiempo debe ser exactamente "hh-mm" (00-23 para horas, 00-59 para minutos)
     * 2. Verifica conflictos con otras reservas del mismo día y espacio via API
     * 3. Excluye la reserva actual del check de conflictos
     * 
     * @param reserva La reserva a editar (desde MongoDB)
     */
    private fun openEditDialog(reserva: ReservaBackend) {
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
        val selectedIndex = (0 until adapterArr.count).firstOrNull { adapterArr.getItem(it) == reserva.zona } ?: 0
        espacioSpinner.setSelection(selectedIndex)

        val horaInicioInput = EditText(ctx).apply { setText(reserva.horaInicio.replace(":","-")) }
        val horaFinalInput = EditText(ctx).apply { setText(reserva.horaFin.replace(":","-")) }
        val cantidadInput = EditText(ctx).apply { setText(reserva.numeroPersonas.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER }

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
                val newEspacio = espacioSpinner.selectedItem?.toString() ?: reserva.zona
                val newHoraInicio = horaInicioInput.text.toString().trim()
                val newHoraFinal = horaFinalInput.text.toString().trim()
                val newCant = cantidadInput.text.toString().toIntOrNull() ?: reserva.numeroPersonas

                // ============ VALIDACIÓN ESTRICTA DE FORMATO DE TIEMPO ============
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
                
                if (newEnd <= newStart) {
                    Toast.makeText(ctx, "La hora final debe ser posterior a la hora de inicio", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val fechaKey = keyFormat.format(parseDate(reserva.fecha))

                // Filtrar reservas aprobadas del mismo día y espacio, EXCLUYENDO la reserva actual
                val sameDaySameSpace = reservasDesdeApi.filter { 
                    it.estado == "aprobada" && 
                    keyFormat.format(parseDate(it.fecha)) == fechaKey && 
                    it.zona == newEspacio && 
                    it.id != reserva.id 
                }

                // Verificar solapamiento con otras reservas
                val conflict = sameDaySameSpace.any { existing ->
                    val exStart = timeToMinutes(existing.horaInicio)
                    val exEnd = timeToMinutes(existing.horaFin)
                    !(newEnd <= exStart || newStart >= exEnd)
                }

                if (conflict) {
                    Toast.makeText(ctx, "Conflicto: la modificación produce solapamiento", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // ============ APLICAR CAMBIOS VÍA API ============
                val saveInicio = normalize(newHoraInicio)  // Convierte "09-30" a "09:30"
                val saveFinal = normalize(newHoraFinal)
                
                actualizarReservaEnApi(reserva.id, newEspacio, reserva.fecha, saveInicio, saveFinal, newCant, reserva.comentarios)
                
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
     * Determina el estado visual de un día en el calendario basado en sus reservas desde MongoDB.
     * 
     * Lógica de colores:
     * - COMPLETED (verde): Fechas pasadas que tienen reservas aprobadas
     * - PENDING (azul): Fecha actual o futura que tiene reservas (aprobadas o pendientes)
     * - NONE (sin color): Días sin reservas
     * 
     * @param date Fecha del día a evaluar
     * @param today Fecha actual para comparación
     * @return Estado del día (NONE, PENDING, COMPLETED)
     */
    private fun determineStatus(date: Date, today: Date): ReservaStatus {
        val key = keyFormat.format(date)
        val hasReserva = reservasDesdeApi.any { keyFormat.format(parseDate(it.fecha)) == key }
        
        return if (!date.after(today) && !isSameDay(date, today)) {
            // Días pasados: verde si hay reservas
            if (hasReserva) ReservaStatus.COMPLETED else ReservaStatus.NONE
        } else {
            // Días actuales/futuros: azul si hay reservas (pendientes o aprobadas)
            if (hasReserva) ReservaStatus.PENDING else ReservaStatus.NONE
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

    // ============ FUNCIONES DE API ============
    
    /**
     * Carga todas las reservas desde la API de MongoDB.
     * Actualiza la lista local y refresca el calendario.
     */
    private fun cargarReservasDesdeApi() {
        RetrofitClient.api.obtenerReservas().enqueue(object : Callback<List<ReservaBackend>> {
            override fun onResponse(call: Call<List<ReservaBackend>>, response: Response<List<ReservaBackend>>) {
                if (response.isSuccessful) {
                    reservasDesdeApi.clear()
                    reservasDesdeApi.addAll(response.body() ?: emptyList())
                    adapter.updateDays(generateMonthDays(currentMonthCalendar))
                } else {
                    Toast.makeText(this@ReservarEspacio, "Error al cargar reservas: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ReservaBackend>>, t: Throwable) {
                Toast.makeText(this@ReservarEspacio, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Crea una nueva solicitud de reserva en el backend (estado pendiente).
     */
    private fun crearReservaEnApi(espacio: String, fecha: Date, horaInicio: String, horaFin: String, cantidad: Int) {
        val fechaStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fecha)
        
        // Validar que tenemos información del usuario
        if (currentUser == null || currentUser?.correo.isNullOrBlank() || 
            currentUser?.nombre.isNullOrBlank() || currentUser?.apartamento.isNullOrBlank() || 
            currentUser?.identificacion.isNullOrBlank()) {
            Toast.makeText(this, "Error: Información del usuario incompleta. Por favor inicie sesión nuevamente.", Toast.LENGTH_LONG).show()
            return
        }
        
        val request = CrearReservaRequest(
            zona = espacio,
            fecha = fechaStr,
            horaInicio = horaInicio,
            horaFin = horaFin,
            numeroPersonas = cantidad,
            comentarios = "",
            residenteCorreo = currentUser!!.correo,
            residenteNombre = currentUser!!.nombre,
            residenteApartamento = currentUser!!.apartamento!!,
            residenteIdentificacion = currentUser!!.identificacion!!
        )

        RetrofitClient.api.crearReserva(request).enqueue(object : Callback<CrearReservaResponse> {
            override fun onResponse(call: Call<CrearReservaResponse>, response: Response<CrearReservaResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ReservarEspacio, "✓ Solicitud enviada. Esperando aprobación del administrador", Toast.LENGTH_LONG).show()
                    
                    // Limpiar formulario
                    etFecha.text.clear()
                    etHoraInicio.text.clear()
                    etHoraFinal.text.clear()
                    etCantidad.text.clear()
                    
                    // Recargar reservas y actualizar calendario
                    cargarReservasDesdeApi()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@ReservarEspacio, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CrearReservaResponse>, t: Throwable) {
                Toast.makeText(this@ReservarEspacio, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Actualiza una reserva existente vía API.
     */
    private fun actualizarReservaEnApi(id: String, zona: String, fecha: String, horaInicio: String, horaFin: String, numeroPersonas: Int, comentarios: String) {
        val request = ActualizarReservaRequest(
            zona = zona,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            numeroPersonas = numeroPersonas,
            comentarios = comentarios
        )

        RetrofitClient.api.actualizarReserva(id, request).enqueue(object : Callback<ActualizarReservaResponse> {
            override fun onResponse(call: Call<ActualizarReservaResponse>, response: Response<ActualizarReservaResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ReservarEspacio, "✓ Reserva actualizada", Toast.LENGTH_SHORT).show()
                    cargarReservasDesdeApi()
                } else {
                    Toast.makeText(this@ReservarEspacio, "Error al actualizar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ActualizarReservaResponse>, t: Throwable) {
                Toast.makeText(this@ReservarEspacio, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Elimina (cancela) una reserva vía API.
     */
    private fun eliminarReservaEnApi(id: String, esTardía: Boolean) {
        RetrofitClient.api.eliminarReserva(id).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    val mensaje = if (esTardía) {
                        "⚠️ Reserva cancelada (TARDÍA). Se ha registrado esta cancelación."
                    } else {
                        "✓ Reserva cancelada exitosamente"
                    }
                    Toast.makeText(this@ReservarEspacio, mensaje, Toast.LENGTH_LONG).show()
                    
                    // TODO: Enviar correo electrónico notificando cancelación tardía
                    
                    cargarReservasDesdeApi()
                } else {
                    Toast.makeText(this@ReservarEspacio, "Error al cancelar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ReservarEspacio, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
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
            // Si falla, intentar con formato simple
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
