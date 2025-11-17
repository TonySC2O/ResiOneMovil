package Pagos

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R

class CuotasAdapter(
    private val cuotas: MutableList<Cuota>,
    private val onItemClick: (Cuota) -> Unit
) : RecyclerView.Adapter<CuotasAdapter.CuotaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuota, parent, false)
        return CuotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuotaViewHolder, position: Int) {
        val cuota = cuotas[position]
        holder.bind(cuota)
        holder.itemView.setOnClickListener {
            if (cuota.estado == EstadoPago.PENDIENTE || cuota.estado == EstadoPago.ATRASADO) {
                onItemClick(cuota)
            }
        }
    }

    override fun getItemCount() = cuotas.size

    fun updateCuotas(newCuotas: List<Cuota>) {
        cuotas.clear()
        cuotas.addAll(newCuotas)
        notifyDataSetChanged()
    }

    class CuotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonto: TextView = itemView.findViewById(R.id.tv_monto)
        private val tvFechaVencimiento: TextView = itemView.findViewById(R.id.tv_fecha_vencimiento)
        private val tvUnidadHabitacional: TextView = itemView.findViewById(R.id.tv_unidad_habitacional)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)

        fun bind(cuota: Cuota) {
            tvMonto.text = "Monto: ${cuota.monto}"
            tvFechaVencimiento.text = "Vencimiento: ${cuota.fechaVencimiento}"
            tvUnidadHabitacional.text = "Unidad: ${cuota.unidadHabitacional}"
            tvEstado.text = "Estado: ${cuota.estado}"
        }
    }
}