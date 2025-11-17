package Pagos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HistorialPagos : AppCompatActivity() {

    private lateinit var adapter: PagosAdapter
    private val pagosList = mutableListOf<Pago>()
    private lateinit var pagosApiService: PagosApiService

    // TODO: Determinar el tipo de usuario (admin o residente) y el ID del residente
    private val userType = "ADMIN" // O "RESIDENTE"
    private val residentId = "some_resident_id" // ID del residente si es necesario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_pagos)

        val rvPagos = findViewById<RecyclerView>(R.id.rv_pagos)

        adapter = PagosAdapter(pagosList) { pago ->
            // TODO: Implementar la lógica para generar el PDF
            Toast.makeText(this, "Generando PDF para el pago ${pago.id}", Toast.LENGTH_SHORT).show()
        }
        rvPagos.layoutManager = LinearLayoutManager(this)
        rvPagos.adapter = adapter

        setupRetrofit()
        fetchPagos()
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.10:5050/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        pagosApiService = retrofit.create(PagosApiService::class.java)
    }

    private fun fetchPagos() {
        pagosApiService.getPagos().enqueue(object : Callback<PagoListResponse> {
            override fun onResponse(call: Call<PagoListResponse>, response: Response<PagoListResponse>) {
                if (response.isSuccessful) {
                    response.body()?.pagos?.let {
                        val filteredList = if (userType == "ADMIN") {
                            it
                        } else {
                            it.filter { pago -> pago.residenteId == residentId }
                        }
                        adapter.updatePagos(filteredList)
                    }
                } else {
                    Toast.makeText(this@HistorialPagos, "Error cargando pagos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PagoListResponse>, t: Throwable) {
                Toast.makeText(this@HistorialPagos, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
