package iut.butinfo3.app_mobile.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iut.butinfo3.app_mobile.calendar.CalendarService
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import iut.butinfo3.app_mobile.view.adapter.CalendarDay
import iut.butinfo3.app_mobile.utils.RecurrenceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel de l'écran d'Accueil (Dashboard).
 *
 * Il est responsable de l'affichage de la "To-Do List" des médicaments du jour.
 * Il réagit automatiquement aux changements d'utilisateur (via le SessionRepository).
 */
class HomeViewModel(
    private val repository: TreatmentRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val calendarService = CalendarService()

    private val _calendarDays = MutableStateFlow<List<CalendarDay>>(emptyList())
    val calendarDays = _calendarDays.asStateFlow()

    private var selectedCalendarDay: CalendarDay? = null

    private var currentMonthContext: Calendar = Calendar.getInstance()
    private val _dailyIntakes = MutableStateFlow<List<DailyIntake>>(emptyList())
    val dailyIntakes = _dailyIntakes.asStateFlow()

    private val _selectedDateIntakes = MutableStateFlow<List<DailyIntake>>(emptyList())
    val selectedDateIntakes = _selectedDateIntakes.asStateFlow()

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    private val _isSelectedDatePast = MutableStateFlow(false)
    val isSelectedDatePast = _isSelectedDatePast.asStateFlow()

    private var intakeJob: Job? = null

    init {
        observeSession()
    }

    /**
     * Surveille le SessionRepository.
     * Si l'utilisateur change, on annule l'ancienne surveillance de médicaments
     * et on en lance une nouvelle pour le nouvel utilisateur.
     */
    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.selectedUser.collect { user ->
                intakeJob?.cancel()
                if (user != null) {
                    intakeJob = viewModelScope.launch {
                        repository.getActiveRemindersFlow(user.id).collect { allActiveReminders ->
                            _dailyIntakes.value = allActiveReminders.filter{
                                RecurrenceUtils.isReminderDueToday(
                                    treatmentStartDate = it.startDate,
                                    interval = it.recurrenceInterval,
                                    unit = it.recurrenceUnit
                                )
                            }
                        }
                    }
                    loadCalendar(currentMonthContext, user.id)
                } else {
                    _dailyIntakes.value = emptyList()
                    _calendarDays.value = emptyList()
                }
            }
        }
    }

    /**
     * Coche ou décoche un médicament (Prise effectuée/annulée).
     * @param intake L'objet représentant la prise (contenant l'ID du rappel et la date).
     * @param isChecked Le nouvel état de la case.
     */
    fun toggleTaken(intake: DailyIntake, isChecked: Boolean) {
        viewModelScope.launch {
            repository.setMedicamentTaken(intake, isChecked)
            val userId = sessionRepository.selectedUser.value?.id
            if (userId != null) loadCalendar(currentMonthContext, userId)
        }
    }

    /**
     * Récupère le statut des médicaments pour une date donnée.
     * @param userId L'identifiant de l'utilisateur
     * @param date La date pour laquelle on veut connaître le statut
     * @return Une paire (total, pris) représentant le nombre total de médicaments prévus et le nombre pris
     */
    suspend fun getMedicationStatusForDate(userId: Int, date: java.util.Date): Pair<Int, Int> {
        return repository.getMedicationStatusForDateSync(userId, date)
    }

    /**
     * Charge le calendrier pour un mois donné et un utilisateur.
     */
    fun loadCalendar(calendar: Calendar, userId: Int) {
        this.currentMonthContext = calendar.clone() as Calendar

        viewModelScope.launch {
            val days = calendarService.generateCalendarDays(
                currentCalendar = calendar,
                selectedDay = selectedCalendarDay,
                statusProvider = { date ->
                    getMedicationStatusForDate(userId, date)
                }
            )
            _calendarDays.value = days
        }
    }

    /**
     * Gère le clic sur un jour.
     * Met à jour l'état interne et demande un rafraîchissement de la grille.
     */
    fun selectDay(day: CalendarDay) {
        selectedCalendarDay = day

        val userId = sessionRepository.selectedUser.value?.id ?: return

        loadCalendar(currentMonthContext, userId)

        // Charger les traitements du jour sélectionné
        val date = day.date ?: return

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        _isSelectedDatePast.value = selectedCal.before(today)

        val isToday = selectedCal[Calendar.YEAR] == today[Calendar.YEAR] &&
                selectedCal[Calendar.MONTH] == today[Calendar.MONTH] &&
                selectedCal[Calendar.DAY_OF_MONTH] == today[Calendar.DAY_OF_MONTH]

        if (isToday) {
            // Pour aujourd'hui, utiliser le flow réactif existant
            _selectedDateIntakes.value = emptyList()
            _selectedDate.value = null
        } else {
            _selectedDateIntakes.value = emptyList()
            _selectedDate.value = date
            viewModelScope.launch {
                val intakes = repository.getIntakesForDate(userId, date)
                _selectedDateIntakes.value = intakes
            }
        }
    }
}