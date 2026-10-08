package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import java.text.SimpleDateFormat
import java.util.Locale

class OrdonnanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvOrdonnanceName: TextView = itemView.findViewById(R.id.tvOrdonnanceName)
    val tvOrdonnanceDate: TextView = itemView.findViewById(R.id.tvOrdonnanceDate)
    val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
    val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
    val layoutAddress: View = itemView.findViewById(R.id.layoutAddress)
    val layoutNotes: View = itemView.findViewById(R.id.layoutNotes)
    val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
    val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    val cardView: CardView = itemView as CardView
}

class OrdonnanceAdapter(
    private var ordonnances: List<Ordonnance>,
    private val onEditClick: (Ordonnance) -> Unit,
    private val onDeleteClick: (Ordonnance) -> Unit
) : RecyclerView.Adapter<OrdonnanceViewHolder>() {

    fun submitList(newList: List<Ordonnance>) {
        ordonnances = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdonnanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ordonnance, parent, false)
        return OrdonnanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdonnanceViewHolder, position: Int) {
        val ordonnance = ordonnances[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Mettre nom medecin
        holder.tvOrdonnanceName.text = ordonnance.doctorName ?: "Médecin non spécifié"

        // Mettre date scan en sous titre
        holder.tvOrdonnanceDate.text = "Scanné le ${dateFormat.format(ordonnance.scanDate)}"

        // Mettre date prescription avec icone
        val prescriptionDateText = ordonnance.prescriptionDate?.let {
            "Ordonnance du ${dateFormat.format(it)}"
        } ?: "Date de prescription non spécifiée"
        holder.tvDate.text = prescriptionDateText

        // Afficher ou masquer adresse
        if (!ordonnance.doctorAddress.isNullOrBlank()) {
            holder.layoutAddress.visibility = View.VISIBLE
            holder.tvAddress.text = ordonnance.doctorAddress
        } else {
            holder.layoutAddress.visibility = View.GONE
        }

        // Afficher ou masquer notes
        if (!ordonnance.notes.isNullOrBlank()) {
            holder.layoutNotes.visibility = View.VISIBLE
            holder.tvNotes.text = ordonnance.notes
        } else {
            holder.layoutNotes.visibility = View.GONE
        }

        // Bouton edition
        holder.btnEdit.setOnClickListener {
            onEditClick(ordonnance)
        }

        // Bouton suppression
        holder.btnDelete.setOnClickListener {
            onDeleteClick(ordonnance)
        }
    }

    override fun getItemCount(): Int = ordonnances.size
}