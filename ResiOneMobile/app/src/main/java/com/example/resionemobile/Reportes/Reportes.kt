package com.example.resionemobile.Reportes

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import coil.load
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.api.ReporteBackend
import com.example.resionemobile.api.CambiarEstadoRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity para visualizar la lista de reportes creados.
 * 
 * Funcionalidades:
 * - Muestra lista scrolleable de reportes con diferentes estados
 * - Expandir/colapsar reportes para ver detalles
 * - Ver información completa del reporte en diálogo
 * - Visualizar imágenes adjuntas al reporte
 * - Leyenda de colores para identificar estados
 * 
 * Estados de reportes:
 * - Pendiente (Rojo): Reportes nuevos sin revisar
 * - Análisis (Azul): Reportes en proceso de revisión
 * - Resuelto (Verde): Reportes completados
 * 
 * TODO: Integrar con sistema de almacenamiento (MongoDB) para cargar reportes reales
 * TODO: Implementar sincronización con CrearReporte para mostrar reportes creados
 */
class Reportes : BaseActivity() {

    private lateinit var reportesContainer: LinearLayout
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var reportesFromApi = listOf<ReporteBackend>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        // Configurar toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Obtener contenedor de reportes
        reportesContainer = findViewById(R.id.reportes_container)
    }
    
    override fun onResume() {
        super.onResume()
        // Cargar reportes desde la API cada vez que se vuelve a la pantalla
        cargarReportesDesdeApi()
    }

    /**
     * Carga los reportes desde la API de MongoDB.
     * Los administradores ven todos los reportes.
     * Los residentes solo ven sus propios reportes.
     */
    private fun cargarReportesDesdeApi() {
        RetrofitClient.api.obtenerReportes().enqueue(
            object : retrofit2.Callback<com.example.resionemobile.api.ReportesListResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.resionemobile.api.ReportesListResponse>,
                    response: retrofit2.Response<com.example.resionemobile.api.ReportesListResponse>
                ) {
                    if (response.isSuccessful) {
                        val todosLosReportes = response.body()?.reportes ?: emptyList()
                        
                        // Todos los usuarios ven todos los reportes
                        reportesFromApi = todosLosReportes
                        
                        loadReportes()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@Reportes,
                            "Error al cargar reportes: ${response.code()}\n${errorBody ?: response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                        // Mostrar interfaz vacía
                        reportesFromApi = emptyList()
                        loadReportes()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.resionemobile.api.ReportesListResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@Reportes,
                        "Error de conexión: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    t.printStackTrace()
                    // Mostrar interfaz vacía
                    reportesFromApi = emptyList()
                    loadReportes()
                }
            }
        )
    }

    /**
     * Carga y muestra los reportes en el contenedor scrolleable.
     * Infla un item_reporte.xml por cada reporte y configura sus listeners.
     */
    private fun loadReportes() {
        reportesContainer.removeAllViews()
        
        if (reportesFromApi.isEmpty()) {
            // Mostrar mensaje si no hay reportes
            val emptyView = TextView(this).apply {
                text = "No hay reportes creados.\n\nCrea un reporte desde el menú 'Crear Reportes'"
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(32, 64, 32, 32)
                setTextColor(android.graphics.Color.GRAY)
            }
            reportesContainer.addView(emptyView)
            return
        }

        reportesFromApi.forEachIndexed { index, reporte ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_reporte, reportesContainer, false)

            // Configurar vistas del item
            val statusIndicator = itemView.findViewById<View>(R.id.status_indicator)
            val tvTitulo = itemView.findViewById<TextView>(R.id.tv_reporte_titulo)
            val tvDescripcion = itemView.findViewById<TextView>(R.id.tv_reporte_descripcion)
            val imgThumbnail = itemView.findViewById<ImageView>(R.id.img_reporte_thumbnail)
            val btnExpand = itemView.findViewById<ImageButton>(R.id.btn_expand)
            val expandedContent = itemView.findViewById<LinearLayout>(R.id.expanded_content)

            // Configurar datos - usar número de seguimiento como título
            tvTitulo.text = "* Reporte ${reporte.seguimiento}"
            tvDescripcion.text = "${reporte.tipo} - Prioridad: ${reporte.nivelPrioridad}"

            // Configurar color de estado
            val colorResId = when (reporte.estado) {
                "Pendiente" -> R.drawable.circle_red
                "En análisis" -> R.drawable.circle_blue
                "Resuelto" -> R.drawable.circle_green
                else -> R.drawable.circle_red
            }
            statusIndicator.setBackgroundResource(colorResId)

            // Mostrar thumbnail si tiene archivos
            if (reporte.archivos.isNotEmpty()) {
                imgThumbnail.visibility = View.VISIBLE
                val primerArchivo = reporte.archivos.first()
                val urlCompleta = "http://10.0.2.2:5050/$primerArchivo"
                
                // Cargar imagen desde servidor usando Coil
                imgThumbnail.load(urlCompleta) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
                
                imgThumbnail.setOnClickListener {
                    mostrarArchivosDialog(reporte)
                }
            } else {
                imgThumbnail.visibility = View.GONE
            }

            // Configurar botón expandir/colapsar
            var isExpanded = false
            btnExpand.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    expandedContent.visibility = View.VISIBLE
                    btnExpand.setImageResource(android.R.drawable.arrow_up_float)
                } else {
                    expandedContent.visibility = View.GONE
                    btnExpand.setImageResource(android.R.drawable.arrow_down_float)
                }
            }

            // Click en el card completo para ver detalles
            itemView.setOnClickListener {
                showReporteDetail(reporte, index)
            }

            reportesContainer.addView(itemView)
        }
    }

    /**
     * Muestra un diálogo con los detalles completos del reporte.
     * Incluye tipo, estado, descripción, fecha y comentarios del admin.
     * 
     * Sistema de permisos:
     * - Solo UsuarioAdmin puede cambiar el estado del reporte
     * 
     * @param reporte El reporte a mostrar en detalle
     * @param index Índice del reporte en la lista
     */
    private fun showReporteDetail(reporte: ReporteBackend, index: Int) {
        val message = StringBuilder()
        message.append("Número de seguimiento:\n${reporte.seguimiento}\n\n")
        message.append("Reportado por:\n${reporte.residenteNombre}\n")
        if (!reporte.residenteApartamento.isNullOrEmpty()) {
            message.append("Apartamento: ${reporte.residenteApartamento}\n")
        }
        message.append("\n")
        message.append("Tipo de incidencia:\n${reporte.tipo}\n\n")
        message.append("Prioridad: ${reporte.nivelPrioridad}\n\n")
        message.append("Estado: ${reporte.estado}\n\n")
        message.append("Técnico asignado: ${reporte.tecnicoAsignado}\n\n")
        message.append("Fecha del incidente:\n${reporte.fecha}\n\n")
        message.append("Descripción:\n${reporte.descripcion}\n\n")
        
        if (reporte.comentariosAdmin.isNotEmpty()) {
            message.append("Comentarios del administrador:\n${reporte.comentariosAdmin}\n\n")
        }
        
        if (reporte.archivos.isNotEmpty()) {
            message.append("Archivos adjuntos: ${reporte.archivos.size}")
        } else {
            message.append("Sin archivos adjuntos")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Reporte ${index + 1}")
            .setMessage(message.toString())
        
        // Opciones de administrador
        if (esAdministrador) {
            when (reporte.estado) {
                "Pendiente" -> {
                    // Admin puede cambiar a En análisis
                    builder.setPositiveButton("Marcar en análisis") { _, _ ->
                        mostrarDialogoCambiarEstado(reporte, "En análisis")
                    }
                }
                "En análisis" -> {
                    // Admin puede marcar como resuelto
                    builder.setPositiveButton("Marcar como resuelto") { _, _ ->
                        mostrarDialogoCambiarEstado(reporte, "Resuelto")
                    }
                }
                "Resuelto" -> {
                    // Reporte ya está resuelto, solo cerrar
                    builder.setPositiveButton("Cerrar", null)
                }
            }
        } else {
            builder.setPositiveButton("Cerrar", null)
        }

        builder.show()
    }
    
    /**
     * Muestra diálogo para cambiar el estado del reporte.
     * Solo accesible por UsuarioAdmin.
     * Si el estado es "En análisis", solicita la identificación del técnico.
     * 
     * @param reporte El reporte a actualizar
     * @param nuevoEstado El nuevo estado del reporte
     */
    private fun mostrarDialogoCambiarEstado(reporte: ReporteBackend, nuevoEstado: String) {
        // Si el estado es "En análisis", mostrar diálogo de asignación de técnico
        if (nuevoEstado == "En análisis") {
            mostrarDialogoAsignarTecnico(reporte, nuevoEstado)
            return
        }
        
        // Para otros estados, solo pedir comentarios
        val inputEditText = EditText(this).apply {
            hint = "Comentarios (opcional)"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Cambiar estado a: $nuevoEstado")
            .setMessage("Puedes agregar comentarios sobre este cambio de estado.")
            .setView(inputEditText)
            .setPositiveButton("Confirmar") { _, _ ->
                val comentarios = inputEditText.text.toString().trim()
                
                val request = CambiarEstadoRequest(
                    estado = nuevoEstado,
                    comentariosAdmin = comentarios,
                    identificacionTecnico = null
                )
                
                RetrofitClient.api.cambiarEstadoReporte(reporte.id, request).enqueue(
                    object : retrofit2.Callback<com.example.resionemobile.api.GenericResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<com.example.resionemobile.api.GenericResponse>,
                            response: retrofit2.Response<com.example.resionemobile.api.GenericResponse>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@Reportes,
                                    "✓ Estado actualizado a: $nuevoEstado",
                                    Toast.LENGTH_SHORT
                                ).show()
                                cargarReportesDesdeApi()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Toast.makeText(
                                    this@Reportes,
                                    "Error: ${errorBody ?: response.message()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: retrofit2.Call<com.example.resionemobile.api.GenericResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(
                                this@Reportes,
                                "Error de conexión: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra diálogo para asignar un técnico de mantenimiento al reporte.
     * Solicita la identificación del técnico y valida que exista y tenga el rol correcto.
     * 
     * @param reporte El reporte a actualizar
     * @param nuevoEstado El nuevo estado del reporte ("En análisis")
     */
    private fun mostrarDialogoAsignarTecnico(reporte: ReporteBackend, nuevoEstado: String) {
        val dialogView = LayoutInflater.from(this).inflate(
            android.R.layout.simple_list_item_2, null
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        
        val etIdentificacion = EditText(this).apply {
            hint = "Identificación del técnico"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(20, 20, 20, 20)
        }
        
        val etComentarios = EditText(this).apply {
            hint = "Comentarios (opcional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(20, 20, 20, 20)
            minLines = 2
        }
        
        container.addView(etIdentificacion)
        container.addView(etComentarios)
        
        AlertDialog.Builder(this)
            .setTitle("Asignar técnico de mantenimiento")
            .setMessage("Ingrese la identificación del técnico que atenderá este reporte.\n\nEl sistema validará que la identificación exista y que el usuario tenga el rol de TECNICO_MANTENIMIENTO.")
            .setView(container)
            .setPositiveButton("Asignar") { _, _ ->
                val identificacion = etIdentificacion.text.toString().trim()
                val comentarios = etComentarios.text.toString().trim()
                
                if (identificacion.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Debe ingresar la identificación del técnico",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                
                // Crear request con identificación del técnico
                val request = CambiarEstadoRequest(
                    estado = nuevoEstado,
                    comentariosAdmin = comentarios,
                    identificacionTecnico = identificacion
                )
                
                // Enviar petición al backend
                RetrofitClient.api.cambiarEstadoReporte(reporte.id, request).enqueue(
                    object : retrofit2.Callback<com.example.resionemobile.api.GenericResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<com.example.resionemobile.api.GenericResponse>,
                            response: retrofit2.Response<com.example.resionemobile.api.GenericResponse>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@Reportes,
                                    "✓ Técnico asignado exitosamente\nReporte en análisis",
                                    Toast.LENGTH_LONG
                                ).show()
                                cargarReportesDesdeApi()
                            } else {
                                // Mostrar error específico del backend
                                val errorBody = response.errorBody()?.string()
                                val errorMessage = try {
                                    val jsonError = Gson().fromJson(errorBody, JsonObject::class.java)
                                    jsonError.get("mensaje")?.asString ?: "Error desconocido"
                                } catch (e: Exception) {
                                    errorBody ?: "Error al procesar la respuesta"
                                }
                                
                                Toast.makeText(
                                    this@Reportes,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: retrofit2.Call<com.example.resionemobile.api.GenericResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(
                                this@Reportes,
                                "Error de conexión: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo con la lista de archivos adjuntos al reporte
     */
    private fun mostrarArchivosDialog(reporte: ReporteBackend) {
        if (reporte.archivos.isEmpty()) {
            Toast.makeText(this, "No hay archivos adjuntos", Toast.LENGTH_SHORT).show()
            return
        }

        val archivosNombres = reporte.archivos.mapIndexed { index, ruta ->
            val nombreArchivo = ruta.substringAfterLast("/")
            val extension = nombreArchivo.substringAfterLast(".", "")
            val tipo = when {
                extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> "🖼️ Imagen"
                extension.lowercase() in listOf("mp4", "avi", "mov", "mkv", "webm") -> "🎥 Video"
                else -> "📄 Archivo"
            }
            "$tipo ${index + 1}: $nombreArchivo"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Archivos adjuntos (${reporte.archivos.size})")
            .setItems(archivosNombres) { _, which ->
                val rutaArchivo = reporte.archivos[which]
                val nombreArchivo = rutaArchivo.substringAfterLast("/")
                val extension = nombreArchivo.substringAfterLast(".", "").lowercase()
                
                when {
                    extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> {
                        // Es una imagen, mostrar en visor de pantalla completa
                        mostrarImagenPantallaCompleta(rutaArchivo)
                    }
                    extension in listOf("mp4", "avi", "mov", "mkv", "webm") -> {
                        // Es un video, abrir en reproductor
                        reproducirVideo(rutaArchivo)
                    }
                    else -> {
                        Toast.makeText(
                            this,
                            "Tipo de archivo no soportado para visualización",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /**
     * Muestra una imagen en pantalla completa desde el servidor
     */
    private fun mostrarImagenPantallaCompleta(rutaArchivo: String) {
        val urlCompleta = "http://10.0.2.2:5050/$rutaArchivo"
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_viewer, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.fullscreen_image)
        
        // Cargar imagen desde servidor usando Coil
        imageView.load(urlCompleta) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_dialog_alert)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()
        
        dialog.show()
        
        // Hacer la imagen responsiva al tamaño del diálogo
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    /**
     * Reproduce un video desde el servidor usando el reproductor del sistema
     */
    private fun reproducirVideo(rutaArchivo: String) {
        try {
            val urlCompleta = "http://10.0.2.2:5050/$rutaArchivo"
            val videoUri = Uri.parse(urlCompleta)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No se encontró una aplicación para reproducir videos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al abrir el video: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }
}
