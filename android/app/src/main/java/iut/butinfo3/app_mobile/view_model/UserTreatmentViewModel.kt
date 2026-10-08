package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.TreatmentWithMedicament
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran "Mes Traitements".
 *
 * Gère l'affichage de la liste des traitements de l'utilisateur actif
 * et les actions de suppression.
 */
class UserTreatmentViewModel(
    private val repository: TreatmentRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UserTreatmentState>(UserTreatmentState.Idle)
    val uiState = _uiState.asStateFlow()
    private val _treatments = MutableStateFlow<List<TreatmentWithMedicament>>(emptyList())
    val treatments = _treatments.asStateFlow()

    private var treatmentsJob: Job? = null

    init {
        observeSession()
    }

    /**
     * Surveille le SessionRepository.
     * Dès que l'utilisateur change (via le bandeau en haut), on recharge la liste correspondante.
     */
    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.selectedUser.collect { user ->
                treatmentsJob?.cancel()

                if (user != null) {
                    treatmentsJob = viewModelScope.launch {
                        repository.getUserTreatments(user.id).collect { list ->
                            _treatments.value = list
                        }
                    }
                } else {
                    _treatments.value = emptyList()
                }
            }
        }
    }

    /**
     * Supprime un traitement et ses rappels associés.
     * Met à jour l'état UI pour déclencher un feedback visuel (Toast).
     */
    fun deleteTreatment(item: TreatmentWithMedicament) {
        viewModelScope.launch {
            _uiState.value = UserTreatmentState.Loading
            try {
                repository.deleteTreatment(item.treatment)
                _uiState.value = UserTreatmentState.DeleteSuccess
            } catch (_: Exception) {
                _uiState.value = UserTreatmentState.Error("Erreur lors de la suppression")
            }
        }
    }

    /**
     * Réinitialise l'état après que l'UI a consommé l'événement (ex: après affichage du Toast).
     * Empêche le Toast de réapparaître lors d'une rotation d'écran.
     */
    fun resetState() {
        _uiState.value = UserTreatmentState.Idle
    }
}

/**
 * États possibles pour les actions de cet écran.
 */
sealed class UserTreatmentState {
    object Idle : UserTreatmentState()
    object Loading : UserTreatmentState()
    object DeleteSuccess : UserTreatmentState()
    data class Error(val message: String) : UserTreatmentState()
}