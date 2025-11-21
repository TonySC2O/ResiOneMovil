// Archivo: java/com/example/resionemobile/MainActivity.kt
package com.example.resionemobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.api.LoginRequest
import com.example.resionemobile.api.LoginResponse
import com.example.resionemobile.api.RetrofitClient
import com.google.gson.Gson
import Comunicados.ComunicadosFeed
import Registro.CrearRegistro   // ← Aquí está tu clase de registro
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Si ya hay sesión, saltar directo al feed
        if (hayUsuarioLogueado()) {
            startActivity(Intent(this, ComunicadosFeed::class.java))
            finish()
            return
        }

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)  // ← Asegúrate de tener este ID en tu XML

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvRegister.setOnClickListener {
            navigateToRegister()
        }

        tvForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    private fun hayUsuarioLogueado(): Boolean {
        val json = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("current_user", null)
        return !json.isNullOrEmpty()
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(correo = email, contraseña = password)
        RetrofitClient.api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.usuario != null) {
                    val usuario = response.body()!!.usuario!!

                    // Guardar usuario en SharedPreferences
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("current_user", Gson().toJson(usuario))
                        .apply()

                    Toast.makeText(this@MainActivity, "Bienvenido ${usuario.nombre}", Toast.LENGTH_LONG).show()

                    // Ir al feed de comunicados
                    startActivity(Intent(this@MainActivity, ComunicadosFeed::class.java))
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de conexión: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // ← Estas funciones YA ESTÁN DENTRO de la clase
    private fun navigateToRegister() {
        startActivity(Intent(this, CrearRegistro::class.java))
    }

    private fun navigateToForgotPassword() {
        Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
    }
}