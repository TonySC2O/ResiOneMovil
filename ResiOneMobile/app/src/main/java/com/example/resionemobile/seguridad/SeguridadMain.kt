package com.example.resionemobile.seguridad

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.resionemobile.R

class SeguridadMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguridad_main)

        val btnRegistrarEntrada: Button = findViewById(R.id.btnRegistrarEntrada)
        val btnRegistrarSalida: Button = findViewById(R.id.btnRegistrarSalida)
        val btnVerBitacora: Button = findViewById(R.id.btnVerBitacora)

        btnRegistrarEntrada.setOnClickListener {
            val intent = Intent(this, RegistroEntrada::class.java)
            startActivity(intent)
        }

        btnRegistrarSalida.setOnClickListener {
            val intent = Intent(this, RegistroSalida::class.java)
            startActivity(intent)
        }

        btnVerBitacora.setOnClickListener {
            val intent = Intent(this, Bitacora::class.java)
            startActivity(intent)
        }
    }
}
