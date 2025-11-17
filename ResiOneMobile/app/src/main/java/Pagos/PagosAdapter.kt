package Pagos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R

class PagosAdapter(
    private val pagos: MutableList<Pago>,
    private val onGeneratePdfClick: (Pago) -> Unit
) : RecyclerView.Adapter<PagosAdapter.PagoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pago, parent, false)
        return PagoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        val pago = pagos[position]
        holder.bind(pago, onGeneratePdfClick)
    }

    override fun getItemCount() = pagos.size

    fun updatePagos(newPagos: List<Pago>) {
        pagos.clear()
        pagos.addAll(newPagos)
        notifyDataSetChanged()
    }

    class PagoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreResidente: TextView = itemView.findViewById(R.id.tv_nombre_residente)
        private val tvMontoPagado: TextView = itemView.findViewById(R.id.tv_monto_pagado)
        private val tvFechaPago: TextView = itemView.findViewById(R.id.tv_fecha_pago)
        private val tvOpcionPago: TextView = itemView.findViewById(R.id.tv_opcion_pago)
        private val btnGeneratePdf: Button = itemView.findViewById(R.id.btn_generate_pdf)

        fun bind(pago: Pago, onGeneratePdfClick: (Pago) -> Unit) {
            tvNombreResidente.text = "Residente: ${pago.nombreCompleto}"
            tvMontoPagado.text = "Monto: Consultar API de Cuotas"
            tvFechaPago.text = "Fecha: ${pago.fecha}"
            tvOpcionPago.text = "Opción: ${pago.opcionPago}"
            btnGeneratePdf.setOnClickListener { onGeneratePdfClick(pago) }
        }
    }
}