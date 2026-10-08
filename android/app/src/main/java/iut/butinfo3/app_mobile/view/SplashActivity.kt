package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.view_model.SessionState
import iut.butinfo3.app_mobile.view_model.SplashViewModel
import kotlinx.coroutines.launch

/**
 * Écran de démarrage (Splash Screen).
 *
 * Premier point d'entrée de l'application (LAUNCHER).
 * Vérifie l'existence d'une session utilisateur valide et redirige automatiquement :
 * - Vers HomeActivity si session valide (auto-login)
 * - Vers ConnectionActivity si pas de session ou session invalide
 *
 * Pas de layout - utilise uniquement le thème splash pour affichage instantané.
 */
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels {
        ViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pas de setContentView - le thème affiche directement le fond

        observeSessionState()
    }

    /**
     * Observe l'état de la session et navigue en conséquence.
     */
    private fun observeSessionState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { state ->
                    when (state) {
                        is SessionState.Checking -> {
                            // Affiche le logo du thème pendant la vérification
                        }
                        is SessionState.ValidSession -> {
                            navigateToHome()
                        }
                        is SessionState.NoSession,
                        is SessionState.InvalidSession -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    /**
     * Redirige vers l'accueil (auto-login réussi).
     * Clear la pile d'activités pour empêcher le retour au splash.
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Redirige vers l'écran de connexion.
     * Clear la pile d'activités pour empêcher le retour au splash.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, ConnectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
