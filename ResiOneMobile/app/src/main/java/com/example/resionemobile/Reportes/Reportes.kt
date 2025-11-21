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
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
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
        

    }
    
    override fun onResume() {
        super.onResume()
        // Recargar reportes cada vez que se vuelve a la pantalla
        loadReportes()
    }

    /**
     * Carga y muestra los reportes en el contenedor scrolleable.
     * Infla un item_reporte.xml por cada reporte y configura sus listeners.
     */
    private fun loadReportes() {
        reportesContainer.removeAllViews()
        
        val reportes = ReportesManager.obtenerReportes()
        
        if (reportes.isEmpty()) {
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
            tvTitulo.text = "* Reporte ${reporte.numeroSeguimiento}"
            tvDescripcion.text = "${reporte.tipo} - Prioridad: ${reporte.prioridad}"

            // Configurar color de estado
            val colorResId = when (reporte.estado) {
                ReporteEstado.PENDIENTE -> R.drawable.circle_red
                ReporteEstado.ANALISIS -> R.drawable.circle_blue
                ReporteEstado.RESUELTO -> R.drawable.circle_green
            }
            statusIndicator.setBackgroundResource(colorResId)

            // Mostrar/ocultar thumbnail si tiene archivos multimedia
            if (reporte.archivosMultimedia.isNotEmpty()) {
                imgThumbnail.visibility = View.VISIBLE
                
                // Cargar primera imagen/video como thumbnail
                val primerArchivo = reporte.archivosMultimedia.first()
                try {
                    val mimeType = contentResolver.getType(primerArchivo)
                    if (mimeType?.startsWith("image/") == true) {
                        imgThumbnail.setImageURI(primerArchivo)
                    } else if (mimeType?.startsWith("video/") == true) {
                        // Extraer frame del video
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(this, primerArchivo)
                        val bitmap: Bitmap? = mmr.frameAtTime
                        if (bitmap != null) {
                            imgThumbnail.setImageBitmap(bitmap)
                        } else {
                            imgThumbnail.setImageResource(android.R.drawable.ic_media_play)
                        }
                        mmr.release()
                    }
                } catch (e: Exception) {
                    imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                
                imgThumbnail.setOnClickListener {
                    mostrarGaleriaMultimedia(reporte)
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
     * Incluye tipo, estado, descripción, técnico asignado, creador, fecha y opciones para ver multimedia.
     * 
     * Sistema de permisos:
     * - Solo UsuarioAdmin puede asignar técnico (cambia estado a ANALISIS)
     * - Solo UsuarioAdmin puede cambiar estado de ANALISIS a RESUELTO
     * 
     * @param reporte El reporte a mostrar en detalle
     * @param index Índice del reporte en la lista
     */
    private fun showReporteDetail(reporte: ReporteData, index: Int) {
        val estadoText = when (reporte.estado) {
            ReporteEstado.PENDIENTE -> "Pendiente"
            ReporteEstado.ANALISIS -> "En análisis"
            ReporteEstado.RESUELTO -> "Resuelto"
        }
        
        val tecnicoText = reporte.tecnicoAsignado ?: "Ninguno"
        val fechaCreacionText = dateFormat.format(Date(reporte.fechaCreacion))
        val fechaIncidenteText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(reporte.fecha)
        val fechaAsignacionText = if (reporte.fechaAsignacionTecnico != null) {
            dateFormat.format(Date(reporte.fechaAsignacionTecnico))
        } else {
            "No asignado"
        }

        val message = StringBuilder()
        message.append("Número de seguimiento:\n${reporte.numeroSeguimiento}\n\n")
        message.append("Reportado por: ${reporte.creador}\n\n")
        message.append("Tipo de incidencia:\n${reporte.tipo}\n\n")
        message.append("Prioridad: ${reporte.prioridad}\n\n")
        message.append("Estado: $estadoText\n\n")
        message.append("Técnico asignado: $tecnicoText\n\n")
        if (reporte.tecnicoAsignado != null) {
            message.append("Fecha de asignación:\n$fechaAsignacionText\n\n")
        }
        message.append("Fecha del incidente:\n$fechaIncidenteText\n\n")
        message.append("Fecha de creación del reporte:\n$fechaCreacionText\n\n")
        message.append("Descripción:\n${reporte.descripcion}\n\n")
        
        if (reporte.archivosMultimedia.isNotEmpty()) {
            message.append("Archivos adjuntos: ${reporte.archivosMultimedia.size}")
        } else {
            message.append("Sin archivos adjuntos")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Reporte ${index + 1}")
            .setMessage(message.toString())

        // Opciones de archivos multimedia
        if (reporte.archivosMultimedia.isNotEmpty()) {
            builder.setNeutralButton("Ver archivos (${reporte.archivosMultimedia.size})") { _, _ ->
                mostrarGaleriaMultimedia(reporte)
            }
        }
        
        // Opciones de administrador
        if (rolUsuario == "ADMIN") {
            when (reporte.estado) {
                ReporteEstado.PENDIENTE -> {
                    // Admin puede asignar técnico (cambia a ANALISIS)
                    builder.setPositiveButton("Asignar técnico") { _, _ ->
                        mostrarDialogoAsignarTecnico(index)
                    }
                }
                ReporteEstado.ANALISIS -> {
                    // Admin puede marcar como resuelto
                    // TODO: VALIDACIÓN SMS REQUERIDA ANTES DE CAMBIAR ESTADO
                    // TODO: Implementar sistema de validación por SMS con código de 4 letras + 2 números
                    // TODO: Enviar SMS al número registrado del administrador con código de verificación
                    // TODO: Mostrar diálogo para ingresar código de verificación antes de confirmar cambio
                    // TODO: Validar código ingresado contra el código enviado por SMS
                    // TODO: Si código es válido, proceder con cambio de estado
                    // TODO: Si código es inválido, mostrar error y no permitir cambio
                    builder.setPositiveButton("Marcar como resuelto") { _, _ ->
                        ReportesManager.actualizarEstado(index, ReporteEstado.RESUELTO)
                        Toast.makeText(this, "✓ Reporte marcado como resuelto", Toast.LENGTH_SHORT).show()
                        // TODO: Registrar fecha de actualización del estado
                        // TODO: Permitir agregar comentarios/observaciones para el residente
                        // TODO: Enviar notificación por correo electrónico al residente con:
                        // TODO:   - Tipo de incidencia
                        // TODO:   - Descripción del problema
                        // TODO:   - Fecha de creación del reporte
                        // TODO:   - Estado actualizado (RESUELTO)
                        // TODO:   - Observaciones del administrador (si las hay)
                        loadReportes()
                    }
                }
                ReporteEstado.RESUELTO -> {
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
     * Muestra diálogo para asignar un técnico al reporte.
     * Solo accesible por UsuarioAdmin.
     * Al asignar técnico, el estado cambia automáticamente a ANALISIS.
     * 
     * TODO: Reemplazar input de texto libre por selección de técnicos registrados en el sistema
     * TODO: Implementar una de las siguientes opciones:
     *       - ComboBox/Spinner con lista de técnicos disponibles (obtenidos de la base de datos)
     *       - Campo de cédula con validación contra técnicos registrados
     *       - Lista de técnicos con sus nombres, cédulas y especialidades
     * TODO: Validar que el técnico seleccionado esté activo y disponible
     * TODO: Considerar mostrar carga de trabajo actual de cada técnico
     * 
     * TODO: VALIDACIÓN SMS REQUERIDA ANTES DE ASIGNAR TÉCNICO
     * TODO: Implementar sistema de validación por SMS con código de 4 letras + 2 números
     * TODO: Enviar SMS al número registrado del administrador con código de verificación
     * TODO: Mostrar diálogo para ingresar código de verificación antes de asignar técnico
     * TODO: Validar código ingresado contra el código enviado por SMS
     * TODO: Si código es válido, proceder con asignación de técnico y cambio a estado ANALISIS
     * TODO: Si código es inválido, mostrar error y no permitir cambio
     * 
     * @param index Índice del reporte en la lista
     */
    private fun mostrarDialogoAsignarTecnico(index: Int) {
        // NOTA: Este es un input temporal de texto libre
        // Debe ser reemplazado por selección de técnicos desde base de datos
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
                    ReportesManager.asignarTecnico(index, nombreTecnico)
                    ReportesManager.actualizarEstado(index, ReporteEstado.ANALISIS)
                    Toast.makeText(this, "✓ Técnico asignado: $nombreTecnico", Toast.LENGTH_SHORT).show()
                    // TODO: Registrar fecha de actualización del estado
                    // TODO: Permitir agregar comentarios/observaciones para el residente
                    // TODO: Enviar notificación por correo electrónico al residente con:
                    // TODO:   - Tipo de incidencia
                    // TODO:   - Descripción del problema
                    // TODO:   - Fecha de creación del reporte
                    // TODO:   - Estado actualizado (EN ANÁLISIS)
                    // TODO:   - Nombre del técnico asignado
                    // TODO:   - Observaciones del administrador (si las hay)
                    loadReportes()
                } else {
                    Toast.makeText(this, "El nombre del técnico no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
