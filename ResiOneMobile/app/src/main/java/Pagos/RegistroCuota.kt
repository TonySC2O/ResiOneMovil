package Pagos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class RegistroCuota : AppCompatActivity() {

    private lateinit var etMonto: EditText
    private lateinit var etFechaVencimiento: EditText
    private lateinit var etUnidadHabitacional: EditText
    private lateinit var etResidenteId: EditText
    private lateinit var spinnerEstadoPago: Spinner
    private lateinit var btnGuardarCuota: Button
    private lateinit var pagosApiService: PagosApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_cuota)

        etMonto = findViewById(R.id.et_monto)
        etFechaVencimiento = findViewById(R.id.et_fecha_vencimiento)
        etUnidadHabitacional = findViewById(R.id.et_unidad_habitacional)
        etResidenteId = findViewById(R.id.et_residente_id)
        spinnerEstadoPago = findViewById(R.id.spinner_estado_pago)
        btnGuardarCuota = findViewById(R.id.btn_guardar_cuota)

        setupSpinner()
        setupRetrofit()

        btnGuardarCuota.setOnClickListener {
            if (validateInput()) {
                createCuota()
            }
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, EstadoPago.values().map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEstadoPago.adapter = adapter
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/api/") // Make sure this is the correct URL for your API
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        pagosApiService = retrofit.create(PagosApiService::class.java)
    }

    private fun validateInput(): Boolean {
        if (etMonto.text.toString().trim().isEmpty() || etMonto.text.toString().toDouble() <= 0) {
            Toast.makeText(this, "El monto debe ser mayor a cero", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etFechaVencimiento.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "La fecha de vencimiento es requerida", Toast.LENGTH_SHORT).show()
            return false
        }
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            val expiryDate = sdf.parse(etFechaVencimiento.text.toString())
            if (expiryDate != null && expiryDate.before(Date())) {
                Toast.makeText(this, "La fecha de vencimiento no puede ser anterior a la fecha actual", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha inválido. Use YYYY-MM-DD", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etUnidadHabitacional.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "La unidad habitacional es requerida", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etResidenteId.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "El ID del residente es requerido", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createCuota() {
        val cuota = Cuota(
            id = UUID.randomUUID().toString(),
            monto = etMonto.text.toString().toDouble(),
            fechaVencimiento = etFechaVencimiento.text.toString(),
            unidadHabitacional = etUnidadHabitacional.text.toString(),
            residenteId = etResidenteId.text.toString(),
            estado = EstadoPago.valueOf(spinnerEstadoPago.selectedItem.toString())
        )

        pagosApiService.createCuota(cuota).enqueue(object : Callback<GenericCuotaResponse> {
            override fun onResponse(call: Call<GenericCuotaResponse>, response: Response<GenericCuotaResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegistroCuota, "Cuota registrada con éxito", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity after successful registration
                } else {
                    Toast.makeText(this@RegistroCuota, "Error al registrar la cuota", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericCuotaResponse>, t: Throwable) {
                Toast.makeText(this@RegistroCuota, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
