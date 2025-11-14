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
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
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

            // Generate tracking number
            val numeroSeguimiento = ReportesManager.generarNumeroSeguimiento()

            // Crear y guardar el reporte en memoria
            val nuevoReporte = ReporteData(
                numeroSeguimiento = numeroSeguimiento,
                tipo = tipo,
                descripcion = descripcion,
                prioridad = prioridad,
                fecha = selectedDate!!,
                archivosMultimedia = attachedUris.toList(),
                creador = currentUser,  // Usuario que crea el reporte
                estado = ReporteEstado.PENDIENTE,
                tecnicoAsignado = null  // Sin técnico al crear
            )
            
            ReportesManager.agregarReporte(nuevoReporte)
            
            Toast.makeText(this, "✓ Reporte creado exitosamente\nNúmero de seguimiento: $numeroSeguimiento", Toast.LENGTH_LONG).show()
            
            // Limpiar campos después de crear
            etDescripcion.text.clear()
            etFechaReporte.text.clear()
            selectedDate = null
            attachedUris.clear()
            showThumbnails()
            
            // Opcional: navegar a la pantalla de Ver Reportes
            // val intent = Intent(this, Reportes::class.java)
            // startActivity(intent)
        }
        
        // ============ CONFIGURACIÓN DEL BOTÓN DE CAMBIO DE USUARIO (SIMULACIÓN) ============
        // Configurar botón de simulación de cambio de usuario heredado de BaseActivity
        // NOTA: Este botón es SOLO para testing y debe ser removido en producción
        setupUserSwitchButton(R.id.btn_switch_user_reporte)
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
