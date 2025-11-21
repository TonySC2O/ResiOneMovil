package com.example.resionemobile.Reservas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.resionemobile.R
import java.text.SimpleDateFormat
import java.util.*

class CalendarMonthAdapter(
    private var days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarMonthAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.day_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day_item, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.text.text = day.dayNumber

        // default styles
        holder.text.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.day_background)

        // dim days not in month
        if (!day.inMonth) {
            holder.text.alpha = 0.35f
        } else {
            holder.text.alpha = 1.0f
        }

        // color by reservation status
        when (day.status) {
            ReservaStatus.NONE -> {
                // no-op
            }
            ReservaStatus.PENDING -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.teal_700))
                holder.text.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
            ReservaStatus.COMPLETED -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light))
                holder.text.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            }
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}

data class CalendarDay(
    val date: Date,
    val dayNumber: String,
    val inMonth: Boolean,
    var status: ReservaStatus = ReservaStatus.NONE
)

enum class ReservaStatus { NONE, PENDING, COMPLETED }

object CalendarUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun toKey(date: Date): String = sdf.format(date)

    fun startOfMonth(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal
    }
}
