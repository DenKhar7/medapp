package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import iut.butinfo3.app_mobile.model.repository.OrdonnanceRepository
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdonnanceViewModel(
    private val repository: OrdonnanceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _ordonnances = MutableStateFlow<List<Ordonnance>>(emptyList())
    val ordonnances: StateFlow<List<Ordonnance>> = _ordonnances.asStateFlow()

    private val _currentOrdonnance = MutableStateFlow<Ordonnance?>(null)
    val currentOrdonnance: StateFlow<Ordonnance?> = _currentOrdonnance.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun observeOrdonnancesForUser(userId: Int) {
        viewModelScope.launch {
            repository.getOrdonnancesByUser(userId).collect { list ->
                _ordonnances.value = list
            }
        }
    }

    fun loadOrdonnance(ordonnanceId: Int) {
        viewModelScope.launch {
            try {
                val ordonnance = repository.getOrdonnanceById(ordonnanceId)
                _currentOrdonnance.value = ordonnance
                if (ordonnance == null) {
                    _error.value = "Ordonnance introuvable"
                }
            } catch (e: Exception) {
                _error.value = "Erreur lors du chargement: ${e.message}"
            }
        }
    }

    fun addOrdonnance(ordonnance: Ordonnance) {
        viewModelScope.launch {
            try {
                repository.insertOrdonnance(ordonnance)
                _operationSuccess.value = "saved"
            } catch (e: Exception) {
                _error.value = "Erreur lors de l'ajout: ${e.message}"
            }
        }
    }

    fun updateOrdonnance(ordonnance: Ordonnance) {
        viewModelScope.launch {
            try {
                repository.updateOrdonnance(ordonnance)
                _operationSuccess.value = "updated"
            } catch (e: Exception) {
                _error.value = "Erreur lors de la mise à jour: ${e.message}"
            }
        }
    }

    fun deleteOrdonnance(ordonnance: Ordonnance) {
        viewModelScope.launch {
            try {
                repository.deleteOrdonnance(ordonnance)
                _operationSuccess.value = "deleted"
            } catch (e: Exception) {
                _error.value = "Erreur lors de la suppression: ${e.message}"
            }
        }
    }

    fun clearOperationSuccess() {
        _operationSuccess.value = null
    }

    fun clearError() {
        _error.value = null
    }
    
    fun onOrdonnanceClicked(@Suppress("UNUSED_PARAMETER") id: Int) {
        // détails de l'ordonnance avec id
    }

    fun onSaisieClicked() {
        // ouvrir ecran de saisie
    }

    fun onScanClicked() {
        // ouvrir ecran de scan
    }

    fun getActiveUserId(): Int? {
        return authRepository.getActiveUserId()
    }
}