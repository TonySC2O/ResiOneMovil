package Comunicados

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.example.resionemobile.R

class ComunicadosFeed : AppCompatActivity() {

    private lateinit var adapter: ComunicadosAdapter
    private val comunicadosList = mutableListOf<Comunicado>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunicados_feed)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val rvPosts = findViewById<RecyclerView>(R.id.rv_posts)
        val etNewPost = findViewById<EditText>(R.id.et_new_post)
        val btnPost = findViewById<Button>(R.id.btn_post)

        adapter = ComunicadosAdapter(comunicadosList,
            onEdit = { c -> openEditDialog(c) },
            onDelete = { c -> confirmDelete(c) }
        )
        rvPosts.layoutManager = LinearLayoutManager(this)
        rvPosts.adapter = adapter

        btnPost.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Escribe algo antes de publicar", Toast.LENGTH_SHORT).show()
            } else {
                btnPost.isEnabled = false
                // Aquí usamos título vacío por ahora (puedes pedir título separado)
                val request = CrearComunicadoRequest(titulo = "Comunicado", contenido = content, creadoPorAdministrador = true)
                RetrofitClient.api.crearComunicado(request).enqueue(object : Callback<ComunicadoResponse> {
                    override fun onResponse(call: Call<ComunicadoResponse>, response: Response<ComunicadoResponse>) {
                        btnPost.isEnabled = true
                        if (response.isSuccessful) {
                            val created = response.body()?.comunicado
                            if (created != null) {
                                adapter.addComunicado(created)
                                Toast.makeText(this@ComunicadosFeed, "Publicado", Toast.LENGTH_SHORT).show()
                                etNewPost.text.clear()
                            } else {
                                // fallback: refrescar lista
                                fetchComunicados()
                                Toast.makeText(this@ComunicadosFeed, "Publicado (recargando lista)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val err = try { response.errorBody()?.string() } catch (e: Exception) { "read_error" }
                            Log.e("ComFeed", "crear error ${response.code()} - $err")
                            Toast.makeText(this@ComunicadosFeed, "Error al publicar: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onFailure(call: Call<ComunicadoResponse>, t: Throwable) {
                        btnPost.isEnabled = true
                        Log.e("ComFeed", "crear fail", t)
                        Toast.makeText(this@ComunicadosFeed, "Error de red: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        fetchComunicados()
    }

    private fun fetchComunicados() {
        RetrofitClient.api.getComunicados().enqueue(object : Callback<ComunicadoListResponse> {
            override fun onResponse(call: Call<ComunicadoListResponse>, response: Response<ComunicadoListResponse>) {
                if (response.isSuccessful) {
                    val list = response.body()?.comunicados ?: emptyList()
                    adapter.updateComunicados(list)
                } else {
                    val err = try { response.errorBody()?.string() } catch (e: Exception) { "read_err" }
                    Log.e("ComFeed", "get error ${response.code()} - $err")
                    Toast.makeText(this@ComunicadosFeed, "Error cargando comunicados", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ComunicadoListResponse>, t: Throwable) {
                Log.e("ComFeed", "get fail", t)
                Toast.makeText(this@ComunicadosFeed, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openEditDialog(comunicado: Comunicado) {
        val input = EditText(this).apply { setText(comunicado.contenido) }
        AlertDialog.Builder(this)
            .setTitle("Editar comunicado")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = input.text.toString().trim()
                if (nuevo.isNotEmpty()) {
                    val body = EditarComunicadoRequest(titulo = comunicado.titulo, contenido = nuevo)
                    RetrofitClient.api.editarComunicado(comunicado.id, body).enqueue(object : Callback<ComunicadoResponse> {
                        override fun onResponse(call: Call<ComunicadoResponse>, response: Response<ComunicadoResponse>) {
                            if (response.isSuccessful) {
                                val updated = response.body()?.comunicado
                                if (updated != null) {
                                    adapter.updateSingle(updated)
                                    Toast.makeText(this@ComunicadosFeed, "Actualizado", Toast.LENGTH_SHORT).show()
                                } else {
                                    fetchComunicados()
                                    Toast.makeText(this@ComunicadosFeed, "Actualizado (recargando)", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val err = try { response.errorBody()?.string() } catch (e: Exception) { "read_err" }
                                Log.e("ComFeed", "edit error ${response.code()} - $err")
                                Toast.makeText(this@ComunicadosFeed, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<ComunicadoResponse>, t: Throwable) {
                            Log.e("ComFeed", "edit fail", t)
                            Toast.makeText(this@ComunicadosFeed, "Error de red al actualizar", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Contenido vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun confirmDelete(comunicado: Comunicado) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar este comunicado?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                RetrofitClient.api.eliminarComunicado(comunicado.id).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            adapter.removeComunicado(comunicado)
                            Toast.makeText(this@ComunicadosFeed, "Eliminado", Toast.LENGTH_SHORT).show()
                        } else {
                            val err = try { response.errorBody()?.string() } catch (e: Exception) { "read_err" }
                            Log.e("ComFeed", "delete error ${response.code()} - $err")
                            Toast.makeText(this@ComunicadosFeed, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Log.e("ComFeed", "delete fail", t)
                        Toast.makeText(this@ComunicadosFeed, "Error de red al eliminar", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .show()
    }
}
