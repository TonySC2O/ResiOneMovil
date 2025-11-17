package Pagos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class PagosMain : AppCompatActivity() {

    private lateinit var adapter: CuotasAdapter
    private val cuotasList = mutableListOf<Cuota>()
    private lateinit var pagosApiService: PagosApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagos_main)

        val rvCuotas = findViewById<RecyclerView>(R.id.rv_cuotas)
        val btnAddCuota = findViewById<Button>(R.id.btn_add_cuota)
        val btnHistorialPagos = findViewById<Button>(R.id.btn_historial_pagos)

        adapter = CuotasAdapter(cuotasList) { cuota ->
            val intent = Intent(this, RegistroPago::class.java)
            intent.putExtra("cuota", cuota)
            startActivity(intent)
        }
        rvCuotas.layoutManager = LinearLayoutManager(this)
        rvCuotas.adapter = adapter

        setupRetrofit()

        btnAddCuota.setOnClickListener {
            val intent = Intent(this, RegistroCuota::class.java)
            startActivity(intent)
        }

        btnHistorialPagos.setOnClickListener {
            val intent = Intent(this, HistorialPagos::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchCuotas()
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/api/") // Asegúrate de que esta sea la URL correcta de tu API
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        pagosApiService = retrofit.create(PagosApiService::class.java)
    }

    private fun fetchCuotas() {
        pagosApiService.getCuotas().enqueue(object : Callback<CuotaListResponse> {
            override fun onResponse(call: Call<CuotaListResponse>, response: Response<CuotaListResponse>) {
                if (response.isSuccessful) {
                    response.body()?.cuotas?.let { adapter.updateCuotas(it) }
                } else {
                    Toast.makeText(this@PagosMain, "Error cargando cuotas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CuotaListResponse>, t: Throwable) {
                Toast.makeText(this@PagosMain, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
