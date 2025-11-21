package Registro

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.api.GenericResponse
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.api.UsuarioData
import com.example.resionemobile.databinding.ActivityPerfilBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import com.example.resionemobile.BaseActivity
import retrofit2.Response

class PerfilView : BaseActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val gson = Gson()
    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar (usar el ID 'toolbar' del XML)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // Botones
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener { enterEditMode() }
        binding.btnSave.setOnClickListener { saveChanges() }

        // Cargar user desde SharedPreferences
        currentUser = loadUserFromPrefs()
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        populateFields(currentUser!!)
        setEditable(false)
    }

    private fun populateFields(u: UsuarioData) {
        binding.etNombre.setText(u.nombre)
        binding.etCorreo.setText(u.correo)
        binding.etTelefono.setText(u.telefono ?: "")
        binding.etIdentificacion.setText(u.identificacion ?: "")
        binding.etApartamento.setText(u.apartamento ?: "")
        binding.etHabitantes.setText(u.habitantes?.toString() ?: "")
        // mostrar rol legible
        val visibleRol = when (u.rol?.uppercase()) {
            "ADMIN" -> "Admin"
            "TECNICO_MANTENIMIENTO" -> "Técnico de Mantenimiento"
            "AUXILIAR_SEGURIDAD" -> "Auxiliar de Seguridad"
            else -> "Residente"
        }
        binding.etRol.setText(visibleRol)
        binding.etCodigoEmpleado.setText(u.codigoEmpleado ?: "")
    }

    private fun setEditable(enable: Boolean) {
        editing = enable
        binding.etNombre.isEnabled = enable
        binding.etCorreo.isEnabled = enable
        binding.etTelefono.isEnabled = enable
        binding.etIdentificacion.isEnabled = enable
        binding.etApartamento.isEnabled = enable
        binding.etHabitantes.isEnabled = enable
        binding.etRol.isEnabled = enable
        binding.etCodigoEmpleado.isEnabled = enable

        binding.btnEdit.visibility = if (enable) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (enable) View.VISIBLE else View.GONE
    }

    private fun enterEditMode() {
        setEditable(true)
        Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show()
    }

    private fun saveChanges() {
        // Validaciones básicas
        val nombre = binding.etNombre.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val identificacion = binding.etIdentificacion.text.toString().trim()
        val apartamento = binding.etApartamento.text.toString().trim()
        val habitantesStr = binding.etHabitantes.text.toString().trim()
        val rolVisible = binding.etRol.text.toString().trim()
        val codigoEmpleado = binding.etCodigoEmpleado.text.toString().trim()

        when {
            nombre.isEmpty() -> { showToast("Ingrese el nombre"); return }
            correo.isEmpty() -> { showToast("Ingrese el correo"); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> { showToast("Correo inválido"); return }
            telefono.isNotEmpty() && (telefono.length != 8 || !telefono.all { it.isDigit() }) -> { showToast("Teléfono debe tener 8 dígitos"); return }
            identificacion.isNotEmpty() && (identificacion.length != 9 || !identificacion.all { it.isDigit() }) -> { showToast("Identificación debe tener 9 dígitos"); return }
            habitantesStr.isNotEmpty() && (habitantesStr.toIntOrNull() == null || habitantesStr.toInt() < 1) -> { showToast("Habitantes inválido"); return }
        }

        // mapear rol visible a valor para backend
        val roleToSend = when (rolVisible) {
            "Admin", "ADMIN" -> "ADMIN"
            "Técnico de Mantenimiento" -> "TECNICO_MANTENIMIENTO"
            "Auxiliar de Seguridad" -> "AUXILIAR_SEGURIDAD"
            else -> "RESIDENTE"
        }

        // armar body para /editar
        val body = mutableMapOf<String, Any>()
        body["nombre"] = nombre
        body["correo"] = correo
        if (telefono.isNotEmpty()) body["telefono"] = telefono
        if (identificacion.isNotEmpty()) body["identificacion"] = identificacion
        if (apartamento.isNotEmpty()) body["apartamento"] = apartamento
        if (habitantesStr.isNotEmpty()) body["habitantes"] = habitantesStr.toInt()
        body["rol"] = roleToSend
        if (codigoEmpleado.isNotEmpty()) body["codigoEmpleado"] = codigoEmpleado

        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.api.editar(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    // Construimos objeto UsuarioData actualizado (si backend no lo devuelve)
                    val updated = UsuarioData(
                        nombre = nombre,
                        correo = correo,
                        identificacion = if (identificacion.isBlank()) null else identificacion,
                        telefono = if (telefono.isBlank()) null else telefono,
                        apartamento = if (apartamento.isBlank()) null else apartamento,
                        habitantes = if (habitantesStr.isBlank()) null else habitantesStr.toInt(),
                        esAdministrador = roleToSend == "ADMIN",
                        codigoEmpleado = if (codigoEmpleado.isBlank()) null else codigoEmpleado,
                        rol = roleToSend
                    )
                    saveUserToPrefs(updated)
                    currentUser = updated
                    populateFields(updated)
                    setEditable(false)
                    showToast("Perfil actualizado")
                } else {
                    val err = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    showToast("Error ${response.code()}: ${err ?: "sin detalles"}")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                showToast("Error de red: ${t.localizedMessage ?: "desconocido"}")
            }
        })
    }

    private fun loadUserFromPrefs(): UsuarioData? {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("current_user", null)
        return if (!json.isNullOrEmpty()) {
            try { gson.fromJson(json, UsuarioData::class.java) } catch (e: Exception) { null }
        } else null
    }

    private fun saveUserToPrefs(user: UsuarioData) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("current_user", gson.toJson(user)).apply()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
