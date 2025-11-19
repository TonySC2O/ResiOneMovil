package com.example.resionemobile.mantenimiento

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import com.example.resionemobile.api.HistorialManteResponse
import com.example.resionemobile.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistorialMantenimientos : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var historialAdapter: HistorialManteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_mante)

        setupRecyclerView()
        obtenerHistorial()
    }

    private fun setupRecyclerView() {
        rvHistorial = findViewById(R.id.rv_historial_mantenimiento)
        rvHistorial.layoutManager = LinearLayoutManager(this)
        historialAdapter = HistorialManteAdapter(emptyList())
        rvHistorial.adapter = historialAdapter
    }

    private fun obtenerHistorial() {
        RetrofitClient.api.obtenerHistorialMante().enqueue(object : Callback<HistorialManteResponse> {
            override fun onResponse(call: Call<HistorialManteResponse>, response: Response<HistorialManteResponse>) {
                if (response.isSuccessful) {
                    response.body()?.mantenimientos?.let {
                        historialAdapter.updateData(it)
                    }
                } else {
                    Toast.makeText(this@HistorialMantenimientos, "Error al obtener el historial", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HistorialManteResponse>, t: Throwable) {
                Toast.makeText(this@HistorialMantenimientos, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}