package Seguridad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import com.example.resionemobile.R
import com.example.resionemobile.api.EntradaRequest
import com.example.resionemobile.api.EntradaResponse
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.api.VehiculoData
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class RegistroEntrada : AppCompatActivity() {

    private lateinit var etNombreVisitante: TextInputEditText
    private lateinit var etIdVisitante: TextInputEditText
    private lateinit var actvTipoVisita: AutoCompleteTextView
    private lateinit var etEmailVisitante: TextInputEditText
    private lateinit var cbTieneVehiculo: CheckBox
    private lateinit var layoutInfoVehiculo: LinearLayout
    private lateinit var etPlacaVehiculo: TextInputEditText
    private lateinit var etModeloVehiculo: TextInputEditText
    private lateinit var etDescripcionVehiculo: TextInputEditText
    private lateinit var btnRegistrar: Button
    private lateinit var tilInfoResidente: TextInputLayout
    private lateinit var etInfoResidente: TextInputEditText

    private val placaPattern = Regex("^[A-Z]{3}-?\\d{3,4}$", RegexOption.IGNORE_CASE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_entrada)

        setupViews()
        setupListeners()
        setupTipoVisitaDropdown()
    }

    private fun setupViews() {
        etNombreVisitante = findViewById(R.id.etNombreVisitante)
        etIdVisitante = findViewById(R.id.etIdVisitante)
        actvTipoVisita = findViewById(R.id.actvTipoVisita)
        etEmailVisitante = findViewById(R.id.etEmailVisitante)
        cbTieneVehiculo = findViewById(R.id.cbTieneVehiculo)
        layoutInfoVehiculo = findViewById(R.id.layoutInfoVehiculo)
        etPlacaVehiculo = findViewById(R.id.etPlacaVehiculo)
        etModeloVehiculo = findViewById(R.id.etModeloVehiculo)
        etDescripcionVehiculo = findViewById(R.id.etDescripcionVehiculo)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        tilInfoResidente = findViewById(R.id.tilInfoResidente)
        etInfoResidente = findViewById(R.id.etInfoResidente)
    }

    private fun setupListeners() {
        cbTieneVehiculo.setOnCheckedChangeListener { _, isChecked ->
            layoutInfoVehiculo.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        actvTipoVisita.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            if (selected == "Visita a Residente") {
                tilInfoResidente.visibility = View.VISIBLE
            } else {
                tilInfoResidente.visibility = View.GONE
            }
        }

        btnRegistrar.setOnClickListener { 
            if (validarCampos()) {
                registrarEntrada()
            }
        }
    }

    private fun setupTipoVisitaDropdown() {
        val tiposVisita = arrayOf("Recoger/Dejar Producto", "Visita a Residente", "Mantenimiento")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposVisita)
        actvTipoVisita.setAdapter(adapter)
    }

    private fun validarCampos(): Boolean {
        if (etNombreVisitante.text.isNullOrBlank()) {
            etNombreVisitante.error = "El nombre es requerido"
            return false
        }
        if (etIdVisitante.text.isNullOrBlank() || etIdVisitante.text?.length != 8) {
            etIdVisitante.error = "El ID debe tener 8 dígitos"
            return false
        }
        if (actvTipoVisita.text.isNullOrBlank()) {
            actvTipoVisita.error = "Seleccione un tipo de visita"
            return false
        }

        if (tilInfoResidente.visibility == View.VISIBLE && etInfoResidente.text.isNullOrBlank()){
            etInfoResidente.error = "La información del residente es requerida"
            return false
        }

        if (etEmailVisitante.text.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(etEmailVisitante.text.toString()).matches()) {
            etEmailVisitante.error = "Ingrese un correo válido"
            return false
        }

        if (cbTieneVehiculo.isChecked) {
            val placa = etPlacaVehiculo.text.toString().trim()
            if (placa.isBlank()) {
                etPlacaVehiculo.error = "La placa es requerida"
                return false
            } 
            if (!placaPattern.matches(placa)) {
                etPlacaVehiculo.error = "Formato de placa inválido (ej: ABC-123)"
                return false
            }
        }
        return true
    }

    private fun registrarEntrada() {
        val nombre = etNombreVisitante.text.toString()
        val id = etIdVisitante.text.toString()
        val tipoVisita = actvTipoVisita.text.toString()
        val email = etEmailVisitante.text.toString()
        val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        val vehiculo = if (cbTieneVehiculo.isChecked) {
            VehiculoData(
                placa = etPlacaVehiculo.text.toString().uppercase(Locale.ROOT),
                modelo = etModeloVehiculo.text.toString(),
                descripcion = etDescripcionVehiculo.text.toString()
            )
        } else null
        
        val residenteRelacionado = if (tilInfoResidente.visibility == View.VISIBLE) {
            etInfoResidente.text.toString()
        } else null

        val request = EntradaRequest(
            visitanteId = id,
            nombre = nombre,
            identificacion = id,  // Assuming visitanteId is the same as identificacion
            tipoVisita = tipoVisita,
            correo = email,
            fechaHoraIngreso = fechaHora,
            residenteRelacionado = residenteRelacionado,
            vehiculo = vehiculo
        )

        RetrofitClient.api.registrarEntrada(request).enqueue(object : Callback<EntradaResponse> {
            override fun onResponse(call: Call<EntradaResponse>, response: Response<EntradaResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegistroEntrada, "Registro exitoso. QR enviado al visitante.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@RegistroEntrada, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EntradaResponse>, t: Throwable) {
                Toast.makeText(this@RegistroEntrada, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}