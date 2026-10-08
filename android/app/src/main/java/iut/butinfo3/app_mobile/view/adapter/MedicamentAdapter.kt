package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.ItemDrugBinding
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail


/**
 * Adaptateur pour la liste des médicaments (Résultats de recherche / Scan).
 *
 * Technologie : ListAdapter
 * Cet adaptateur hérite de [ListAdapter] (et non de RecyclerView.Adapter).
 * Cela permet une gestion optimisée des mises à jour de liste grâce à [DiffUtil].
 * Plus besoin de `notifyDataSetChanged()` : on utilise `submitList(nouvelleListe)`.
 */
class MedicamentAdapter(
    private val onItemClick: (MedicamentPresDetail) -> Unit,
    private val onAddTreatment: (MedicamentPresDetail) -> Unit
) : ListAdapter<MedicamentPresDetail, MedicamentAdapter.MedicamentViewHolder>(MedicamentDiffCallback()) {

    private val expandedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentViewHolder {
        val binding = ItemDrugBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicamentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicamentViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, expandedIds.contains(item.cip13))
    }

    /**
     * Gère la logique d'ouverture/fermeture d'un item.
     * Met à jour le Set des IDs étendus et notifie l'adaptateur pour rafraîchir l'affichage.
     */
    private fun toggleExpansion(id: String) {
        if (expandedIds.contains(id)) {
            expandedIds.remove(id)
        } else {
            expandedIds.add(id)
        }

        val position = currentList.indexOfFirst { it.cip13 == id }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    /**
     * ViewHolder : "La boîte" qui contient les vues d'une ligne.
     */
    inner class MedicamentViewHolder(private val binding: ItemDrugBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MedicamentPresDetail, isExpanded: Boolean) {
            val context = binding.root.context

            binding.tvName.text = item.nomSpecialite
            binding.tvSubstanceActive.text = item.libelleAtc ?: context.getString(R.string.unknown_substance)

            if (isExpanded) {
                binding.expandedLayout.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.ivArrow.rotation = 90f

                binding.tvClasse.text = item.codeAtc ?: context.getString(R.string.unknown_atc)
                binding.tvVoies.text = item.voie.joinToString(", ")

                binding.tvPresentation.text = context.getString(
                    R.string.format_presentation_full,
                    item.label,
                    item.cip13,
                    item.voie.joinToString(", ")
                )

            } else {
                binding.expandedLayout.visibility = View.GONE
                binding.divider.visibility = View.GONE
                binding.ivArrow.rotation = 0f
            }

            binding.headerLayout.setOnClickListener {
                toggleExpansion(item.cip13)
                onItemClick(item)
            }

            binding.btnAddTreatment.setOnClickListener {
                onAddTreatment(item)
            }
        }
    }
    /**
     * Le Comparateur Intelligent (DiffUtil).
     * C'est le cerveau du ListAdapter. Il permet de calculer les différences entre deux listes
     * pour ne mettre à jour que le strict nécessaire.
     */
    class MedicamentDiffCallback : DiffUtil.ItemCallback<MedicamentPresDetail>() {
        override fun areItemsTheSame(oldItem: MedicamentPresDetail, newItem: MedicamentPresDetail): Boolean {
            return oldItem.cip13 == newItem.cip13
        }

        override fun areContentsTheSame(oldItem: MedicamentPresDetail, newItem: MedicamentPresDetail): Boolean {
            return oldItem == newItem
        }
    }
}