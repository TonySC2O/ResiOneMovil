package com.example.resionemobile.finanzas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R

class FinanzasMain : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finanzas_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val btnRegistrarCuota: Button = findViewById(R.id.btnRegistrarCuota)
        val btnRegistrarPago: Button = findViewById(R.id.btnRegistrarPago)
        val btnHistorialPagos: Button = findViewById(R.id.btnHistorialPagos)

        btnRegistrarCuota.setOnClickListener {
            val intent = Intent(this, RegistroCuota::class.java)
            startActivity(intent)
        }

        btnRegistrarPago.setOnClickListener {
            val intent = Intent(this, RegistroPago::class.java)
            startActivity(intent)
        }

        btnHistorialPagos.setOnClickListener {
            val intent = Intent(this, HistorialPagos::class.java)
            startActivity(intent)
        }
    }
}
