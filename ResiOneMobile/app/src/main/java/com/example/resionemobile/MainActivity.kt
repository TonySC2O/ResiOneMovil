package com.example.resionemobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import Registro.CrearRegistro

/**
 * Activity principal de la aplicación.
 * Pantalla de inicio simple con acceso al menú de navegación.
 * Ahora funciona como pantalla de Login (mock) y navega a Register / ForgotPassword.
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

        // Configurar botón de cambio de usuario para testing (si existe en layout)
        setupUserSwitchButton(R.id.btn_switch_user)

        // -------------------------
        // VIEWS DE LOGIN
        // -------------------------
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Acción del botón ingresar
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Navegar a registro -> abrir CrearRegistro en package Registro
        tvRegister.setOnClickListener {
            navigateToRegister()
        }

        // Navegar a pantalla de olvido de contraseña
        tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    /**
     * Función de login. Actualmente es un mock (solo para testing).
     * Reemplaza el contenido por la llamada a tu API (Retrofit + Coroutines).
     */
    private fun loginUser(email: String, password: String) {
        // TODO: reemplazar por llamada real a la API (Retrofit). Ejemplo:
        // val response = api.login(LoginRequest(email, password))
        // if (response.isSuccessful) { guardar token y navegar }

        // MOCK: credenciales de testing
        if (email == "admin@resi.com" && password == "123456") {
            Toast.makeText(this, "Bienvenido!", Toast.LENGTH_SHORT).show()
            // Navega a la pantalla principal / feed luego del login
            // startActivity(Intent(this, MenuPrincipal::class.java))

            // Si quieres abrir registro tras login exitoso (no usual), descomenta:
            // startActivity(Intent(this, CrearRegistro::class.java))
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToRegister() {
        val i = Intent(this, CrearRegistro::class.java)
        startActivity(i)
    }


    private fun navigateToForgotPassword() {
        try {
            // si más adelante creas la activity de recuperación:
            // val i = Intent(this, Registro.ForgotPasswordActivity::class.java)
            // startActivity(i)
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "ForgotPasswordActivity no encontrada. Crea la activity para cambiar la contraseña.", Toast.LENGTH_SHORT).show()
        }
    }
}
