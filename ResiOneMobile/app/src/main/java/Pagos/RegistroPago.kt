package Pagos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class RegistroPago : AppCompatActivity() {

    private lateinit var cuota: Cuota
    private lateinit var tvCuotaDetails: TextView
    private lateinit var etNombreCompleto: EditText
    private lateinit var etUnidadHabitacional: EditText
    private lateinit var spinnerOpcionPago: Spinner
    private lateinit var btnUploadComprobante: Button
    private lateinit var btnRegistrarPago: Button
    private var comprobanteUri: Uri? = null
    private lateinit var pagosApiService: PagosApiService

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            comprobanteUri = it.data?.data
            Toast.makeText(this, "Comprobante seleccionado: $comprobanteUri", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_pago)

        cuota = intent.getParcelableExtra<Cuota>("cuota")!!

        tvCuotaDetails = findViewById(R.id.tv_cuota_details)
        etNombreCompleto = findViewById(R.id.et_nombre_completo)
        etUnidadHabitacional = findViewById(R.id.et_unidad_habitacional_pago)
        spinnerOpcionPago = findViewById(R.id.spinner_opcion_pago)
        btnUploadComprobante = findViewById(R.id.btn_upload_comprobante)
        btnRegistrarPago = findViewById(R.id.btn_registrar_pago)

        setupViews()
        setupSpinner()
        setupRetrofit()

        btnUploadComprobante.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            filePickerLauncher.launch(intent)
        }

        btnRegistrarPago.setOnClickListener {
            if (validateInput()) {
                createPago()
            }
        }
    }

    private fun setupViews() {
        tvCuotaDetails.text = "Monto a pagar: ${cuota.monto}, Vencimiento: ${cuota.fechaVencimiento}"
        etUnidadHabitacional.setText(cuota.unidadHabitacional)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, OpcionPago.values().map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOpcionPago.adapter = adapter

        spinnerOpcionPago.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOption = OpcionPago.values()[position]
                btnUploadComprobante.visibility = if (selectedOption == OpcionPago.TRANSFERENCIA) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/api/") // Asegúrate de que esta sea la URL correcta de tu API
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        pagosApiService = retrofit.create(PagosApiService::class.java)
    }

    private fun validateInput(): Boolean {
        if (etNombreCompleto.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "El nombre completo es requerido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (spinnerOpcionPago.selectedItem.toString() == OpcionPago.TRANSFERENCIA.name && comprobanteUri == null) {
            Toast.makeText(this, "Debe seleccionar un comprobante en PDF", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createPago() {
        val pago = Pago(
            id = UUID.randomUUID().toString(),
            cuotaId = cuota.id,
            residenteId = cuota.residenteId,
            nombreCompleto = etNombreCompleto.text.toString(),
            unidadHabitacional = etUnidadHabitacional.text.toString(),
            fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            opcionPago = OpcionPago.valueOf(spinnerOpcionPago.selectedItem.toString()),
            comprobanteUrl = if (OpcionPago.valueOf(spinnerOpcionPago.selectedItem.toString()) == OpcionPago.TRANSFERENCIA) comprobanteUri.toString() else null
        )

        pagosApiService.createPago(pago).enqueue(object : Callback<GenericPagoResponse> {
            override fun onResponse(call: Call<GenericPagoResponse>, response: Response<GenericPagoResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegistroPago, "Pago registrado con éxito", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@RegistroPago, "Error al registrar el pago", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericPagoResponse>, t: Throwable) {
                Toast.makeText(this@RegistroPago, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
