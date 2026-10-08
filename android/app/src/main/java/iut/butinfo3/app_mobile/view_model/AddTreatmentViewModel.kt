package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.model.TreatmentFormData
import iut.butinfo3.app_mobile.reminder.ReminderManager
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import iut.butinfo3.app_mobile.model.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


/**
 * ViewModel pour l'écran d'Ajout/Modification de Traitement.
 *
 * Il gère :
 * 1. La validation des données saisies (Dates cohérentes, rappels présents...).
 * 2. La distinction entre Création (Nouveau) et Modification (Existant).
 * 3. La préparation des objets pour la sauvegarde en BDD via le Repository.
 */
class AddTreatmentViewModel(
    private val repository: TreatmentRepository,
    private val userRepository: UserRepository,
    private val currentUserId: Int,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTreatmentState>(AddTreatmentState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _existingData = MutableStateFlow<Pair<UserTreatment, List<TreatmentReminder>>?>(null)
    val existingData = _existingData.asStateFlow()

    val familyMembers: Flow<List<User>> = userRepository.getAllMembers(currentUserId)

    fun getActiveUserId(): Int = currentUserId

    /**
     * Méthode principale : Valide, Construit et Sauvegarde le traitement.
     *
     * @param formData Données du formulaire de traitement.
     */
    fun saveOrUpdateTreatment(formData: TreatmentFormData) {
        if (!validateFormData(formData)) return

        val treatment = buildTreatment(formData)
        val reminders = buildReminders(formData)

        viewModelScope.launch {
            _uiState.value = AddTreatmentState.Loading
            try {
                val savedReminders = saveToRepository(formData.treatmentId, treatment, reminders)
                scheduleRemindersForUser(formData.userId, formData.drugName, savedReminders)
                _uiState.value = AddTreatmentState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AddTreatmentState.Error("Erreur lors de la sauvegarde : ${e.localizedMessage}")
            }
        }
    }

    private fun validateFormData(formData: TreatmentFormData): Boolean {
        if (formData.endDate != null && formData.endDate < formData.startDate) {
            _uiState.value = AddTreatmentState.Error("La date de fin ne peut pas être avant le début du traitement")
            return false
        }
        if (formData.rawReminders.isEmpty()) {
            _uiState.value = AddTreatmentState.Error("Veuillez ajouter au moins une prise (heure et quantité)")
            return false
        }
        return true
    }

    private fun buildTreatment(formData: TreatmentFormData) = UserTreatment(
        id = formData.treatmentId.toInt(),
        userId = formData.userId,
        cipRef = formData.cipDrug,
        startDate = formData.startDate,
        endDate = formData.endDate,
        instructions = formData.instructions,
        isFromPrescription = false
    )

    private fun buildReminders(formData: TreatmentFormData) = formData.rawReminders.map { (time, dose) ->
        TreatmentReminder(
            treatmentId = formData.treatmentId.toInt(),
            timeOfDay = time,
            doseQuantity = dose,
            recurrenceInterval = formData.recurrenceInterval,
            recurrenceUnit = formData.recurrenceUnit
        )
    }

    private suspend fun saveToRepository(
        treatmentId: Long,
        treatment: UserTreatment,
        reminders: List<TreatmentReminder>
    ): List<TreatmentReminder> {
        return if (treatmentId == 0L) {
            repository.saveTreatmentWithReminders(treatment, reminders)
        } else {
            repository.updateTreatmentWithReminders(treatment, reminders)
        }
    }

    private suspend fun scheduleRemindersForUser(
        userId: Int,
        drugName: String,
        reminders: List<TreatmentReminder>
    ) {
        val user = userRepository.getUserById(userId).first()
        reminders.forEach { reminder ->
            val scheduled = reminderManager.scheduleReminder(
                reminder = reminder,
                drugName = drugName,
                userName = user.username,
                treatmentId = reminder.treatmentId
            )
            if (!scheduled) {
                android.util.Log.w("AddTreatmentViewModel", "Failed to schedule reminder ${reminder.id}")
            }
        }
    }

    /**
     * Charge les données d'un traitement existant pour pré-remplir le formulaire.
     */
    fun loadTreatmentData(treatmentId: Long) {
        viewModelScope.launch {
            val treatment = repository.getTreatmentById(treatmentId)
            val reminders = repository.getRemindersForTreatment(treatmentId)
            if (treatment != null) {
                _existingData.value = Pair(treatment, reminders)
            }
        }
    }
}
/**
 * États possibles de l'écran (pour gérer l'affichage du bouton et des Toasts).
 */
sealed class AddTreatmentState {
    object Idle : AddTreatmentState()
    object Loading : AddTreatmentState()
    object Success : AddTreatmentState()
    data class Error(val message: String) : AddTreatmentState()
}