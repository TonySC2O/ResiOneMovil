package Registro

import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import java.util.UUID

class CrearRegistro : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_registro)

        // toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val etName = findViewById<EditText>(R.id.et_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etPasswordConfirm = findViewById<EditText>(R.id.et_password_confirm)
        val etHouseNumber = findViewById<EditText>(R.id.et_house_number)

        val spinnerRole = findViewById<Spinner>(R.id.spinner_role)
        val etAdminCode = findViewById<EditText>(R.id.et_admin_code)
        val adminCodeContainer = findViewById<LinearLayout>(R.id.admin_code_container)

        val btnRegister = findViewById<Button>(R.id.btn_register)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        // Setup spinner
        val roles = listOf("Residente", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Show/hide admin code field
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                adminCodeContainer.visibility =
                    if (roles[position] == "Admin") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            val pass2 = etPasswordConfirm.text.toString()
            val houseNumber = etHouseNumber.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()
            val adminCode = etAdminCode.text.toString().trim()

            // Validaciones
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Ingrese el correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (houseNumber.isEmpty()) {
                Toast.makeText(this, "Ingrese el número de casa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != pass2) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (role == "Admin" && adminCode.isEmpty()) {
                Toast.makeText(this, "Ingrese el código de administrador", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoUsuario = UsuarioRegistro(
                id = UUID.randomUUID().toString(),
                nombre = name,
                email = email,
                password = pass,
                role = role,
                houseNumber = houseNumber,
                adminCode = if (role == "Admin") adminCode else null
            )

            RegistroStore.agregar(nuevoUsuario)

            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()

            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}

data class UsuarioRegistro(
    val id: String,
    val nombre: String,
    val email: String,
    val password: String,
    val role: String,
    val houseNumber: String,
    val adminCode: String? = null
)

object RegistroStore {
    private val lista = mutableListOf<UsuarioRegistro>()
    fun agregar(u: UsuarioRegistro) = lista.add(u)
    fun todos() = lista.toList()
}
