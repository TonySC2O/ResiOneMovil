package com.example.resionemobile.finanzas

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resionemobile.api.RetrofitClient
import com.example.resionemobile.databinding.ActivityHistorialPagosBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialPagos : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialPagosBinding
    private lateinit var adapter: HistorialPagosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialPagosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        obtenerHistorialPagos()
    }

    private fun setupRecyclerView() {
        adapter = HistorialPagosAdapter(emptyList()) { pago ->
            // TODO: Lógica para ver/descargar factura
            Toast.makeText(this, "Visualizando factura para el pago ${pago.id}", Toast.LENGTH_SHORT).show()
        }
        binding.rvHistorialPagos.adapter = adapter
        binding.rvHistorialPagos.layoutManager = LinearLayoutManager(this)
    }

    private fun obtenerHistorialPagos() {
        // Si el usuario es administrador, residenteId es null para obtener todos los pagos.
        // Si es residente, se debe pasar su ID.
        val residenteId = null  // Cambiar por el ID del residente si no es admin

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.historialPagos(residenteId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val pagos = response.body() ?: emptyList()
                        adapter.actualizarPagos(pagos)
                    } else {
                        Toast.makeText(this@HistorialPagos, "Error al obtener el historial", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistorialPagos, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
