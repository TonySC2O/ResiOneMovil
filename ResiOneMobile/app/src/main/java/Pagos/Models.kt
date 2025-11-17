package Pagos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class EstadoPago {
    PENDIENTE,
    CANCELADO,
    ATRASADO
}

@Parcelize
data class Cuota(
    val id: String,
    val monto: Double,
    val fechaVencimiento: String,
    val unidadHabitacional: String,
    val residenteId: String,
    val estado: EstadoPago
) : Parcelable

enum class OpcionPago {
    EFECTIVO,
    TRANSFERENCIA
}

data class Pago(
    val id: String,
    val cuotaId: String,
    val residenteId: String,
    val nombreCompleto: String,
    val unidadHabitacional: String,
    val fecha: String,
    val opcionPago: OpcionPago,
    val comprobanteUrl: String? = null // Para PDF
)

data class CuotaListResponse(val cuotas: List<Cuota>)
data class GenericCuotaResponse(val cuota: Cuota)
data class PagoListResponse(val pagos: List<Pago>)
data class GenericPagoResponse(val pago: Pago)
