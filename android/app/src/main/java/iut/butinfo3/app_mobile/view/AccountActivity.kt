package iut.butinfo3.app_mobile.view

import android.Manifest
import android.app.AlarmManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.AccountActivityBinding
import iut.butinfo3.app_mobile.utils.LocaleHelper
import iut.butinfo3.app_mobile.utils.NotificationPreferences
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view.adapter.ProfilAdapter
import iut.butinfo3.app_mobile.view_model.AccountViewModel
import kotlinx.coroutines.launch


/**
 * Écran de gestion du compte utilisateur ("Mon Espace").
 *
 * Affiche :
 * 1. Les informations du compte principal (Email, Nom).
 * 2. La liste des profils secondaires (Enfants, Conjoint...) via un RecyclerView.
 *
 * Permet :
 * - De modifier son profil.
 * - D'ajouter des profils secondaires.
 * - De se déconnecter.
 */
class AccountActivity : AppCompatActivity() {
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory(this)
    }
    private lateinit var binding: AccountActivityBinding
    private var currentUserId: Int = -1

    override fun attachBaseContext(newBase: Context?) {
        val language = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "fr"
        super.attachBaseContext(newBase?.let { LocaleHelper.setLocale(it, language) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getIntExtra("EXTRA_USER_ID", -1)
        currentUserId=userId
        if (currentUserId != -1) {
            viewModel.loadAccountData(currentUserId)
        } else {
            Toast.makeText(this, "Erreur : Profil introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.recyclerProfils.layoutManager = LinearLayoutManager(this)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        updateLanguageDisplay()
    }

    private fun setupRecyclerView() {
        val profilAdapter = ProfilAdapter { clickedProfile ->
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("EXTRA_USER_ID", clickedProfile.id)
            startActivity(intent)
        }

        binding.recyclerProfils.layoutManager = LinearLayoutManager(this)
        binding.recyclerProfils.adapter = profilAdapter
    }

    /**
     * Observe les flux de données (Flows) du ViewModel.
     * Utilise 2 coroutines distinctes pour mettre à jour l'UI indépendamment.
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        if (user != null) {
                            binding.tvEmail.text = user.email
                            binding.tvNomPrincipal.text = user.username
                        }
                    }
                }
                launch {
                    viewModel.linkedUsers.collect { list ->
                        (binding.recyclerProfils.adapter as? ProfilAdapter)?.submitList(list)
                    }
                }
            }
        }
    }

    /**
     * Instancie les listeners de la classe
     * Pour le bouton deconnexion, cela va appeler lm méthode disconnect du viewModel qui va supprimer l'utilisateur actif,
     * Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK vont faire en sorte de nettoyer l'historique,
     * on ne pourra plus revenir dessus même si on slide vers la gauche.
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnDeconnexion.setOnClickListener {
            viewModel.onDisconnect()

            val intent = Intent(this, ConnectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.layoutProfilPrincipal.setOnClickListener {
            if (currentUserId != -1) {
                val intent = Intent(this, EditProfileActivity::class.java)
                intent.putExtra("EXTRA_USER_ID", currentUserId)
                startActivity(intent)
            }
        }

        binding.btnAjouterProfil.setOnClickListener {
            val intent = Intent(this, AddProfileActivity::class.java)
            intent.putExtra("PARENT_ID", currentUserId)
            startActivity(intent)
        }

        binding.layoutNotifications.setOnClickListener {
            checkAndRequestExactAlarmPermission()
        }

        binding.layoutLangue.setOnClickListener {
            showLanguageDialog()
        }
    }

    /**
     * Vérifie si les notifications/alarmes sont autorisées,
     * Vérifie si les permissions de notifs ont été demandées trop de fois
     * Demande les autorisations manquantes à l'utilisateur pour avoir des notifications
     */
    private fun checkAndRequestExactAlarmPermission() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }else{
            Toast.makeText(this, "Alarme déjà autorisé", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            val prefs = NotificationPreferences(this)
            if (notifPermission != PackageManager.PERMISSION_GRANTED) {
                when {
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                    }

                    !prefs.hasAskedPermission() -> {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                        prefs.setPermissionAsked()
                    }
                    else -> {
                        Toast.makeText(this, "Accès bloqué. Veuillez activer les notifications dans les paramètres.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Notifications déjà actives", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showLanguageDialog() {
        val dialog = Dialog(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_language, null)
        dialog.setContentView(dialogView)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val layoutFrench = dialogView.findViewById<LinearLayout>(R.id.layoutFrench)
        val layoutEnglish = dialogView.findViewById<LinearLayout>(R.id.layoutEnglish)
        val ivSelectedFrench = dialogView.findViewById<ImageView>(R.id.ivSelectedFrench)
        val ivSelectedEnglish = dialogView.findViewById<ImageView>(R.id.ivSelectedEnglish)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOkLanguage)

        // Variable pour suivre la sélection
        var selectedLanguage = LocaleHelper.getSavedLanguage(this)

        // Fonction pour mettre à jour l'affichage de la sélection
        fun updateSelection() {
            ivSelectedFrench.visibility = if (selectedLanguage == "fr") android.view.View.VISIBLE else android.view.View.GONE
            ivSelectedEnglish.visibility = if (selectedLanguage == "en") android.view.View.VISIBLE else android.view.View.GONE

            // Changer le background pour indiquer la sélection
            if (selectedLanguage == "fr") {
                layoutFrench.setBackgroundResource(R.drawable.language_option_selected)
                layoutEnglish.setBackgroundResource(R.drawable.language_option_background)
            } else {
                layoutFrench.setBackgroundResource(R.drawable.language_option_background)
                layoutEnglish.setBackgroundResource(R.drawable.language_option_selected)
            }
        }

        // Initialiser la sélection
        updateSelection()

        // Gérer les clics sur les options
        layoutFrench.setOnClickListener {
            selectedLanguage = "fr"
            updateSelection()
        }

        layoutEnglish.setOnClickListener {
            selectedLanguage = "en"
            updateSelection()
        }

        btnOk.setOnClickListener {
            LocaleHelper.saveLanguage(this, selectedLanguage)

            // Redémarrer l'activité pour appliquer la nouvelle langue
            recreate()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateLanguageDisplay() {
        val currentLanguage = LocaleHelper.getSavedLanguage(this)
        binding.tvLangue.text = LocaleHelper.getLanguageDisplayName(currentLanguage)
    }
}