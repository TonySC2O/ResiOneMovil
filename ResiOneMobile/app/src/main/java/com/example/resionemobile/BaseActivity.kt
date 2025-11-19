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

/**
 * Activity base que proporciona funcionalidad común para todas las activities de la aplicación.
 * 
 * Funcionalidades centralizadas:
 * - Manejo del menú de navegación global
 * - Navegación entre las principales pantallas de la app
 * 
 * Todas las activities deben heredar de esta clase para mantener consistencia
 * en la navegación y el comportamiento del menú.
 */
abstract class BaseActivity : AppCompatActivity() {

    // ============ SISTEMA DE USUARIO ACTUAL (SIMULACIÓN) ============
    /**
     * Usuario actualmente autenticado en la sesión.
     * 
     * Valores de prueba:
     * - UsuarioDePrueba: Usuario normal con permisos básicos
     * - UsuarioExtra: Usuario normal adicional para testing
     * - UsuarioAdmin: Administrador con permisos completos
     * 
     * IMPORTANTE: Este es un sistema de simulación TEMPORAL para testing.
     * Debe ser reemplazado por un sistema de autenticación real (login con credenciales,
     * tokens de sesión, etc.) cuando se integre con el backend.
     * 
     * TODO: Reemplazar con sistema de autenticación real
     * TODO: Implementar SharedPreferences o base de datos para persistir sesión
     * TODO: Conectar con API de login cuando esté disponible
     */
    protected var currentUser: String = "UsuarioDePrueba"

    /**
     * Configura el botón de cambio de usuario para simulación.
     * Este método debe ser llamado desde onCreate() de las Activities que necesiten
     * el botón de simulación de cambio de usuario.
     * 
     * El botón cicla entre tres usuarios: UsuarioDePrueba → UsuarioExtra → UsuarioAdmin
     * 
     * IMPORTANTE: Este método y sus botones asociados deben ser REMOVIDOS en producción.
     * 
     * @param buttonId ID del botón en el layout (ej: R.id.btn_switch_user)
     * @param onUserChanged Callback opcional que se ejecuta después de cambiar el usuario
     */
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
            
            // Recargar menú para mostrar/ocultar opción de admin
            invalidateOptionsMenu()
            
            // Ejecutar callback si existe (para recargar datos, actualizar UI, etc.)
            onUserChanged?.invoke()
        }
    }

    /**
     * Infla el menú de opciones en la ActionBar.
     * El menú se define en res/menu/menu_main.xml
     * 
     * Muestra u oculta la opción "Administrar Reservas" según el usuario actual.
     * Solo UsuarioAdmin puede ver esta opción.
     * 
     * Este métod0 puede ser sobreescrito por las activities hijas si necesitan
     * agregar items adicionales al menú.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        // Mostrar "Administrar Reservas" solo para administradores
        val adminReservasItem = menu.findItem(R.id.action_admin_reservas)
        adminReservasItem?.isVisible = (currentUser == "UsuarioAdmin")
        
        return true
    }

    /**
     * Maneja los clicks en los items del menú de opciones de forma centralizada.
     * 
     * Opciones disponibles:
     * - Reservas de Espacios: Navega a la pantalla de reservas
     * - Crear Reportes: Navega a la pantalla de creación de reportes
     * - Settings: Configuración (por implementar)
     * 
     * Las activities hijas pueden sobreescribir este métod0 y llamar a super.onOptionsItemSelected()
     * para mantener la funcionalidad base y agregar opciones adicionales.
     */
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
            R.id.action_settings -> {
                // TODO: Implementar navegación a configuración cuando esté disponible
                Toast.makeText(this, "Configuración - Por implementar", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Navega a la pantalla de Reservas de Espacios.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToReservas() {
        if (this is ReservarEspacio) {
            Toast.makeText(this, "Ya estás en Reservas de Espacios", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, ReservarEspacio::class.java)
            startActivity(intent)
        }
    }

    /**
     * Navega a la pantalla de Administrar Reservas (solo para administradores).
     * Verifica que el usuario sea admin y que no estemos ya en esa pantalla.
     */
    private fun navigateToAdminReservas() {
        if (currentUser != "UsuarioAdmin") {
            Toast.makeText(this, "Acceso denegado: Solo administradores", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (this is AdminReservas) {
            Toast.makeText(this, "Ya estás en Administrar Reservas", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, AdminReservas::class.java)
            startActivity(intent)
        }
    }

    /**
     * Navega a la pantalla de Ver Reportes.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToVerReportes() {
        if (this is Reportes) {
            Toast.makeText(this, "Ya estás en Ver Reportes", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, Reportes::class.java)
            startActivity(intent)
        }
    }

    /**
     * Navega a la pantalla de Crear Reportes.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToCrearReportes() {
        if (this is CrearReporte) {
            Toast.makeText(this, "Ya estás en Crear Reportes", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, CrearReporte::class.java)
            startActivity(intent)
        }
    }

    /**
     * Navega a la pantalla principal (MainActivity/Inicio).
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToInicio() {
        if (this is MainActivity) {
            Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
