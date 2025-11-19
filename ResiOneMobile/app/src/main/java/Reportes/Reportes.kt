package Reportes

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
import androidx.lifecycle.lifecycleScope
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import api.RetrofitClient
import kotlinx.coroutines.launch
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        // Configurar toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Obtener contenedor de reportes
        reportesContainer = findViewById(R.id.reportes_container)

        // Cargar reportes desde ReportesManager
        loadReportes()
        
        // ============ CONFIGURACIÓN DEL BOTÓN DE CAMBIO DE USUARIO (SIMULACIÓN) ============
        // Configurar botón de simulación de cambio de usuario heredado de BaseActivity
        // Al cambiar usuario, recarga los reportes para actualizar permisos visibles
        // NOTA: Este botón es SOLO para testing y debe ser removido en producción
        setupUserSwitchButton(R.id.btn_switch_user_reportes) {
            loadReportes()  // Recargar reportes cuando cambia el usuario
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar reportes cada vez que se vuelve a la pantalla
        loadReportes()
    }

    /**
     * Carga y muestra los reportes desde el backend.
     * Hace petición GET al servidor y muestra los resultados.
     */
    private fun loadReportes() {
        reportesContainer.removeAllViews()
        
        // Mostrar mensaje de carga
        val loadingView = TextView(this).apply {
            text = "Cargando reportes..."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 64, 32, 32)
            setTextColor(android.graphics.Color.GRAY)
        }
        reportesContainer.addView(loadingView)
        
        // Cargar reportes desde el backend
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reportesApi.obtenerReportes()
                
                if (response.isSuccessful && response.body() != null) {
                    val reportes = response.body()!!
                    
                    // Limpiar vista de carga
                    reportesContainer.removeAllViews()
                    
                    if (reportes.isEmpty()) {
                        // Mostrar mensaje si no hay reportes
                        val emptyView = TextView(this@Reportes).apply {
                            text = "No hay reportes creados.\n\nCrea un reporte desde el menú 'Crear Reportes'"
                            textSize = 16f
                            gravity = android.view.Gravity.CENTER
                            setPadding(32, 64, 32, 32)
                            setTextColor(android.graphics.Color.GRAY)
                        }
                        reportesContainer.addView(emptyView)
                        return@launch
                    }
                    
                    mostrarReportes(reportes)
                    
                } else {
                    reportesContainer.removeAllViews()
                    val errorView = TextView(this@Reportes).apply {
                        text = "Error al cargar reportes: ${response.message()}"
                        textSize = 16f
                        gravity = android.view.Gravity.CENTER
                        setPadding(32, 64, 32, 32)
                        setTextColor(android.graphics.Color.RED)
                    }
                    reportesContainer.addView(errorView)
                }
                
            } catch (e: Exception) {
                reportesContainer.removeAllViews()
                val errorView = TextView(this@Reportes).apply {
                    text = "Error de conexión: ${e.message}\n\nVerifica que el servidor esté corriendo"
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 64, 32, 32)
                    setTextColor(android.graphics.Color.RED)
                }
                reportesContainer.addView(errorView)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Muestra la lista de reportes en la interfaz
     */
    private fun mostrarReportes(reportes: List<api.ReporteBackend>) {
        reportes.forEachIndexed { index, reporte ->
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

            // Configurar color de estado (mapear strings del backend a estados)
            val colorResId = when (reporte.estado.uppercase()) {
                "PENDIENTE" -> R.drawable.circle_red
                "ANALISIS", "EN ANÁLISIS" -> R.drawable.circle_blue
                "RESUELTO", "COMPLETADO" -> R.drawable.circle_green
                else -> R.drawable.circle_red
            }
            statusIndicator.setBackgroundResource(colorResId)

            // Mostrar thumbnail si hay archivos
            if (reporte.archivos.isNotEmpty()) {
                imgThumbnail.visibility = View.VISIBLE
                
                // Cargar primera imagen desde el servidor
                val primerArchivo = reporte.archivos.first()
                val imageUrl = "http://10.0.2.2:5050${primerArchivo}"  // URL completa del servidor
                
                // Usar Glide o cargar manualmente - por ahora mostrar ícono
                imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                
                // TODO: Implementar librería de carga de imágenes como Glide o Coil
                // Glide.with(this).load(imageUrl).into(imgThumbnail)
                
                imgThumbnail.setOnClickListener {
                    Toast.makeText(this@Reportes, "Archivos: ${reporte.archivos.size}", Toast.LENGTH_SHORT).show()
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
                showReporteDetailBackend(reporte, index)
            }

            reportesContainer.addView(itemView)
        }
    }
    
    /**
     * Muestra diálogo con información completa del reporte desde el backend.
     * Incluye opciones de administrador según el estado del reporte.
     */
    private fun showReporteDetailBackend(reporte: api.ReporteBackend, index: Int) {
        val message = StringBuilder()
        message.append("Número de seguimiento:\n${reporte.seguimiento}\n\n")
        message.append("Tipo de incidencia:\n${reporte.tipo}\n\n")
        message.append("Prioridad: ${reporte.nivelPrioridad}\n\n")
        message.append("Estado: ${reporte.estado}\n\n")
        message.append("Fecha: ${reporte.fecha}\n\n")
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
            .setTitle("Detalle del Reporte")
            .setMessage(message.toString())

        // Opciones de administrador según el estado
        if (currentUser == "UsuarioAdmin") {
            when (reporte.estado.uppercase()) {
                "PENDIENTE" -> {
                    // Admin puede asignar técnico (cambia a ANALISIS)
                    builder.setPositiveButton("Asignar técnico") { _, _ ->
                        mostrarDialogoAsignarTecnicoBackend(reporte)
                    }
                }
                "ANALISIS", "EN ANÁLISIS" -> {
                    // Admin puede marcar como resuelto
                    builder.setPositiveButton("Marcar como resuelto") { _, _ ->
                        mostrarDialogoMarcarResuelto(reporte)
                    }
                }
                "RESUELTO", "COMPLETADO" -> {
                    // Reporte ya está resuelto, solo cerrar
                    builder.setPositiveButton("Cerrar", null)
                }
                else -> {
                    builder.setPositiveButton("Cerrar", null)
                }
            }
        } else {
            builder.setPositiveButton("Cerrar", null)
        }

        builder.show()
    }

    
    /**
     * Muestra un diálogo con la galería de archivos multimedia del reporte.
     * Al seleccionar un archivo, abre el visualizador o reproductor correspondiente.
     * 
     * @param reporte El reporte del cual mostrar los archivos
     */
    private fun mostrarGaleriaMultimedia(reporte: ReporteData) {
        if (reporte.archivosMultimedia.isEmpty()) {
            Toast.makeText(this, "No hay archivos adjuntos", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = reporte.archivosMultimedia.mapIndexed { index, uri ->
            val mimeType = contentResolver.getType(uri)
            val tipo = when {
                mimeType?.startsWith("image/") == true -> "Imagen"
                mimeType?.startsWith("video/") == true -> "Video"
                else -> "Archivo"
            }
            "$tipo ${index + 1}"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Archivos adjuntos (${items.size})")
            .setItems(items) { _, which ->
                val uri = reporte.archivosMultimedia[which]
                val mimeType = contentResolver.getType(uri)
                
                when {
                    mimeType?.startsWith("image/") == true -> {
                        // Abrir imagen en visualizador de pantalla completa
                        abrirVisualizadorImagen(uri)
                    }
                    mimeType?.startsWith("video/") == true -> {
                        // Abrir video en reproductor
                        abrirReproductorVideo(uri)
                    }
                    else -> {
                        Toast.makeText(this, "Tipo de archivo no soportado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * Muestra diálogo para asignar técnico usando el backend
     */
    private fun mostrarDialogoAsignarTecnicoBackend(reporte: api.ReporteBackend) {
        val inputEditText = EditText(this).apply {
            hint = "Nombre del técnico"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Asignar técnico")
            .setMessage("Ingresa el nombre del técnico a asignar.\nEl estado cambiará a 'En análisis'.")
            .setView(inputEditText)
            .setPositiveButton("Asignar") { _, _ ->
                val nombreTecnico = inputEditText.text.toString().trim()
                if (nombreTecnico.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val request = CambiarEstadoRequest(
                                nuevoEstado = "Analisis",
                                comentarios = "Técnico asignado: $nombreTecnico",
                                codigo = "ADMIN123",  // TODO: Implementar validación SMS real
                                codigoValido = true,
                                correoResidente = "residente@resione.com"  // TODO: Obtener correo real
                            )
                            
                            val response = RetrofitClient.reportesApi.cambiarEstado(reporte.id, request)
                            
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@Reportes, 
                                    "✓ Técnico asignado: $nombreTecnico", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadReportes()
                            } else {
                                Toast.makeText(
                                    this@Reportes,
                                    "Error al asignar técnico: ${response.message()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@Reportes,
                                "Error de conexión: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "El nombre del técnico no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Muestra diálogo para marcar reporte como resuelto usando el backend
     */
    private fun mostrarDialogoMarcarResuelto(reporte: api.ReporteBackend) {
        val inputEditText = EditText(this).apply {
            hint = "Comentarios/observaciones (opcional)"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Marcar como resuelto")
            .setMessage("¿Confirmas que este reporte ha sido resuelto?\n\nPuedes agregar comentarios adicionales:")
            .setView(inputEditText)
            .setPositiveButton("Confirmar") { _, _ ->
                val comentarios = inputEditText.text.toString().trim()
                
                lifecycleScope.launch {
                    try {
                        val request = CambiarEstadoRequest(
                            nuevoEstado = "Resuelto",
                            comentarios = comentarios.ifEmpty { "Reporte marcado como resuelto" },
                            codigo = "ADMIN123",  // TODO: Implementar validación SMS real
                            codigoValido = true,
                            correoResidente = "residente@resione.com"  // TODO: Obtener correo real
                        )
                        
                        val response = RetrofitClient.reportesApi.cambiarEstado(reporte.id, request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@Reportes, 
                                "✓ Reporte marcado como resuelto", 
                                Toast.LENGTH_SHORT
                            ).show()
                            loadReportes()
                        } else {
                            Toast.makeText(
                                this@Reportes,
                                "Error al actualizar estado: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@Reportes,
                            "Error de conexión: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Abre un diálogo con la imagen en pantalla completa.
     * Permite visualizar la imagen con zoom y desplazamiento.
     * 
     * @param imageUri URI de la imagen a mostrar
     */
    private fun abrirVisualizadorImagen(imageUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_viewer, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.fullscreen_image)
        
        try {
            imageView.setImageURI(imageUri)
            
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * Abre el reproductor de video nativo del sistema para reproducir el video.
     * Utiliza un Intent con ACTION_VIEW para abrir la aplicación de video predeterminada.
     * 
     * @param videoUri URI del video a reproducir
     */
    private fun abrirReproductorVideo(videoUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se encontró una aplicación para reproducir videos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el video", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}

/**
 * Enum que representa los posibles estados de un reporte.
 */
enum class ReporteEstado {
    PENDIENTE,  // Rojo - Reporte nuevo sin revisar
    ANALISIS,   // Azul - Reporte en proceso de revisión
    RESUELTO    // Verde - Reporte completado
}
