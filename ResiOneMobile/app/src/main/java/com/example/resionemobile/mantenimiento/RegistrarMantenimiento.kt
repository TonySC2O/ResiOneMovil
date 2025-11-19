package com.example.resionemobile.mantenimiento

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.R
import com.example.resionemobile.api.MantenimientoResponse
import com.example.resionemobile.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RegistrarMantenimiento : AppCompatActivity() {

    private lateinit var spinnerIncidencia: Spinner
    private lateinit var spinnerTipoMantenimiento: Spinner
    private lateinit var etResponsable: EditText
    private lateinit var etFechaEjecucion: EditText
    private lateinit var btnPickFecha: ImageButton
    private lateinit var etDescripcion: EditText
    private lateinit var etObservaciones: EditText
    private lateinit var btnAdjuntarAntes: ImageButton
    private lateinit var btnAdjuntarDespues: ImageButton
    private lateinit var fotosAntesContainer: LinearLayout
    private lateinit var fotosDespuesContainer: LinearLayout
    private lateinit var btnRegistrar: Button

    private val fotosAntesUris = mutableListOf<Uri>()
    private val fotosDespuesUris = mutableListOf<Uri>()
    private var selectedContainer: LinearLayout? = null

    private val calendar: Calendar = Calendar.getInstance()

    companion object {
        private const val PICK_IMAGE_REQUEST_BEFORE = 100
        private const val PICK_IMAGE_REQUEST_AFTER = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_mante)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        spinnerIncidencia = findViewById(R.id.spinner_incidencia)
        spinnerTipoMantenimiento = findViewById(R.id.spinner_tipo_mantenimiento)
        etResponsable = findViewById(R.id.et_responsable)
        etFechaEjecucion = findViewById(R.id.et_fecha_ejecucion)
        btnPickFecha = findViewById(R.id.btn_pick_fecha_ejecucion)
        etDescripcion = findViewById(R.id.et_descripcion_mantenimiento)
        etObservaciones = findViewById(R.id.et_observaciones)
        btnAdjuntarAntes = findViewById(R.id.btn_adjuntar_antes)
        btnAdjuntarDespues = findViewById(R.id.btn_adjuntar_despues)
        fotosAntesContainer = findViewById(R.id.fotos_antes_container)
        fotosDespuesContainer = findViewById(R.id.fotos_despues_container)
        btnRegistrar = findViewById(R.id.btn_registrar_mantenimiento)

        // TODO: Cargar incidencias reales desde la API
    }

    private fun setupListeners() {
        btnPickFecha.setOnClickListener { showDatePickerDialog() }
        btnAdjuntarAntes.setOnClickListener {
            selectedContainer = fotosAntesContainer
            openGallery(PICK_IMAGE_REQUEST_BEFORE)
        }
        btnAdjuntarDespues.setOnClickListener {
            selectedContainer = fotosDespuesContainer
            openGallery(PICK_IMAGE_REQUEST_AFTER)
        }
        btnRegistrar.setOnClickListener { if (validarCampos()) registrarMantenimiento() }
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateInView() {
        val format = "dd-MM-yyyy"
        val sdf = SimpleDateFormat(format, Locale.US)
        etFechaEjecucion.setText(sdf.format(calendar.time))
    }

    private fun openGallery(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val imageUris = if (data?.clipData != null) {
                (0 until data.clipData!!.itemCount).map { data.clipData!!.getItemAt(it).uri }
            } else if (data?.data != null) {
                listOf(data.data!!)
            } else {
                emptyList()
            }

            if (requestCode == PICK_IMAGE_REQUEST_BEFORE) {
                fotosAntesUris.addAll(imageUris)
                addImagesToContainer(fotosAntesContainer, imageUris)
            } else if (requestCode == PICK_IMAGE_REQUEST_AFTER) {
                fotosDespuesUris.addAll(imageUris)
                addImagesToContainer(fotosDespuesContainer, imageUris)
            }
        }
    }

    private fun addImagesToContainer(container: LinearLayout, uris: List<Uri>) {
        uris.forEach { uri ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { marginEnd = 8 }
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            container.addView(imageView)
        }
    }

    private fun validarCampos(): Boolean {
        // Validaciones básicas (se pueden mejorar)
        if (etResponsable.text.isBlank()) {
            etResponsable.error = "El responsable es requerido"
            return false
        }
        if (etFechaEjecucion.text.isBlank()) {
            etFechaEjecucion.error = "La fecha es requerida"
            return false
        }
        if (etDescripcion.text.isBlank()) {
            etDescripcion.error = "La descripción es requerida"
            return false
        }
        // TODO: Validar que la fecha no sea anterior a la actual.
        return true
    }

    private fun registrarMantenimiento() {
        val partMap = mutableMapOf<String, RequestBody>(
            "incidenciaAsociada" to (spinnerIncidencia.selectedItem?.toString() ?: "N/A").toRequestBody("text/plain".toMediaTypeOrNull()),
            "tipoMantenimiento" to spinnerTipoMantenimiento.selectedItem.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "descripcion" to etDescripcion.text.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "responsable" to etResponsable.text.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "fechaEjecucion" to etFechaEjecucion.text.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            "observaciones" to etObservaciones.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        )

        val fotosAntesParts = fotosAntesUris.map { uri -> uriToMultipartBody("fotosAntes", uri) }
        val fotosDespuesParts = fotosDespuesUris.map { uri -> uriToMultipartBody("fotosDespues", uri) }

        RetrofitClient.api.registrarMantenimiento(partMap, fotosAntesParts, fotosDespuesParts)
            .enqueue(object : Callback<MantenimientoResponse> {
                override fun onResponse(call: Call<MantenimientoResponse>, response: Response<MantenimientoResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistrarMantenimiento, "Mantenimiento registrado con éxito", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegistrarMantenimiento, "Error: ${response.code()} - ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MantenimientoResponse>, t: Throwable) {
                    Toast.makeText(this@RegistrarMantenimiento, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun uriToMultipartBody(partName: String, uri: Uri): MultipartBody.Part {
        val filePath = getRealPathFromURI(uri)
        val file = File(filePath)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var path: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        if (cursor?.moveToFirst() == true) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            path = cursor.getString(columnIndex)
        }
        cursor?.close()
        return path
    }
}
