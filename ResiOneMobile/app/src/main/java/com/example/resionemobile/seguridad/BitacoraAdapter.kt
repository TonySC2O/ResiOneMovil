package com.example.resionemobile.seguridad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import com.example.resionemobile.api.BitacoraItem

class BitacoraAdapter(private var registros: List<BitacoraItem>) : RecyclerView.Adapter<BitacoraAdapter.ViewHolder>() {

    fun updateData(newRegistros: List<BitacoraItem>) {
        this.registros = newRegistros
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bitacora, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val registro = registros[position]
        holder.bind(registro)
    }

    override fun getItemCount(): Int = registros.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreVisitante: TextView = itemView.findViewById(R.id.tvNombreVisitante)
        private val tvIdVisitante: TextView = itemView.findViewById(R.id.tvIdVisitante)
        private val tvFechaIngreso: TextView = itemView.findViewById(R.id.tvFechaIngreso)
        private val tvFechaSalida: TextView = itemView.findViewById(R.id.tvFechaSalida)

        fun bind(registro: BitacoraItem) {
            tvNombreVisitante.text = registro.nombre
            tvIdVisitante.text = "ID: ${registro.visitanteId}"
            tvFechaIngreso.text = "Ingreso: ${registro.fechaHoraIngreso}"
            tvFechaSalida.text = if(registro.fechaHoraSalida.isNullOrEmpty()) "Salida: --" else "Salida: ${registro.fechaHoraSalida}"
        }
    }
}
