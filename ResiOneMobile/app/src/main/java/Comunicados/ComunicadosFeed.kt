package Comunicados

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.BaseActivity
import com.example.resionemobile.R
import com.example.resionemobile.api.*
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComunicadosFeed : BaseActivity() {

    private lateinit var adapter: ComunicadosAdapter
    private val comunicadosList = mutableListOf<Comunicado>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunicados_feed)

        // Menú con 3 puntos aparece aquí
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        val rvPosts = findViewById<RecyclerView>(R.id.rv_posts)
        val etNewPost = findViewById<EditText>(R.id.et_new_post)
        val btnPost = findViewById<Button>(R.id.btn_post)

        adapter = ComunicadosAdapter(comunicadosList,
            onEdit = { openEditDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        rvPosts.layoutManager = LinearLayoutManager(this)
        rvPosts.adapter = adapter

        btnPost.setOnClickListener {
            val contenido = etNewPost.text.toString().trim()
            if (contenido.isEmpty()) {
                Toast.makeText(this, "Escribe algo antes de publicar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CrearComunicadoRequest(
                titulo = "Comunicado",
                contenido = contenido,
                creadoPorAdministrador = esAdministrador // Usa el rol real
            )

            RetrofitClient.api.crearComunicado(request).enqueue(object : Callback<ComunicadoResponse> {
                override fun onResponse(call: Call<ComunicadoResponse>, response: Response<ComunicadoResponse>) {
                    if (response.isSuccessful && response.body()?.comunicado != null) {
                        adapter.addComunicado(response.body()!!.comunicado!!)
                        etNewPost.text.clear()
                        Toast.makeText(this@ComunicadosFeed, "Publicado", Toast.LENGTH_SHORT).show()
                    } else {
                        fetchComunicados()
                        Toast.makeText(this@ComunicadosFeed, "Publicado (recargando)", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ComunicadoResponse>, t: Throwable) {
                    Toast.makeText(this@ComunicadosFeed, "Error de red", Toast.LENGTH_SHORT).show()
                }
            })
        }

        fetchComunicados()
    }

    private fun fetchComunicados() {
        RetrofitClient.api.getComunicados().enqueue(object : Callback<ComunicadoListResponse> {
            override fun onResponse(call: Call<ComunicadoListResponse>, response: Response<ComunicadoListResponse>) {
                if (response.isSuccessful) {
                    val list = response.body()?.comunicados ?: emptyList()
                    adapter.updateComunicados(list)
                }
            }
            override fun onFailure(call: Call<ComunicadoListResponse>, t: Throwable) {
                Toast.makeText(this@ComunicadosFeed, "Error al cargar", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openEditDialog(comunicado: Comunicado) {
        val input = EditText(this).apply { setText(comunicado.contenido) }
        AlertDialog.Builder(this)
            .setTitle("Editar")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoTexto = input.text.toString().trim()
                if (nuevoTexto.isNotEmpty()) {
                    RetrofitClient.api.editarComunicado(comunicado.id, EditarComunicadoRequest(
                        titulo = comunicado.titulo ?: "Comunicado",
                        contenido = nuevoTexto
                    ))
                        .enqueue(object : Callback<ComunicadoResponse> {
                            override fun onResponse(call: Call<ComunicadoResponse>, response: Response<ComunicadoResponse>) {
                                if (response.isSuccessful && response.body()?.comunicado != null) {
                                    adapter.updateSingle(response.body()!!.comunicado!!)
                                }
                            }
                            override fun onFailure(call: Call<ComunicadoResponse>, t: Throwable) {}
                        })
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete(comunicado: Comunicado) {
        AlertDialog.Builder(this)
            .setMessage("¿Eliminar comunicado?")
            .setPositiveButton("Sí") { _, _ ->
                RetrofitClient.api.eliminarComunicado(comunicado.id).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            adapter.removeComunicado(comunicado)
                            Toast.makeText(this@ComunicadosFeed, "Eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                })
            }
            .setNegativeButton("No", null)
            .show()
    }
}