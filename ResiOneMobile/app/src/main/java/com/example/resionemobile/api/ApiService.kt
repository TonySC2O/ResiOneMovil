package com.example.resionemobile.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.DELETE



interface ApiService {
    @POST("registro")
    fun registro(@Body body: RegistroRequest): Call<GenericResponse>

    @POST("login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @PUT("editar")
    fun editar(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("posts")
    fun getPosts(): Call<PostListResponse>

    @POST("posts")
    fun createPost(@Body body: Post): Call<GenericPostResponse>

    @PUT("posts/{id}")
    fun updatePost(@Path("id") id: String, @Body body: Post): Call<GenericPostResponse>

    @DELETE("posts/{id}")
    fun deletePost(@Path("id") id: String): Call<GenericPostResponse>

}
