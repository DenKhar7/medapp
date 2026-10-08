package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.databinding.ItemUserTreatmentBinding
import iut.butinfo3.app_mobile.model.entity.TreatmentWithMedicament
import java.util.Locale

/**
 * Adaptateur pour la liste des traitements actifs de l'utilisateur.
 *
 * Affiche une carte complexe avec le nom du médicament, les dates et des boutons d'action.
 * Utilise des Lambdas pour remonter les événements de clic vers l'Activité.
 *
 * @param onDetailClick Fonction appelée au clic sur la carte (pour voir les détails complets).
 * @param onEditClick Fonction appelée au clic sur le bouton "Crayon" (Modifier).
 * @param onDeleteClick Fonction appelée au clic sur le bouton "Poubelle" (Supprimer).
 */
class UserTreatmentAdapter(
    private val onDetailClick: (TreatmentWithMedicament) -> Unit,
    private val onEditClick: (TreatmentWithMedicament) -> Unit,
    private val onDeleteClick: (TreatmentWithMedicament) -> Unit
) : ListAdapter<TreatmentWithMedicament,UserTreatmentAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemUserTreatmentBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Remplit les vues avec les données de l'utilisateur.
         *
         * @param TreatmentWithMedicament L'objet traitement avec medicament à afficher.
         */
        fun bind(item: TreatmentWithMedicament) {

            binding.tvName.text = item.medicament.nomSpecialite
            binding.tvSubstanceActive.text = item.medicament.libelleAtc

            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val debut = dateFormat.format(item.treatment.startDate)
            val fin = item.treatment.endDate ?: "Indéterminé"

            binding.tvDates.text = "Du $debut au $fin"

            if (!item.treatment.instructions.isNullOrBlank()) {
                binding.tvInstructions.text = item.treatment.instructions
                binding.tvInstructions.visibility = android.view.View.VISIBLE
            } else {
                binding.tvInstructions.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onDetailClick(item) }
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserTreatmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    /**
     * Comparateur pour optimiser le rafraîchissement de la liste.
     */
    class DiffCallback : DiffUtil.ItemCallback<TreatmentWithMedicament>() {
        override fun areItemsTheSame(oldItem: TreatmentWithMedicament, newItem: TreatmentWithMedicament) =
            oldItem.treatment.id == newItem.treatment.id

        override fun areContentsTheSame(oldItem: TreatmentWithMedicament, newItem: TreatmentWithMedicament) =
            oldItem == newItem
    }
}