package Comunicados;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.api.Post
import com.example.resionemobile.api.PostListResponse
import com.example.resionemobile.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.example.resionemobile.R
import Pagos.PagosMain

class ComunicadosFeed : AppCompatActivity() {

    private lateinit var adapter: PostsAdapter
    private val postsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunicados_feed)

        val rvPosts = findViewById<RecyclerView>(R.id.rv_posts)
        val etNewPost = findViewById<EditText>(R.id.et_new_post)
        val btnPost = findViewById<Button>(R.id.btn_post)
        val btnPagos = findViewById<Button>(R.id.btn_pagos)

        adapter = PostsAdapter(postsList, onEdit = { post -> editPost(post) }, onDelete = { post -> deletePost(post) })
        rvPosts.layoutManager = LinearLayoutManager(this)
        rvPosts.adapter = adapter

        btnPost.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Escribe algo antes de publicar", Toast.LENGTH_SHORT).show()
            } else {
                createPost(content)
                etNewPost.text.clear()
            }
        }

        btnPagos.setOnClickListener {
            val intent = Intent(this, PagosMain::class.java)
            startActivity(intent)
        }

        fetchPosts()
    }

    private fun fetchPosts() {
        RetrofitClient.api.getPosts().enqueue(object : Callback<PostListResponse> {
            override fun onResponse(call: Call<PostListResponse>, response: Response<PostListResponse>) {
                if (response.isSuccessful) {
                    response.body()?.posts?.let { adapter.updatePosts(it) }
                } else {
                    Toast.makeText(this@ComunicadosFeed, "Error cargando posts", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PostListResponse>, t: Throwable) {
                Toast.makeText(this@ComunicadosFeed, "Error de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createPost(content: String) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val post = Post(id = UUID.randomUUID().toString(), autor = "Yo", contenido = content, fecha = date)

        RetrofitClient.api.createPost(post).enqueue(object : Callback<com.example.resionemobile.api.GenericPostResponse> {
            override fun onResponse(call: Call<com.example.resionemobile.api.GenericPostResponse>, response: Response<com.example.resionemobile.api.GenericPostResponse>) {
                if (response.isSuccessful) {
                    response.body()?.post?.let { adapter.addPost(it) }
                }
            }

            override fun onFailure(call: Call<com.example.resionemobile.api.GenericPostResponse>, t: Throwable) {}
        })
    }

    private fun editPost(post: Post) {
        Toast.makeText(this, "Funcionalidad editar aún no implementada", Toast.LENGTH_SHORT).show()
    }

    private fun deletePost(post: Post) {
        RetrofitClient.api.deletePost(post.id).enqueue(object : Callback<com.example.resionemobile.api.GenericPostResponse> {
            override fun onResponse(call: Call<com.example.resionemobile.api.GenericPostResponse>, response: Response<com.example.resionemobile.api.GenericPostResponse>) {
                if (response.isSuccessful) adapter.removePost(post)
            }
            override fun onFailure(call: Call<com.example.resionemobile.api.GenericPostResponse>, t: Throwable) {}
        })
    }
}
