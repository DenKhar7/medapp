package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.repository.MedicamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran de Recherche et Scan de médicaments.
 *
 * Gère une liste temporaire de médicaments scannés pendant la session.
 *
 * @property repository Le dépôt pour interroger l'API ou la BDD locale des médicaments.
 */
class MedicamentViewModel(val repository : MedicamentRepository) : ViewModel() {

    /**
     * Action déclenchée quand on clique sur un médicament de la liste.
     * //TODO : Actuellement vide, présent uniquement pour que le onclick marche.
     */
    fun onMedicamentClicked(id: Int) {
    }
    private val _searchResults = MutableStateFlow<List<MedicamentPresDetail>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _uiState = MutableStateFlow<MedicamentState>(MedicamentState.Idle)
    val uiState = _uiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                _uiState.value = MedicamentState.Idle
                return@launch
            }

            _uiState.value = MedicamentState.Loading

            try {
                val results = repository.searchByLibelle(query)
                _searchResults.value = results
                _uiState.value = if (results.isEmpty()) MedicamentState.Empty else MedicamentState.Success
            } catch (e: Exception) {
                _uiState.value = MedicamentState.Error(e.message ?: "Erreur réseau")
            }
        }
    }
}
/**
 * États possibles de l'écran de scan.
 */
sealed class MedicamentState {
    object Success : MedicamentState()
    object Idle : MedicamentState()
    object Loading : MedicamentState()
    object Empty : MedicamentState()
    data class Error(val msg : String) : MedicamentState()
}