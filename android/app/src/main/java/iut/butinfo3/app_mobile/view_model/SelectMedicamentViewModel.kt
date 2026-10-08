package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.repository.MedicamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail

class SelectMedicamentViewModel(private val repository: MedicamentRepository) : ViewModel() {
    private val _medicaments = MutableStateFlow<List<MedicamentSpeResume>>(emptyList())
    val medicaments: StateFlow<List<MedicamentSpeResume>> = _medicaments
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun searchMedicaments(query: String, limit: Int = 50) {
        viewModelScope.launch {
            try {
                val presList = repository.searchByLibelle(query, limit)
                val resumeList = presList.map {
                    MedicamentSpeResume(
                        cis = it.cis,
                        nom = it.nomSpecialite,
                        nomOrganisation = it.nomOrganisation,
                        codeAtc = it.codeAtc,
                        libelleAtc = it.libelleAtc,
                        voies = it.voie
                    )
                }
                _medicaments.value = resumeList
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    suspend fun ensurePresentationInDb(cis: Int): Boolean {
        return try {
            val presentations = repository.getPresentationsByCis(cis)
            presentations.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPresentationsByCis(cis: Int): List<MedicamentPresDetail> {
        return try {
            repository.getPresentationsByCis(cis)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
