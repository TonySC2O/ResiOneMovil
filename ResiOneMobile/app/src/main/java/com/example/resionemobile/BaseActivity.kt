package com.example.resionemobile

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import Reportes.CrearReporte
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

    /**
     * Infla el menú de opciones en la ActionBar.
     * El menú se define en res/menu/menu_main.xml
     * 
     * Este métod0 puede ser sobreescrito por las activities hijas si necesitan
     * agregar items adicionales al menú.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
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
            R.id.action_reportes -> {
                navigateToReportes()
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
     * Navega a la pantalla de Crear Reportes.
     * Verifica que no estemos ya en esa pantalla antes de navegar.
     */
    private fun navigateToReportes() {
        if (this is CrearReporte) {
            Toast.makeText(this, "Ya estás en Crear Reportes", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, CrearReporte::class.java)
            startActivity(intent)
        }
    }
}
