package Finanzas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.api.Pago
import com.example.resionemobile.databinding.ItemPagoBinding

class HistorialPagosAdapter(
    private var pagos: List<Pago>,
    private val onFacturaClick: (Pago) -> Unit
) : RecyclerView.Adapter<HistorialPagosAdapter.PagoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val binding = ItemPagoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        val pago = pagos[position]
        holder.bind(pago)
    }

    override fun getItemCount(): Int = pagos.size

    fun actualizarPagos(nuevosPagos: List<Pago>) {
        this.pagos = nuevosPagos
        notifyDataSetChanged()
    }

    inner class PagoViewHolder(private val binding: ItemPagoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pago: Pago) {
            binding.tvNombreResidente.text = pago.nombreResidente
            binding.tvUnidadHabitacional.text = "Unidad: ${pago.unidadHabitacional}"
            binding.tvFechaPago.text = "Pagado el: ${pago.fechaPago.substringBefore("T")}"
            binding.tvMetodoPago.text = "Método: ${pago.metodoPago}"
            binding.btnVerFactura.setOnClickListener {
                onFacturaClick(pago)
            }
        }
    }
}
