package com.example.resionemobile

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import Reportes.CrearReporte
import Reportes.Reportes
import Reservas.AdminReservas
import Reservas.ReservarEspacio
import com.example.resionemobile.seguridad.RegistroEntrada
import com.example.resionemobile.seguridad.RegistroSalida
import com.example.resionemobile.chatbot.ChatBotActivity
import com.example.resionemobile.mantenimiento.RegistrarMantenimiento


/**
 * Activity base que proporciona funcionalidad común para todas las activities de la aplicación.
 * Incluye ahora acceso al ResiBot desde el menú principal.
 */
abstract class BaseActivity : AppCompatActivity() {

    // ============ SISTEMA DE USUARIO ACTUAL (SIMULACIÓN) ============
    protected var currentUser: String = "UsuarioDePrueba"

    protected fun setupUserSwitchButton(buttonId: Int, onUserChanged: (() -> Unit)? = null) {
        val btnSwitchUser = findViewById<android.widget.Button>(buttonId)
        btnSwitchUser?.setOnClickListener {
            currentUser = when (currentUser) {
                "UsuarioDePrueba" -> "UsuarioExtra"
                "UsuarioExtra" -> "UsuarioAdmin"
                else -> "UsuarioDePrueba"
            }
            btnSwitchUser.text = "Usuario: $currentUser (Cambiar)"
            Toast.makeText(this, "Usuario cambiado a: $currentUser", Toast.LENGTH_SHORT).show()
            invalidateOptionsMenu()
            onUserChanged?.invoke()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Mostrar "Administrar Reservas" solo para administradores
        val adminReservasItem = menu.findItem(R.id.action_admin_reservas)
        adminReservasItem?.isVisible = (currentUser == "UsuarioAdmin")

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reservas -> {
                navigateToReservas()
                true
            }
            R.id.action_admin_reservas -> {
                navigateToAdminReservas()
                true
            }
            R.id.action_ver_reportes -> {
                navigateToVerReportes()
                true
            }
            R.id.action_reportes -> {
                navigateToCrearReportes()
                true
            }
            R.id.action_inicio -> {
                navigateToInicio()
                true
            }
            R.id.action_visient -> {
                navigateToVisiEntradas()
                true
            }
            R.id.action_mantenimiento -> {
                navigateToRegMante()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Configuración - Por implementar", Toast.LENGTH_SHORT).show()
                true
            }

            // NUEVO: Acceso al Chatbot desde el menú
            R.id.action_chatbot -> {
                navigateToChatbot()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // ============ MÉTODOS DE NAVEGACIÓN ============

    private fun navigateToReservas() {
        if (this is ReservarEspacio) {
            Toast.makeText(this, "Ya estás en Reservas de Espacios", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, ReservarEspacio::class.java))
        }
    }

    private fun navigateToAdminReservas() {
        if (currentUser != "UsuarioAdmin") {
            Toast.makeText(this, "Acceso denegado: Solo administradores", Toast.LENGTH_SHORT).show()
            return
        }
        if (this is AdminReservas) {
            Toast.makeText(this, "Ya estás en Administrar Reservas", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, AdminReservas::class.java))
        }
    }

    private fun navigateToVerReportes() {
        if (this is Reportes) {
            Toast.makeText(this, "Ya estás en Ver Reportes", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, Reportes::class.java))
        }
    }

    private fun navigateToCrearReportes() {
        if (this is CrearReporte) {
            Toast.makeText(this, "Ya estás en Crear Reportes", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, CrearReporte::class.java))
        }
    }

    /**
     * Navega a la pantalla de Registrar Entradas.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToVisiEntradas() {
        if (this is RegistroEntrada) {
            Toast.makeText(this, "Ya estás en Registro de Entradas", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, RegistroEntrada::class.java)
            startActivity(intent)
        }
    }

    /**
     * Navega a la pantalla de Registrar Mantenimiento.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToRegMante() {
        if (this is RegistrarMantenimiento) {
            Toast.makeText(this, "Ya estás en Mantenimiento", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, RegistrarMantenimiento::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToInicio() {
        if (this is MainActivity) {
            Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    // NUEVO MÉTODO: Navegación al Chatbot
    private fun navigateToChatbot() {
        if (this is com.example.resionemobile.chatbot.ChatBotActivity) {
            Toast.makeText(this, "Ya estás en ResiBot", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }
    }
}