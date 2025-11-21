package com.example.resionemobile

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar

class Inicio : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        actualizarDatosUsuario()
    }

    private fun actualizarDatosUsuario() {
        val tvBienvenida: TextView = findViewById(R.id.tvBienvenida)
        val tvNombre: TextView = findViewById(R.id.tvNombre)
        val tvIdentificacion: TextView = findViewById(R.id.tvIdentificacion)
        val tvResidencia: TextView = findViewById(R.id.tvResidencia)
        val tvRol: TextView = findViewById(R.id.tvRol)

        Log.d("Inicio", "currentUser: $currentUser")

        // Cargar datos del usuario actual
        currentUser?.let { usuario ->
            Log.d("Inicio", "Usuario encontrado: ${usuario.nombre}, ${usuario.apartamento}, ${usuario.rol}, esAdmin: ${usuario.esAdministrador}")
            tvBienvenida.text = "¡Bienvenido!"
            tvNombre.text = "Nombre: ${usuario.nombre}"
            tvIdentificacion.text = "Identificación: ${usuario.identificacion ?: "No especificada"}"
            tvResidencia.text = "Residencia: ${usuario.apartamento ?: "No especificada"}"
            
            // Determinar el rol a mostrar
            val rolMostrar = when {
                usuario.esAdministrador -> "ADMINISTRADOR"
                !usuario.rol.isNullOrBlank() -> usuario.rol!!
                else -> "RESIDENTE"
            }
            tvRol.text = "Rol: $rolMostrar"
        } ?: run {
            Log.d("Inicio", "Usuario es null")
            tvBienvenida.text = "Bienvenido"
            tvNombre.text = "Nombre: No disponible"
            tvIdentificacion.text = "Identificación: No disponible"
            tvResidencia.text = "Residencia: No disponible"
            tvRol.text = "Rol: No disponible"
        }
    }
}
