package iut.butinfo3.app_mobile.view.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.ItemProfileAvatarBinding
import iut.butinfo3.app_mobile.model.entity.User

/**
 * Adaptateur pour le "Sélecteur de Profil" (Bandeau horizontal des têtes).
 *
 * Afficher les avatars des utilisateurs disponibles et mettre en surbrillance
 * celui qui est actuellement connecté.
 *
 * Cet adaptateur gère deux états d'affichage pour chaque item :
 * Sélectionné :Opaque, Gras, Fond blanc.
 * Non-sélectionné : Semi-transparent (Alpha 0.6), Normal, Fond bleu.
 *
 */
class ProfileSelectorAdapter(
    private val onProfileClick: (User) -> Unit
) : ListAdapter<User, ProfileSelectorAdapter.ViewHolder>(UserDiffCallback()) {

    var selectedUserId: Int = -1

    inner class ViewHolder(val binding: ItemProfileAvatarBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
        * Remplit les vues avec les données de l'utilisateur.
        *
        * @param user L'objet utilisateur à afficher.
        */
        fun bind(user: User) {
            binding.tvInitial.text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?"
            binding.tvFirstName.text = user.username

            if (user.id == selectedUserId) {
                binding.root.alpha = 1.0f
                binding.tvFirstName.setTypeface(null, Typeface.BOLD)

                binding.circleBackground.setBackgroundResource(R.drawable.bg_circle_white)
                binding.tvInitial.setTextColor("#8B8BFB".toColorInt())
            } else {
                binding.root.alpha = 0.6f
                binding.tvFirstName.setTypeface(null, Typeface.NORMAL)

                binding.circleBackground.setBackgroundResource(R.drawable.circle_background_blue)
                binding.tvInitial.setTextColor(Color.WHITE)
            }

            binding.root.setOnClickListener { onProfileClick(user) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfileAvatarBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder((binding))
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}