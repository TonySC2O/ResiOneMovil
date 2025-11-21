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

// IMPORTS CORREGIDOS SEGÚN TUS CARPETAS REALES
import Reservas.AdminReservas
import Reservas.ReservarEspacio
import Reportes.CrearReporte
import Reportes.Reportes
import Registro.PerfilView                  // ← funciona si PerfilView.kt tiene package Registro
import com.example.resionemobile.mantenimiento.RegistrarMantenimiento
import Comunicados.ComunicadosFeed           // ← para el botón "Inicio"

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
        if (json != null) {
            try {
                currentUser = Gson().fromJson(json, UsuarioData::class.java)
                esAdministrador = currentUser?.esAdministrador == true
                rolUsuario = (currentUser?.rol ?: "RESIDENTE").uppercase()
            } catch (e: Exception) {
                currentUser = null
                esAdministrador = false
                rolUsuario = "RESIDENTE"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Ocultar opciones de admin por defecto
        menu.findItem(R.id.action_admin_reservas)?.isVisible = false
        //menu.findItem(R.id.action_registrar_mantenimiento)?.isVisible = false
        //menu.findItem(R.id.action_registro_entrada)?.isVisible = false
        //menu.findItem(R.id.action_registro_salida)?.isVisible = false

        when (rolUsuario) {
            "ADMIN" -> {
                menu.findItem(R.id.action_admin_reservas)?.isVisible = true
                //menu.findItem(R.id.action_registrar_mantenimiento)?.isVisible = true
                //menu.findItem(R.id.action_registro_entrada)?.isVisible = true
                //menu.findItem(R.id.action_registro_salida)?.isVisible = true
            }
            "TECNICO_MANTENIMIENTO", "TÉCNICO DE MANTENIMIENTO" -> {
                //menu.findItem(R.id.action_registrar_mantenimiento)?.isVisible = true
            }
            "AUXILIAR_DE_SEGURIDAD", "AUXILIAR DE SEGURIDAD" -> {
                //menu.findItem(R.id.action_registro_entrada)?.isVisible = true
               // menu.findItem(R.id.action_registro_salida)?.isVisible = true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reservas -> startActivity(Intent(this, ReservarEspacio::class.java))
            R.id.action_admin_reservas -> startActivity(Intent(this, AdminReservas::class.java))
            R.id.action_reportes -> startActivity(Intent(this, CrearReporte::class.java))
            R.id.action_ver_reportes -> startActivity(Intent(this, Reportes::class.java))
            R.id.action_inicio -> startActivity(Intent(this, MainActivity::class.java))
            //R.id.action_perfil -> startActivity(Intent(this, PerfilView::class.java))
            //R.id.action_registro_entrada -> startActivity(Intent(this, RegistroEntrada::class.java))
           // R.id.action_registro_salida -> startActivity(Intent(this, RegistroSalida::class.java))
            //R.id.action_registrar_mantenimiento -> startActivity(Intent(this, RegistrarMantenimiento::class.java))
           // R.id.action_chatbot -> startActivity(Intent(this, ChatBotActivity::class.java))
            R.id.action_salir -> {
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}