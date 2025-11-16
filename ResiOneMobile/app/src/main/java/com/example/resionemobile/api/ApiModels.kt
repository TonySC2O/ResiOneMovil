package com.example.resionemobile.api

data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val telefono: String,
    val identificacion: String,
    val apartamento: String,
    val habitantes: Int,
    val contraseña: String,
    val esAdministrador: Boolean,
    val codigoEmpleado: String? = null
)


data class GenericResponse(
    val mensaje: String
)

data class LoginRequest(
    val correo: String,
    val contraseña: String
)

data class UsuarioData(
    val nombre: String,
    val correo: String,
    val identificacion: String?,
    val telefono: String?,
    val apartamento: String?,
    val habitantes: Int?,
    val esAdministrador: Boolean,
    val codigoEmpleado: String?
)

data class LoginResponse(
    val mensaje: String,
    val usuario: UsuarioData?
)

data class Post(
    val id: String,
    val autor: String,
    val contenido: String,
    val fecha: String
)

data class GenericPostResponse(
    val mensaje: String,
    val post: Post?
)

data class PostListResponse(
    val mensaje: String,
    val posts: List<Post>
)