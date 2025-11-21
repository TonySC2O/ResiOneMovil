package com.example.resionemobile.mantenimiento

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R

class MantenimientoMain : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenimiento_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val btnRegistrarMantenimiento: Button = findViewById(R.id.btnRegistrarMantenimiento)
        val btnHistorialMantenimientos: Button = findViewById(R.id.btnHistorialMantenimientos)

        btnRegistrarMantenimiento.setOnClickListener {
            val intent = Intent(this, RegistrarMantenimiento::class.java)
            startActivity(intent)
        }

        btnHistorialMantenimientos.setOnClickListener {
            val intent = Intent(this, HistorialMantenimientos::class.java)
            startActivity(intent)
        }
    }
}
