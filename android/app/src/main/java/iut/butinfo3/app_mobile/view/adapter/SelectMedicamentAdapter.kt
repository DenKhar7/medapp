package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume

class SelectMedicamentAdapter(
    private var items: List<MedicamentSpeResume>,
    private val onItemSelected: (cis: Int, label: String) -> Unit
) : RecyclerView.Adapter<SelectMedicamentAdapter.ViewHolder>() {

    fun submitList(newItems: List<MedicamentSpeResume>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drug, parent, false)
        return ViewHolder(view, onItemSelected)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        itemView: View,
        private val onItemSelected: (cis: Int, label: String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSub: TextView = itemView.findViewById(R.id.tvSubstanceActive)
        private val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val expandedLayout: View = itemView.findViewById(R.id.expandedLayout)

        fun bind(item: MedicamentSpeResume) {
            tvName.text = item.nom
            tvSub.text = item.codeAtc ?: "Code ATC non disponible"

            // Mode sélection: on désactive l'UI "expand" du layout réutilisé
            ivArrow.visibility = View.GONE
            divider.visibility = View.GONE
            expandedLayout.visibility = View.GONE

            cardView.setOnClickListener {
                onItemSelected(item.cis, item.nom)
            }
        }
    }
}
