package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // IMPORTANT
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import java.util.Date

enum class MedicationStatus {
    ALL_TAKEN, PARTIAL, NONE, NO_MEDICATION
}

data class CalendarDay(
    val dayNumber: Int?,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val hasReminder: Boolean = false,
    val isCurrentMonth: Boolean = true,
    val medicationStatus: MedicationStatus = MedicationStatus.NO_MEDICATION,
    val isPast: Boolean = false,
    val date: Date? = null
)

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.CalendarDayViewHolder>(CalendarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val viewIndicator: View = itemView.findViewById(R.id.viewIndicator)
        private val cardView: CardView = itemView.findViewById(R.id.cardView)

        fun bind(day: CalendarDay) {
            if (day.dayNumber != null) {
                setupVisibleDay(day)
            } else {
                setupEmptyDay()
            }
        }

        private fun setupEmptyDay() {
            tvDayNumber.visibility = View.INVISIBLE
            viewIndicator.visibility = View.GONE
            cardView.setCardBackgroundColor(0x00FFFFFF.toInt()) // Transparent
            cardView.elevation = 0f
            itemView.setOnClickListener(null)
        }

        private fun setupVisibleDay(day: CalendarDay) {
            tvDayNumber.text = day.dayNumber.toString()
            tvDayNumber.visibility = View.VISIBLE

            applyColors(day)
            viewIndicator.visibility = if (day.hasReminder) View.VISIBLE else View.GONE
            itemView.setOnClickListener {
                if (day.isCurrentMonth) onDayClick(day)
            }
        }

        private fun applyColors(day: CalendarDay) {
            // Texte
            val textColor = when {
                !day.isCurrentMonth -> 0x4D999999.toInt() // Gris
                day.isSelected || day.isToday -> 0xFFFFFFFF.toInt() // Blanc
                else -> 0xFF1A1A1A.toInt() // Noir
            }
            tvDayNumber.setTextColor(textColor)

            // Fond
            cardView.alpha = 1f
            cardView.elevation = 0f

            val backgroundColor = when {
                day.isSelected -> {
                    cardView.elevation = 4f
                    0xFF8B8BFB.toInt() // Violet
                }
                day.isToday -> {
                    cardView.alpha = 0.9f
                    cardView.elevation = 2f
                    0xFF8B8BFB.toInt() // Violet
                }
                day.isPast -> when (day.medicationStatus) {
                    MedicationStatus.ALL_TAKEN -> 0xFF2196F3.toInt() // Bleu
                    MedicationStatus.PARTIAL, MedicationStatus.NONE -> 0xFF9E9E9E.toInt() // Gris
                    else -> 0xFFFFFFFF.toInt() // Blanc
                }
                else -> {
                    if (day.isToday && day.medicationStatus == MedicationStatus.ALL_TAKEN) {
                        0xFF4CAF50.toInt() // Vert
                    } else {
                        0xFFFFFFFF.toInt() // Blanc
                    }
                }
            }
            cardView.setCardBackgroundColor(backgroundColor)
        }
    }

    class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.dayNumber == newItem.dayNumber && oldItem.isCurrentMonth == newItem.isCurrentMonth
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}