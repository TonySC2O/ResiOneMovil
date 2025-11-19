package com.example.resionemobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import Registro.CrearRegistro
import Comunicados.ComunicadosFeed
import Comunicados.ComunicadosAdapter
import com.example.resionemobile.api.LoginRequest
import com.example.resionemobile.api.LoginResponse
import com.example.resionemobile.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import com.google.gson.Gson

/**
 * Activity principal de la aplicación.
 * Pantalla de inicio simple con acceso al menú de navegación.
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Configurar toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Configurar botón de cambio de usuario para testing
        setupUserSwitchButton(R.id.btn_switch_user)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvRegister.setOnClickListener { navigateToRegister() }
        tvForgotPassword.setOnClickListener { navigateToForgotPassword() }
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(correo = email, contraseña = password)
        RetrofitClient.api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.usuario != null) {
                        // Guardar usuario en SharedPreferences como JSON bajo la clave "current_user"
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        val gson = Gson()
                        prefs.edit().putString("current_user", gson.toJson(body.usuario)).apply()

                        // SI TIENES TOKEN en la respuesta: guardarlo también (ejemplo)
                        // body.token?.let { prefs.edit().putString("auth_token", it).apply() }

                        Toast.makeText(this@MainActivity, "Bienvenido ${body.usuario.nombre}", Toast.LENGTH_SHORT).show()
                        // Navegar a ComunicadosFeed después del login exitoso
                        val intent = Intent(this@MainActivity, ComunicadosFeed::class.java)
                        startActivity(intent)
                        finish() // Cierra el login
                    } else {
                        Toast.makeText(this@MainActivity, body?.mensaje ?: "Error desconocido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // response.errorBody()?.string() puede consumir el stream; lo dejamos como antes
                    Toast.makeText(this@MainActivity, "Error ${response.code()}: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, CrearRegistro::class.java))
    }

    private fun navigateToForgotPassword() {
        Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
    }
}