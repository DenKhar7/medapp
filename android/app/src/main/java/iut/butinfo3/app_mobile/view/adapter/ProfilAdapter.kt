package iut.butinfo3.app_mobile.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.ItemProfilBinding
import iut.butinfo3.app_mobile.model.entity.User

/**
 * Adaptateur pour l'affichage de la liste des profils (Gestion des comptes).
 *
 * Utilise [ListAdapter] pour une gestion efficace des mises à jour de liste,
 * UserDiffCallBack() est placé dnas la classe ProfileSelectorAdapter, puisque les deux utilisé les mêmes classes.
 *
 * Cet adaptateur gère l'attribution dynamique d'une couleur de fond pour l'avatar
 * en fonction de la position de l'élément dans la liste (Cycle de 3 couleurs).
 *
 * @param onProfilClick Fonction de rappel (Callback) déclenchée quand l'utilisateur clique sur une carte profil.
 */
class ProfilAdapter(
    private val onProfilClick: (User) -> Unit
) : ListAdapter<User, ProfilAdapter.ViewHolder>(ProfileSelectorAdapter.UserDiffCallback()) {
    private val avatarColors = listOf(
        R.drawable.circle_background_purple,
        R.drawable.circle_background_blue,
        R.drawable.circle_background_green,
    )

    inner class ViewHolder(private val binding: ItemProfilBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Remplit les vues avec les données de l'utilisateur.
         *
         * @param user L'objet utilisateur à afficher.
         * @param position La position dans la liste (utilisée pour choisir la couleur).
         */
        fun bind(user: User,position: Int) {
            val context = binding.root.context
            binding.tvNomProfil.text = user.username.ifBlank { context.getString(R.string.profil_default_name) }
            binding.tvRelationProfil.text = user.email ?: context.getString(R.string.aucun_email)
            val colorRes = avatarColors[position%avatarColors.size]
            binding.ivProfilAvatar.setBackgroundResource(colorRes)

            binding.root.setOnClickListener {
                onProfilClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfilBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user,position)
    }


}