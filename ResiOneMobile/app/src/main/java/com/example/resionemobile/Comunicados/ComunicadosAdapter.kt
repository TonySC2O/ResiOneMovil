package com.example.resionemobile.Comunicados

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import com.example.resionemobile.api.Comunicado

class ComunicadosAdapter(
    private val items: MutableList<Comunicado>,
    private val onEdit: (Comunicado) -> Unit,
    private val onDelete: (Comunicado) -> Unit
) : RecyclerView.Adapter<ComunicadosAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tv_title)
        val tvContenido: TextView = view.findViewById(R.id.tv_content)
        val tvFecha: TextView = view.findViewById(R.id.tv_date)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.tvTitulo.text = c.titulo ?: "Sin título"
        holder.tvContenido.text = c.contenido
        holder.tvFecha.text = c.fechaPublicacion ?: ""
        holder.btnEdit.setOnClickListener { onEdit(c) }
        holder.btnDelete.setOnClickListener { onDelete(c) }
    }

    override fun getItemCount() = items.size

    fun updateComunicados(list: List<Comunicado>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addComunicado(c: Comunicado) {
        items.add(0, c)
        notifyItemInserted(0)
    }

    fun removeComunicado(c: Comunicado) {
        val index = items.indexOfFirst { it.id == c.id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateSingle(updated: Comunicado) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            items[index] = updated
            notifyItemChanged(index)
        }
    }
}