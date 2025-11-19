package Reportes

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import api.CrearReporteRequest
import api.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
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

            // Crear reporte en el backend usando API
            crearReporteEnBackend(tipo, descripcion, prioridad)
        }
        
        // ============ CONFIGURACIÓN DEL BOTÓN DE CAMBIO DE USUARIO (SIMULACIÓN) ============
        // Configurar botón de simulación de cambio de usuario heredado de BaseActivity
        // NOTA: Este botón es SOLO para testing y debe ser removido en producción
        setupUserSwitchButton(R.id.btn_switch_user_reporte)
    }
    
    /**
     * Envía el reporte al backend usando Retrofit con archivos adjuntos
     */
    private fun crearReporteEnBackend(tipo: String, descripcion: String, prioridad: String) {
        lifecycleScope.launch {
            try {
                val response = if (attachedUris.isNotEmpty()) {
                    // Crear reporte con archivos adjuntos
                    crearReporteConArchivos(tipo, descripcion, prioridad)
                } else {
                    // Crear reporte sin archivos (JSON simple)
                    val request = CrearReporteRequest(
                        tipo = tipo,
                        descripcion = descripcion,
                        nivelPrioridad = prioridad,
                        archivos = emptyList(),
                        correoResidente = "$currentUser@resione.com"
                    )
                    RetrofitClient.reportesApi.crearReporte(request)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val resultado = response.body()!!
                    val numeroSeguimiento = resultado.reporte.seguimiento
                    
                    // Mostrar éxito
                    Toast.makeText(
                        this@CrearReporte,
                        "✓ Reporte creado exitosamente\nNúmero de seguimiento: $numeroSeguimiento",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Limpiar campos
                    findViewById<EditText>(R.id.et_descripcion).text.clear()
                    findViewById<EditText>(R.id.et_fecha_reporte).text.clear()
                    selectedDate = null
                    attachedUris.clear()
                    showThumbnails()
                    
                } else {
                    Toast.makeText(
                        this@CrearReporte,
                        "Error al crear reporte: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@CrearReporte,
                    "Error de conexión: ${e.message}\nVerifica que el servidor esté corriendo",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Crea un reporte con archivos adjuntos usando multipart
     */
    private suspend fun crearReporteConArchivos(tipo: String, descripcion: String, prioridad: String): retrofit2.Response<api.CrearReporteResponse> {
        // Preparar campos de texto como RequestBody
        val tipoBody = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
        val descripcionBody = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        val prioridadBody = prioridad.toRequestBody("text/plain".toMediaTypeOrNull())
        val correoBody = "$currentUser@resione.com".toRequestBody("text/plain".toMediaTypeOrNull())
        
        // Preparar archivos como MultipartBody.Part
        val archivosParts = mutableListOf<MultipartBody.Part>()
        
        for (uri in attachedUris) {
            try {
                // Copiar archivo de URI a archivo temporal
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(cacheDir, "upload_${System.currentTimeMillis()}.tmp")
                    val outputStream = FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    // Determinar tipo MIME
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    
                    // Crear RequestBody y MultipartBody.Part
                    val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("archivos", file.name, requestBody)
                    archivosParts.add(part)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Hacer petición con archivos
        return RetrofitClient.reportesApi.crearReporteConArchivos(
            tipoBody,
            descripcionBody,
            prioridadBody,
            correoBody,
            archivosParts
        )
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
}
