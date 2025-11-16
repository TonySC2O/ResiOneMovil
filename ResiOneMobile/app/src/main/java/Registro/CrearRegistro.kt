package Registro

import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import com.example.resionemobile.api.RegistroRequest
import com.example.resionemobile.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class CrearRegistro : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_registro)

        // Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Campos
        val etName = findViewById<EditText>(R.id.et_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etId = findViewById<EditText>(R.id.et_id)
        val etHouseNumber = findViewById<EditText>(R.id.et_house_number)
        val etHabitantes = findViewById<EditText>(R.id.et_habitantes)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etPasswordConfirm = findViewById<EditText>(R.id.et_password_confirm)
        val spinnerRole = findViewById<Spinner>(R.id.spinner_role)
        val etAdminCode = findViewById<EditText>(R.id.et_admin_code)
        val adminCodeContainer = findViewById<LinearLayout>(R.id.admin_code_container)

        val btnRegister = findViewById<Button>(R.id.btn_register)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        // Configurar Spinner
        val roles = listOf("Residente", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                adminCodeContainer.visibility =
                    if (roles[position] == "Admin") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val idNumber = etId.text.toString().trim()
            val houseNumber = etHouseNumber.text.toString().trim()
            val habitantesStr = etHabitantes.text.toString().trim()
            val pass = etPassword.text.toString()
            val pass2 = etPasswordConfirm.text.toString()
            val role = spinnerRole.selectedItem.toString()
            val adminCode = etAdminCode.text.toString().trim()

            // Validaciones locales
            when {
                name.isEmpty() -> showToast("Ingrese el nombre")
                email.isEmpty() -> showToast("Ingrese el correo")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToast("Correo inválido")
                phone.length != 8 || !phone.all { it.isDigit() } -> showToast("Teléfono debe tener 8 dígitos")
                idNumber.length != 9 || !idNumber.all { it.isDigit() } -> showToast("Identificación debe tener 9 dígitos")
                houseNumber.isEmpty() -> showToast("Ingrese número de apartamento")
                habitantesStr.isEmpty() || habitantesStr.toIntOrNull() == null || habitantesStr.toInt() < 1 ->
                    showToast("Habitantes inválido")
                pass.length < 10 -> showToast("Contraseña debe tener al menos 10 caracteres (6 letras + 4 números + .)")
                !pass.matches(Regex("^[A-Za-z]{6}\\d{4}\\.\$")) -> showToast("Contraseña debe tener 6 letras + 4 números + .")
                pass != pass2 -> showToast("Las contraseñas no coinciden")
                role == "Admin" && !adminCode.matches(Regex("^[A-Za-z]{4}\\d{2}\$")) -> showToast("Código admin inválido (4 letras + 2 números)")
                else -> {
                    // Campos correctos, enviar al backend
                    registrar(
                        nombre = name,
                        correo = email,
                        telefono = phone,
                        identificacion = idNumber,
                        apartamento = houseNumber,
                        habitantes = habitantesStr.toInt(),
                        contraseña = pass,
                        esAdministrador = role == "Admin",
                        codigoEmpleado = if (role == "Admin") adminCode else null
                    )
                }
            }
        }

        btnCancel.setOnClickListener { finish() }
    }

    private fun registrar(
        nombre: String,
        correo: String,
        telefono: String,
        identificacion: String,
        apartamento: String,
        habitantes: Int,
        contraseña: String,
        esAdministrador: Boolean,
        codigoEmpleado: String?
    ) {
        val req = RegistroRequest(
            nombre = nombre,
            correo = correo,
            telefono = telefono,
            identificacion = identificacion,
            apartamento = apartamento,
            habitantes = habitantes,
            contraseña = contraseña,
            esAdministrador = esAdministrador,
            codigoEmpleado = codigoEmpleado
        )

        RetrofitClient.api.registro(req).enqueue(object : Callback<com.example.resionemobile.api.GenericResponse> {
            override fun onResponse(
                call: Call<com.example.resionemobile.api.GenericResponse>,
                response: Response<com.example.resionemobile.api.GenericResponse>
            ) {
                if (response.isSuccessful) {
                    showToast("Registro exitoso")
                    finish()
                } else {
                    showToast("Error ${response.code()}: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<com.example.resionemobile.api.GenericResponse>, t: Throwable) {
                showToast("Error de red: ${t.localizedMessage}")
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
