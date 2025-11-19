package com.example.resionemobile.mantenimiento

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import com.example.resionemobile.api.BitacoraMantenimiento

class HistorialManteAdapter(private var mantenimientos: List<BitacoraMantenimiento>) : RecyclerView.Adapter<HistorialManteAdapter.ViewHolder>() {

    fun updateData(newMantenimientos: List<BitacoraMantenimiento>) {
        this.mantenimientos = newMantenimientos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mantenimiento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mantenimientos[position])
    }

    override fun getItemCount(): Int = mantenimientos.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tipo: TextView = itemView.findViewById(R.id.tv_mantenimiento_tipo)
        private val fecha: TextView = itemView.findViewById(R.id.tv_mantenimiento_fecha)
        private val descripcion: TextView = itemView.findViewById(R.id.tv_mantenimiento_descripcion)
        private val responsable: TextView = itemView.findViewById(R.id.tv_mantenimiento_responsable)

        fun bind(mantenimiento: BitacoraMantenimiento) {
            tipo.text = mantenimiento.tipoMantenimiento
            fecha.text = mantenimiento.fechaEjecucion
            descripcion.text = mantenimiento.descripcion
            responsable.text = "Responsable: ${mantenimiento.responsable}"

            // TODO: Añadir un OnClickListener para ver el detalle completo
        }
    }
}