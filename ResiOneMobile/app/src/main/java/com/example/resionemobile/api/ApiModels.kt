package com.example.resionemobile.api

import com.google.gson.annotations.SerializedName



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

data class Comunicado(
    @SerializedName("_id") val id: String,
    val titulo: String?,
    val contenido: String,
    val fechaPublicacion: String?,
    val estado: String?,
    val creadoPorAdministrador: Boolean?,
    val ultimaActualizacion: String?
)

// Response lista: { comunicados: [...] }
data class ComunicadoListResponse(
    val mensaje: String? = null,
    @SerializedName("comunicados") val comunicados: List<Comunicado> = emptyList()
)

// Response al crear/editar: devuelve el objeto creado/actualizado
data class ComunicadoResponse(
    val mensaje: String? = null,
    val comunicado: Comunicado? = null
)

// Request para crear (ajusta campos según quieras enviar)
data class CrearComunicadoRequest(
    val titulo: String,
    val contenido: String,
    val creadoPorAdministrador: Boolean = true // o false segun tu lógica
)

// Request para editar
data class EditarComunicadoRequest(
    val titulo: String?,
    val contenido: String?
)