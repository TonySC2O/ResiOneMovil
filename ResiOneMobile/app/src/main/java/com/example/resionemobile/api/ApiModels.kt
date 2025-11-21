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
    val codigoEmpleado: String? = null,
    val rol: String
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
    val codigoEmpleado: String?,
    val rol: String
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


data class Cuota(
    @SerializedName("_id") val id: String,
    val monto: Double,
    val fechaVencimiento: String,
    val unidadHabitacional: String,
    val residente: String,
    val estado: String,
    val createdAt: String
)

data class CuotaListResponse(
    val mensaje: String,
    val cuotas: List<Cuota>
)

data class CrearCuotaRequest(
    val monto: Double,
    val fechaVencimiento: String,
    val unidadHabitacional: String,
    val residente: String,
    val estado: String? = null
)

data class CrearCuotaResponse(
    val mensaje: String,
    val cuota: Cuota
)

data class Pago(
    @SerializedName("_id") val id: String,
    val cuotaId: String,
    val residenteId: String,
    val nombreResidente: String,
    val unidadHabitacional: String,
    val fechaPago: String,
    val metodoPago: String,
    val comprobantePDF: String?,
    val createdAt: String
)

data class PagoListResponse(
    val mensaje: String,
    val pagos: List<Pago>
)

data class PagoRequest(
    val cuotaId: String,
    val residenteId: String,
    val nombreResidente: String,
    val unidadHabitacional: String,
    val fechaPago: String,
    val metodoPago: String
)

data class Factura(
    @SerializedName("_id") val id: String,
    val numeroFactura: String,
    val pagoId: String,
    val cuotaId: String,
    val detalle: String,
    val nombreResidente: String,
    val metodoPago: String,
    val pdfPath: String?,
    val fechaEmision: String
)

data class FacturaResponse(
    val mensaje: String,
    val factura: Factura
)


data class EntradaRequest(
    val visitanteId: String,
    val nombre: String,
    val identificacion: String,
    val tipoVisita: String,
    val correo: String,
    val fechaHoraIngreso: String,
    val residenteRelacionado: String?,
    val vehiculo: VehiculoData?
)

data class VehiculoData(
    val placa: String,
    val modelo: String,
    val descripcion: String
)

data class EntradaResponse(
    val mensaje: String,
    val qrCode: String,      // Base64 o URL del QR retornado por backend
    val entradaId: String
)

data class SalidaRequest(
    val qrIdentificador: String,
    val fechaHoraSalida: String
)


data class BitacoraItem(
    @SerializedName("_id") val id: String,
    val visitanteId: String,
    val nombre: String,
    val fechaHoraIngreso: String,
    val fechaHoraSalida: String?,
    val tipoVisita: String,
    val placa: String?,
    val residenteRelacionado: String?
)

data class BitacoraResponse(
    val mensaje: String,
    val registros: List<BitacoraItem>
)


data class BitacoraMantenimiento(
    @SerializedName("_id") val id: String,
    val incidenciaAsociada: String,
    val tipoMantenimiento: String,
    val descripcion: String,
    val responsable: String,
    val fechaEjecucion: String,
    val fotosAntes: List<String>,   // URLs de las imágenes
    val fotosDespues: List<String>,
    val observaciones: String?
)

data class MantenimientoRequest(
    val incidenciaAsociada: String,
    val tipoMantenimiento: String,
    val descripcion: String,
    val responsable: String,
    val fechaEjecucion: String,
    val observaciones: String?
    // Las imágenes se enviarán por separado como Multipart
)

data class MantenimientoResponse(
    val mensaje: String,
    val mantenimiento: BitacoraMantenimiento
)

data class HistorialManteResponse(
    val mantenimientos: List<BitacoraMantenimiento>
)


// ===== REPORTES =====

data class ReporteBackend(
    @SerializedName("_id") val id: String,
    val tipo: String,
    val descripcion: String,
    val nivelPrioridad: String,
    val archivos: List<String>,
    val fecha: String,
    val estado: String,
    val seguimiento: String,
    val comentariosAdmin: String,
    val tecnicoAsignado: String,
    val residenteCorreo: String,
    val residenteNombre: String,
    val residenteApartamento: String?,
    val residenteIdentificacion: String?
)

data class CrearReporteRequest(
    val tipo: String,
    val descripcion: String,
    val nivelPrioridad: String,
    val archivos: List<String>,
    val fecha: String,
    val residenteCorreo: String,
    val residenteNombre: String,
    val residenteApartamento: String?,
    val residenteIdentificacion: String?
)

data class CrearReporteResponse(
    val mensaje: String,
    val reporte: ReporteBackend
)

data class ReportesListResponse(
    val reportes: List<ReporteBackend>
)

data class CambiarEstadoRequest(
    val estado: String,
    val comentariosAdmin: String,
    val identificacionTecnico: String? = null
)


// ===== RESERVAS =====

data class ReservaBackend(
    @SerializedName("_id") val id: String,
    val zona: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroPersonas: Int,
    val comentarios: String,
    val estado: String,  // "pendiente" o "aprobada"
    val residenteCorreo: String,
    val residenteNombre: String,
    val residenteApartamento: String,
    val residenteIdentificacion: String,
    val administradorQueResponde: String?,
    val fechaRespuesta: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ReservasListResponse(
    val reservas: List<ReservaBackend>
)

data class CrearReservaRequest(
    val zona: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroPersonas: Int,
    val comentarios: String,
    val residenteCorreo: String,
    val residenteNombre: String,
    val residenteApartamento: String,
    val residenteIdentificacion: String
)

data class CrearReservaResponse(
    val mensaje: String,
    val reserva: ReservaBackend
)

data class AprobarReservaRequest(
    val administradorCorreo: String
)

data class AprobarReservaResponse(
    val mensaje: String,
    val reserva: ReservaBackend
)

data class RechazarReservaRequest(
    val razonRechazo: String,
    val administradorCorreo: String
)

data class RechazarReservaResponse(
    val mensaje: String,
    val razonRechazo: String
)

data class ActualizarReservaRequest(
    val zona: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroPersonas: Int,
    val comentarios: String
)

data class ActualizarReservaResponse(
    val mensaje: String,
    val reserva: ReservaBackend
)
