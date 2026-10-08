package iut.butinfo3.app_mobile.view_model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.repository.CameraRepository
import iut.butinfo3.app_mobile.model.repository.OrdonnanceRepository
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import iut.butinfo3.app_mobile.model.OcrResult

class OrdonnanceScreenViewModel(
    private val ordonnanceRepository: OrdonnanceRepository,
    private val cameraRepository: CameraRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _ordonnances = MutableStateFlow<List<Ordonnance>>(emptyList())
    val ordonnances: StateFlow<List<Ordonnance>> = _ordonnances.asStateFlow()

    // event one shot pour ocrResult pour eviter auto clonage de l activite
    private val _ocrResult = MutableSharedFlow<OcrResult>(extraBufferCapacity = 1)
    val ocrResult = _ocrResult.asSharedFlow()

    // event one shot pour erreurs UI
    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error = _error.asSharedFlow()

    fun loadOrdonnancesForActiveUser() {
        val userId = authRepository.getActiveUserId()
        if (userId == null) {
            // on utilise error shared flow pour remonter vers UI
            viewModelScope.launch { _error.emit("Aucun utilisateur actif") }
            return
        }
        viewModelScope.launch {
            ordonnanceRepository.getOrdonnancesByUser(userId).collect { list ->
                _ordonnances.value = list
            }
        }
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val file = cameraRepository.saveImageFromUri(uri)
                val text = cameraRepository.performOCR(file)
                val (doctor, address) = cameraRepository.extractDoctorInfo(text)
                _ocrResult.emit(OcrResult(file, text, doctor, address))
            } catch (e: Exception) {
                _error.emit(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun deleteOrdonnance(ordonnance: Ordonnance) {
        viewModelScope.launch {
            try {
                ordonnanceRepository.deleteOrdonnance(ordonnance)
                loadOrdonnancesForActiveUser()
            } catch (e: Exception) {
                _error.emit("Erreur lors de la suppression: ${e.message}")
            }
        }
    }

    fun getActiveUserId(): Int? {
        return authRepository.getActiveUserId()
    }
}
