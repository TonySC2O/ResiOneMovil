package com.example.resionemobile.Reportes

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CrearReporte : BaseActivity() {

    private val attachedUris = mutableListOf<Uri>()

    private var selectedDate: Date? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            attachedUris.clear()
            attachedUris.addAll(uris)
            Toast.makeText(this, "Archivos adjuntados: ${attachedUris.size}", Toast.LENGTH_SHORT).show()
            showThumbnails()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_reporte)

        // Configurar MaterialToolbar como ActionBar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val spinnerTipo = findViewById<Spinner>(R.id.spinner_tipo)
        val spinnerPrioridad = findViewById<Spinner>(R.id.spinner_prioridad)
        val etFechaReporte = findViewById<EditText>(R.id.et_fecha_reporte)
        val btnPickFechaReporte = findViewById<ImageButton>(R.id.btn_pick_fecha_reporte)
        val etDescripcion = findViewById<EditText>(R.id.et_descripcion)
        val btnAdjuntar = findViewById<ImageButton>(R.id.btn_adjuntar)
        val btnRealizar = findViewById<Button>(R.id.btn_realizar)

        // Date picker for incident date
        // Bloquea fechas anteriores a hoy directamente en el calendario
        btnPickFechaReporte.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val minDate = Calendar.getInstance()
            minDate.set(Calendar.HOUR_OF_DAY, 0)
            minDate.set(Calendar.MINUTE, 0)
            minDate.set(Calendar.SECOND, 0)
            minDate.set(Calendar.MILLISECOND, 0)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                
                selectedDate = selectedCalendar.time
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etFechaReporte.setText(dateFormat.format(selectedDate!!))
            }, year, month, day)
            
            // Bloquear fechas anteriores a hoy
            datePickerDialog.datePicker.minDate = minDate.timeInMillis
            datePickerDialog.show()
        }

        btnAdjuntar.setOnClickListener {
            // Allow images and videos: use wildcard and filter by mime type later
            pickMedia.launch("*/*")
        }

        btnRealizar.setOnClickListener {
            val tipo = spinnerTipo.selectedItem?.toString() ?: ""
            val prioridad = spinnerPrioridad.selectedItem?.toString() ?: ""
            val descripcion = etDescripcion.text.toString().trim()
            
            // Validate all mandatory fields
            if (tipo.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona el tipo de incidencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (prioridad.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona el nivel de prioridad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedDate == null) {
                Toast.makeText(this, "Por favor selecciona la fecha del incidente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (descripcion.isEmpty()) {
                Toast.makeText(this, "Por favor describe la incidencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Formatear fecha para enviar al backend
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaFormateada = dateFormat.format(selectedDate!!)

            // Si hay archivos adjuntos, usar Multipart
            if (attachedUris.isNotEmpty()) {
                enviarReporteConArchivos(tipo, descripcion, prioridad, fechaFormateada)
            } else {
                // Sin archivos, usar el endpoint simple
                val request = com.example.resionemobile.api.CrearReporteRequest(
                    tipo = tipo,
                    descripcion = descripcion,
                    nivelPrioridad = prioridad,
                    archivos = emptyList(),
                    fecha = fechaFormateada,
                    residenteCorreo = currentUser?.correo ?: "",
                    residenteNombre = currentUser?.nombre ?: "Usuario",
                    residenteApartamento = currentUser?.apartamento,
                    residenteIdentificacion = currentUser?.identificacion
                )

                com.example.resionemobile.api.RetrofitClient.api.crearReporte(request).enqueue(
                    object : retrofit2.Callback<com.example.resionemobile.api.CrearReporteResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<com.example.resionemobile.api.CrearReporteResponse>,
                            response: retrofit2.Response<com.example.resionemobile.api.CrearReporteResponse>
                        ) {
                            if (response.isSuccessful) {
                                val reporte = response.body()?.reporte
                                val numeroSeguimiento = reporte?.seguimiento ?: "N/A"
                                Toast.makeText(
                                    this@CrearReporte,
                                    "✓ Reporte creado exitosamente\nNúmero de seguimiento: $numeroSeguimiento",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Limpiar campos después de crear
                                etDescripcion.text.clear()
                                etFechaReporte.text.clear()
                                selectedDate = null
                                attachedUris.clear()
                                showThumbnails()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Toast.makeText(
                                    this@CrearReporte,
                                    "Error al crear reporte: ${response.code()}\n${errorBody ?: response.message()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: retrofit2.Call<com.example.resionemobile.api.CrearReporteResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(
                                this@CrearReporte,
                                "Error de conexión: ${t.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            t.printStackTrace()
                        }
                    }
                )
            }

        }
    }

    private fun showThumbnails() {
        val container = findViewById<LinearLayout>(R.id.attachments_container)
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val sizePx = (120 * density).toInt()
        val margin = (8 * density).toInt()

        for (uri in attachedUris) {
            try {
                val mime = contentResolver.getType(uri) ?: ""
                val imageView = ImageView(this)
                val lp = LinearLayout.LayoutParams(sizePx, sizePx)
                lp.setMargins(margin, margin, margin, margin)
                imageView.layoutParams = lp
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                if (mime.startsWith("image")) {
                    imageView.setImageURI(uri)
                } else if (mime.startsWith("video")) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(this, uri)
                    val bitmap: Bitmap? = mmr.frameAtTime
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        // fallback placeholder
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                    mmr.release()
                } else {
                    // unknown type -> show generic icon
                    imageView.setImageResource(android.R.drawable.ic_menu_help)
                }

                // Add to container
                container.addView(imageView)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Envía el reporte con archivos adjuntos mediante Multipart
     */
    private fun enviarReporteConArchivos(tipo: String, descripcion: String, prioridad: String, fecha: String) {
        // Crear partes de texto
        val tipoPart = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
        val descripcionPart = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        val prioridadPart = prioridad.toRequestBody("text/plain".toMediaTypeOrNull())
        val fechaPart = fecha.toRequestBody("text/plain".toMediaTypeOrNull())
        val residenteCorreoPart = (currentUser?.correo ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val residenteNombrePart = (currentUser?.nombre ?: "Usuario").toRequestBody("text/plain".toMediaTypeOrNull())
        val residenteApartamentoPart = (currentUser?.apartamento ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val residenteIdentificacionPart = (currentUser?.identificacion ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

        // Convertir URIs a MultipartBody.Part
        val archivosParts = attachedUris.mapNotNull { uri -> 
            uriToMultipartBody("archivos", uri) 
        }

        // Enviar a la API
        com.example.resionemobile.api.RetrofitClient.api.crearReporteConArchivos(
            tipoPart,
            descripcionPart,
            prioridadPart,
            fechaPart,
            residenteCorreoPart,
            residenteNombrePart,
            residenteApartamentoPart,
            residenteIdentificacionPart,
            archivosParts
        ).enqueue(
            object : retrofit2.Callback<com.example.resionemobile.api.CrearReporteResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.resionemobile.api.CrearReporteResponse>,
                    response: retrofit2.Response<com.example.resionemobile.api.CrearReporteResponse>
                ) {
                    if (response.isSuccessful) {
                        val reporte = response.body()?.reporte
                        val numeroSeguimiento = reporte?.seguimiento ?: "N/A"
                        Toast.makeText(
                            this@CrearReporte,
                            "✓ Reporte creado exitosamente\nNúmero de seguimiento: $numeroSeguimiento\nArchivos adjuntos: ${attachedUris.size}",
                            Toast.LENGTH_LONG
                        ).show()

                        // Limpiar campos después de crear
                        findViewById<EditText>(R.id.et_descripcion).text.clear()
                        findViewById<EditText>(R.id.et_fecha_reporte).text.clear()
                        selectedDate = null
                        attachedUris.clear()
                        showThumbnails()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@CrearReporte,
                            "Error al crear reporte: ${response.code()}\n${errorBody ?: response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.resionemobile.api.CrearReporteResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@CrearReporte,
                        "Error de conexión: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    t.printStackTrace()
                }
            }
        )
    }

    /**
     * Convierte un URI a MultipartBody.Part para subirlo al servidor
     */
    private fun uriToMultipartBody(partName: String, uri: Uri): MultipartBody.Part? {
        return try {
            val filePath = getRealPathFromURI(uri)
            if (filePath == null) {
                Toast.makeText(this, "No se pudo acceder al archivo", Toast.LENGTH_SHORT).show()
                return null
            }
            
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "Archivo no encontrado: ${file.name}", Toast.LENGTH_SHORT).show()
                return null
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, file.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al procesar archivo: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Obtiene la ruta real del archivo desde un URI
     */
    private fun getRealPathFromURI(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }
        
        return path
    }
}
