package com.example.resionemobile.finanzas

import android.R
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.api.Cuota
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.databinding.ActivityRegistroPagoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistroPago : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroPagoBinding
    private var cuotasPendientes = listOf<Cuota>()
    private var selectedCuota: Cuota? = null
    private var pdfUri: Uri? = null

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let {
                pdfUri = it
                binding.tvComprobanteSeleccionado.text = getFileName(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroPagoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDatePicker()
        setupPaymentMethodSelector()
        obtenerCuotasPendientes()

        binding.btnSeleccionarComprobante.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            selectPdfLauncher.launch(intent)
        }

        binding.btnRegistrarPago.setOnClickListener {
            registrarPago()
        }

        binding.spinnerCuotasPendientes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {  // Ignora el item "Seleccione una cuota"
                    selectedCuota = cuotasPendientes[position - 1]
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePicker() {
        binding.etFechaPago.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, {
                _, selectedYear, selectedMonth, selectedDay ->
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                calendar.set(selectedYear, selectedMonth, selectedDay)
                binding.etFechaPago.setText(sdf.format(calendar.time))
            }, year, month, day).show()
        }
    }

    private fun setupPaymentMethodSelector() {
        binding.rgMetodoPago.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbTransferencia.id) {
                binding.btnSeleccionarComprobante.visibility = View.VISIBLE
            } else {
                binding.btnSeleccionarComprobante.visibility = View.GONE
                pdfUri = null
                binding.tvComprobanteSeleccionado.text = ""
            }
        }
    }

    private fun obtenerCuotasPendientes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.obtenerCuotas().execute()
                if (response.isSuccessful) {
                    val todasLasCuotas = response.body() ?: emptyList()
                    cuotasPendientes = todasLasCuotas.filter { it.estado == "Pendiente" || it.estado == "Atrasado" }
                    withContext(Dispatchers.Main) {
                        val displayItems = mutableListOf("Seleccione una cuota")
                        displayItems.addAll(cuotasPendientes.map { "ID: ${it.id} - Monto: ${it.monto} - Vence: ${it.fechaVencimiento}" })
                        val adapter = ArrayAdapter(this@RegistroPago, R.layout.simple_spinner_item, displayItems)
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spinnerCuotasPendientes.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                // TODO: Manejar error
            }
        }
    }

    private fun registrarPago() {
        if (selectedCuota == null) {
            Toast.makeText(this, "Seleccione una cuota", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaPago = binding.etFechaPago.text.toString()
        if (fechaPago.isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha de pago", Toast.LENGTH_SHORT).show()
            return
        }

        val metodoPagoId = binding.rgMetodoPago.checkedRadioButtonId
        if (metodoPagoId == -1) {
            Toast.makeText(this, "Seleccione un método de pago", Toast.LENGTH_SHORT).show()
            return
        }
        val metodoPago = findViewById<RadioButton>(metodoPagoId).text.toString()

        var comprobantePart: MultipartBody.Part? = null
        if (metodoPago == "Transferencia") {
            if (pdfUri == null) {
                Toast.makeText(this, "Debe seleccionar un comprobante en PDF", Toast.LENGTH_SHORT).show()
                return
            }
            pdfUri?.let { uri ->
                val file = File(cacheDir, getFileName(uri))
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                comprobantePart = MultipartBody.Part.createFormData("comprobantePDF", file.name, requestFile)
            }
        }

        val residenteId = "ID_DEL_RESIDENTE_LOGUEADO" // TODO: Reemplazar con dato real
        val nombreResidente = "NOMBRE_DEL_RESIDENTE" // TODO: Reemplazar con dato real

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val response = RetrofitClient.api.registrarPago(
                    cuotaId = selectedCuota!!.id,
                    residenteId = residenteId,
                    nombreResidente = nombreResidente,
                    unidadHabitacional = selectedCuota!!.unidadHabitacional,
                    fechaPago = fechaPago,
                    metodoPago = metodoPago,
                    comprobantePDF = comprobantePart
                ).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistroPago, "Pago registrado exitosamente", Toast.LENGTH_LONG).show()
                        // TODO (Opcional): Emitir factura aquí o en otra pantalla
                        finish()
                    } else {
                        Toast.makeText(this@RegistroPago, "Error al registrar el pago", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistroPago, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (colIndex >= 0) {
                       result = cursor.getString(colIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result ?: "comprobante.pdf"
    }
}
