package iut.butinfo3.app_mobile.view.adapter

import android.graphics.Paint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import iut.butinfo3.app_mobile.databinding.ItemMedicamentJourBinding
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.entity.IntakeStatus


/**
 * Adaptateur pour la liste des médicaments du jour (Écran d'Accueil).
 *
 * Affiche une liste de [DailyIntake] avec une case à cocher.
 * Gère l'aspect visuel (barré/grisé) quand le médicament est pris.
 * Supporte 3 modes : TODAY (checkbox interactive), PAST (statut lecture seule), FUTURE (info seule).
 *
 * @param onCheckedChange Callback appelé quand l'utilisateur coche/décoche une case.
 * Renvoie l'objet modifié et le nouvel état booléen pour mettre à jour la db.
 */
class MedicamentsDuJourAdapter(
    private val onCheckedChange: (DailyIntake, Boolean) -> Unit
) : ListAdapter<DailyIntake, MedicamentsDuJourAdapter.ViewHolder>(DiffCallback()) {

    enum class DayMode { TODAY, PAST, FUTURE }

    private var dayMode: DayMode = DayMode.TODAY

    fun setDayMode(mode: DayMode) {
        if (dayMode != mode) {
            dayMode = mode
            notifyDataSetChanged() // Force le rebind de tous les items
        }
    }

    inner class ViewHolder(private val binding: ItemMedicamentJourBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyIntake) {
            binding.tvMedicamentName.text = item.medicamentName
            binding.tvTime.text = item.time
            binding.tvDosage.text = "Qte: ${item.dosage}"

            binding.cbMedicament.setOnCheckedChangeListener(null)

            when (dayMode) {
                DayMode.TODAY -> bindToday(item)
                DayMode.PAST -> bindPast(item)
                DayMode.FUTURE -> bindFuture()
            }
        }

        private fun bindToday(item: DailyIntake) {
            binding.cbMedicament.visibility = View.VISIBLE
            binding.tvStatus.visibility = View.GONE
            binding.cbMedicament.isEnabled = true
            binding.cbMedicament.isChecked = item.isTaken
            binding.tvMedicamentName.setTextColor(0xFF000000.toInt())

            updateStrikeThrough(item.isTaken)

            binding.cbMedicament.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(isChecked)
                onCheckedChange(item, isChecked)
            }

            binding.root.setOnClickListener {
                binding.cbMedicament.isChecked = !binding.cbMedicament.isChecked
            }
        }

        private fun bindPast(item: DailyIntake) {
            binding.cbMedicament.visibility = View.GONE
            binding.tvStatus.visibility = View.VISIBLE

            val isTaken = item.intakeStatus == IntakeStatus.TAKEN
            updateStrikeThrough(isTaken)

            when (item.intakeStatus) {
                IntakeStatus.TAKEN -> {
                    binding.tvStatus.text = "Pris"
                    binding.tvStatus.setTextColor(0xFF4CAF50.toInt())
                    binding.root.alpha = 0.5f
                    binding.tvMedicamentName.setTextColor(0xFF000000.toInt())
                }
                IntakeStatus.SKIPPED -> {
                    binding.tvStatus.text = "Ignoré"
                    binding.tvStatus.setTextColor(0xFF9E9E9E.toInt())
                    binding.root.alpha = 0.5f
                }
                IntakeStatus.MISSED, null -> {
                    binding.tvStatus.text = "Manqué"
                    binding.tvStatus.setTextColor(0xFFF44336.toInt())
                    binding.root.alpha = 0.7f
                    binding.tvMedicamentName.setTextColor(0xFFF44336.toInt())
                }
            }

            binding.root.setOnClickListener(null)
        }

        private fun bindFuture() {
            binding.cbMedicament.visibility = View.GONE
            binding.tvStatus.visibility = View.GONE

            binding.tvMedicamentName.paintFlags = binding.tvMedicamentName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvMedicamentName.setTextColor(0xFF000000.toInt())
            binding.root.alpha = 1.0f
            binding.root.setOnClickListener(null)
        }

        private fun updateStrikeThrough(isTaken: Boolean) {
            if (isTaken) {
                binding.tvMedicamentName.paintFlags = binding.tvMedicamentName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.5f
            } else {
                binding.tvMedicamentName.paintFlags = binding.tvMedicamentName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicamentJourBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyIntake>() {
        override fun areItemsTheSame(oldItem: DailyIntake, newItem: DailyIntake) =
            oldItem.reminderId == newItem.reminderId

        override fun areContentsTheSame(oldItem: DailyIntake, newItem: DailyIntake) =
            oldItem == newItem
    }
}