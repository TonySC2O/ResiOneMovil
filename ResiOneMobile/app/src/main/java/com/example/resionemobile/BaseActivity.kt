package com.example.resionemobile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.resionemobile.api.UsuarioData
import com.example.resionemobile.chatbot.ChatBotActivity
import com.example.resionemobile.seguridad.RegistroEntrada
import com.example.resionemobile.seguridad.RegistroSalida
import com.google.gson.Gson
import com.example.resionemobile.Reservas.AdminReservas
import com.example.resionemobile.Reservas.ReservarEspacio
import com.example.resionemobile.Reportes.CrearReporte
import com.example.resionemobile.Reportes.Reportes
import com.example.resionemobile.Registro.PerfilView
import com.example.resionemobile.mantenimiento.RegistrarMantenimiento
import com.example.resionemobile.mantenimiento.MantenimientoMain
import com.example.resionemobile.finanzas.FinanzasMain
import com.example.resionemobile.seguridad.SeguridadMain
import com.example.resionemobile.Comunicados.ComunicadosFeed

abstract class BaseActivity : AppCompatActivity() {

    protected var currentUser: UsuarioData? = null
    protected var esAdministrador = false
    protected var rolUsuario = "RESIDENTE"

    override fun onResume() {
        super.onResume()
        cargarUsuario()
        invalidateOptionsMenu()
    }

    private fun cargarUsuario() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val json = prefs.getString("current_user", null)
        android.util.Log.d("BaseActivity", "JSON guardado: $json")
        if (json != null) {
            try {
                currentUser = Gson().fromJson(json, UsuarioData::class.java)
                esAdministrador = currentUser?.esAdministrador == true
                rolUsuario = (currentUser?.rol ?: "RESIDENTE").uppercase()
                android.util.Log.d("BaseActivity", "Usuario cargado: $currentUser")
            } catch (e: Exception) {
                android.util.Log.e("BaseActivity", "Error al cargar usuario", e)
                currentUser = null
                esAdministrador = false
                rolUsuario = "RESIDENTE"
            }
        } else {
            android.util.Log.d("BaseActivity", "No hay usuario guardado")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Ocultar opciones restringidas por defecto
        menu.findItem(R.id.action_admin_reservas)?.isVisible = false
        menu.findItem(R.id.action_mantenimiento)?.isVisible = false
        menu.findItem(R.id.action_seguridad)?.isVisible = false

        // Mostrar opciones según el rol del usuario
        val esAdmin = esAdministrador || rolUsuario == "ADMIN"
        
        // Gestionar Reservas - Solo administradores
        if (esAdmin) {
            menu.findItem(R.id.action_admin_reservas)?.isVisible = true
        }

        // Mantenimiento - Administradores y Técnicos de Mantenimiento
        if (esAdmin || rolUsuario == "TÉCNICO DE MANTENIMIENTO" || rolUsuario == "TECNICO_MANTENIMIENTO") {
            menu.findItem(R.id.action_mantenimiento)?.isVisible = true
        }

        // Seguridad - Administradores y Auxiliares de Seguridad
        if (esAdmin || rolUsuario == "AUXILIAR DE SEGURIDAD" || rolUsuario == "AUXILIAR_DE_SEGURIDAD") {
            menu.findItem(R.id.action_seguridad)?.isVisible = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reservas -> startActivity(Intent(this, ReservarEspacio::class.java))
            R.id.action_admin_reservas -> startActivity(Intent(this, AdminReservas::class.java))
            R.id.action_reportes -> startActivity(Intent(this, CrearReporte::class.java))
            R.id.action_ver_reportes -> startActivity(Intent(this, Reportes::class.java))
            R.id.action_inicio -> startActivity(Intent(this, Inicio::class.java))
            R.id.action_comunicados -> startActivity(Intent(this, ComunicadosFeed::class.java))
            R.id.action_seguridad -> startActivity(Intent(this, SeguridadMain::class.java))
            R.id.action_mantenimiento -> startActivity(Intent(this, MantenimientoMain::class.java))
            R.id.action_finanzas -> startActivity(Intent(this, FinanzasMain::class.java))
            R.id.action_salir -> {
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}