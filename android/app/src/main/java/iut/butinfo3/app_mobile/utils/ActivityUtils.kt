package iut.butinfo3.app_mobile.utils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.view.AccountActivity
import iut.butinfo3.app_mobile.view.adapter.ProfileSelectorAdapter
import iut.butinfo3.app_mobile.view_model.MainViewModel
import kotlinx.coroutines.launch


/**
 * Utilisé pour configurer et animer le bandeau de sélection des profiles dans les classes homeActivity
 * et UserTreatmentsActivity.
 *
 * C'est une Fonction d'Extension : cela permet d'ajouter
 * cette fonctionnalité à n'importe quelle activité sans dupliquer le code ou avoir à instacier une classe "Utils".
 *
 * Si on clique sur un profil cela va le sélectionner et notifier l'app que l'utilisateur "sélectionné" à été mis à jour
 * Si on clique sur un profil déjà sélectionné cela va affiche rle compte de l'utilisateur sélectionné.
 */

fun AppCompatActivity.setupProfileSelector(
    recyclerView: RecyclerView,
    mainViewModel: MainViewModel,
    onProfileClick: (User) -> Unit = {}
) {
    val adapter = ProfileSelectorAdapter { clickedUser ->
        val currentSelectedId = mainViewModel.selectedUser.value?.id

        if (clickedUser.id == currentSelectedId) {
            val intent = Intent(this, AccountActivity::class.java)
            intent.putExtra("EXTRA_USER_ID", mainViewModel.parentUserId)
            startActivity(intent)
        } else {
            mainViewModel.selectUser(clickedUser)
        }
        onProfileClick(clickedUser)
    }

    recyclerView.adapter = adapter
    recyclerView.layoutManager =
        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                mainViewModel.availableProfiles.collect { profiles ->
                    adapter.submitList(profiles)
                }
            }
            launch {
                mainViewModel.selectedUser.collect { user ->
                    if (user != null) {
                        adapter.selectedUserId = user.id
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}
