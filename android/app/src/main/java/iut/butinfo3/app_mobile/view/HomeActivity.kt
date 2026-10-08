package iut.butinfo3.app_mobile.view

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import iut.butinfo3.app_mobile.databinding.HomeActivityBinding
import iut.butinfo3.app_mobile.utils.NavigationHelper
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.utils.setupProfileSelector
import iut.butinfo3.app_mobile.view.adapter.CalendarAdapter
import iut.butinfo3.app_mobile.view.adapter.MedicamentsDuJourAdapter
import iut.butinfo3.app_mobile.view_model.HomeViewModel
import iut.butinfo3.app_mobile.view_model.MainViewModel
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.reminder.ReminderScheduler
import androidx.core.content.edit
import androidx.recyclerview.widget.GridLayoutManager
import iut.butinfo3.app_mobile.model.DailyIntake
import iut.butinfo3.app_mobile.utils.LocaleHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: HomeActivityBinding

    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory(this) }
    private val viewModel: HomeViewModel by viewModels { ViewModelFactory(this) }

    private lateinit var navigationHelper: NavigationHelper

    // Calendrier
    private var currentCalendar = Calendar.getInstance()
    private var currentLanguage: String = "fr"
    private val calendarAdapter = CalendarAdapter { clickedDay ->
        viewModel.selectDay(clickedDay)
    }

    override fun attachBaseContext(newBase: Context?) {
        val language = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "fr"
        currentLanguage = language
        super.attachBaseContext(newBase?.let { LocaleHelper.setLocale(it, language) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAndRequestPermissions()
        setupDailyReminderScheduler()

        setupBottomNavigation()
        setupRecyclerViews()

        setupCalendarRecycler()
        setupCalendarNavigation()

        setupObservers()
        setupClickListeners()

        setupProfileSelector(binding.headerSelector.rvProfileSelector, mainViewModel)
    }

    /**
     * Demande les autorisations pour programmer des alarms et envoyer des notification.
     * Stocke et vérifie si la demande à déjà été envoyé
     */
    private fun checkAndRequestPermissions() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

            if (notifPermission != PackageManager.PERMISSION_GRANTED) {
                val alreadyAsked = prefs.getBoolean("notification_permission_asked", false)

                if (!alreadyAsked) {
                    prefs.edit { putBoolean("notification_permission_asked", true) }
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                }
            }
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            val alreadyAsked = prefs.getBoolean("exact_alarm_permission_asked", false)

            if (!alreadyAsked) {
                prefs.edit { putBoolean("exact_alarm_permission_asked", true) }
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
        }
    }

    /**
     * Initialise la liste des médicaments du jour.
     */
    private fun setupRecyclerViews() {
        val medicAdapter = MedicamentsDuJourAdapter { intake, isChecked ->
            viewModel.toggleTaken(intake, isChecked)
        }
        binding.rvMedicamentsDuJour.adapter = medicAdapter
        binding.rvMedicamentsDuJour.layoutManager = LinearLayoutManager(this)
    }

    private fun setupCalendarRecycler() {
        binding.rvCalendrier.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 7)
            adapter = calendarAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupCalendarNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            refreshCalendar()
        }

        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            refreshCalendar()
        }
    }

    /**
     * Helper pour recharger le calendrier via le ViewModel
     */
    private fun refreshCalendar() {
        val userId = mainViewModel.selectedUser.value?.id ?: return
        viewModel.loadCalendar(currentCalendar, userId)
    }

    private fun updateMonthTitle() {
        val locale = Locale.forLanguageTag(currentLanguage)
        val format = SimpleDateFormat("MMMM yyyy", locale)
        val title = format.format(currentCalendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        binding.tvMonthYear.text = title
    }

    /**
     * Observe les flux de données (Flows) du ViewModel.
     * Met à jour les medicaments en fonction s'il sont cochés ou non.
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.selectedUser.collect { user ->
                        user?.let { viewModel.loadCalendar(currentCalendar, it.id) }
                    }
                }
                launch {
                    viewModel.dailyIntakes.collect { list ->
                        if (viewModel.selectedDate.value == null) {
                            updateMedicamentsList(MedicamentsDuJourAdapter.DayMode.TODAY, list)
                        }
                    }
                }
                launch {
                    viewModel.selectedDate.collect { date ->
                        handleSelectedDateChange(date)
                    }
                }
                launch {
                    viewModel.selectedDateIntakes.collect { list ->
                        if (viewModel.selectedDate.value != null) {
                            updateMedicamentsList(getSelectedDayMode(), list)
                        }
                    }
                }
                launch {
                    viewModel.calendarDays.collect { days ->
                        calendarAdapter.submitList(days)
                        updateMonthTitle()
                    }
                }
            }
        }
    }

    private fun handleSelectedDateChange(date: Date?) {
        if (date == null) {
            showTodayMedicaments()
        } else {
            showSelectedDateMedicaments(date)
        }
    }

    private fun showTodayMedicaments() {
        binding.tvMedicamentsDuJourTitle.text = getString(R.string.medicaments_du_jour)
        updateMedicamentsList(MedicamentsDuJourAdapter.DayMode.TODAY, viewModel.dailyIntakes.value)
    }

    private fun showSelectedDateMedicaments(date: Date) {
        binding.tvMedicamentsDuJourTitle.text = formatSelectedDateTitle(date)
        updateMedicamentsList(getSelectedDayMode(), viewModel.selectedDateIntakes.value)
    }

    private fun formatSelectedDateTitle(date: Date): String {
        val locale = Locale.forLanguageTag(currentLanguage)
        val dateFormat = SimpleDateFormat("d MMMM", locale)
        return getString(R.string.medicaments_du_date, dateFormat.format(date))
    }

    private fun getSelectedDayMode(): MedicamentsDuJourAdapter.DayMode {
        return if (viewModel.isSelectedDatePast.value) {
            MedicamentsDuJourAdapter.DayMode.PAST
        } else {
            MedicamentsDuJourAdapter.DayMode.FUTURE
        }
    }

    private fun updateMedicamentsList(mode: MedicamentsDuJourAdapter.DayMode, list: List<DailyIntake>) {
        val adapter = binding.rvMedicamentsDuJour.adapter as? MedicamentsDuJourAdapter
        adapter?.setDayMode(mode)
        adapter?.submitList(list)
    }

    private fun setupClickListeners() {
        binding.btnTreatment.setOnClickListener {
            val intent = Intent(this, UserTreatmentsActivity::class.java)
            val currentUserId = mainViewModel.selectedUser.value?.id
            if (currentUserId != null) {
                intent.putExtra("EXTRA_USER_ID", currentUserId)
            }
            startActivity(intent)
        }
        binding.btnEmergency.setOnClickListener {
            showEmergencyOptions()
        }
    }

    private fun setupBottomNavigation() {
        navigationHelper = NavigationHelper(
            binding.navHome,
            binding.navUserTreatments,
            binding.navMedicaments,
            binding.navOrdonnances,
            binding.navPathologie
        )
        navigationHelper.init(NavigationHelper.NAV_HOME)

        binding.navUserTreatments.setOnClickListener { startActivity(Intent(this, UserTreatmentsActivity::class.java)) }
        binding.navMedicaments.setOnClickListener { startActivity(Intent(this, MedicamentActivity::class.java)) }
        binding.navPathologie.setOnClickListener { startActivity(Intent(this, PathologieActivity::class.java)) }
        binding.navOrdonnances.setOnClickListener { startActivity(Intent(this, OrdonnanceActivity::class.java)) }
    }

    /**
     * Lance le programme qui initialise les alarmes pour chaque jours.
     */
    private fun setupDailyReminderScheduler() {
        ReminderScheduler(this).scheduleDaily()
    }

    override fun onResume() {
        super.onResume()

        val savedLanguage = LocaleHelper.getSavedLanguage(this)
        if (savedLanguage != currentLanguage) {
            recreate()
            return
        }

        navigationHelper.setActiveTab(NavigationHelper.NAV_HOME)
    }

    /**
     * Affiche le panneau du bas avec les choix.
     */
    private fun showEmergencyOptions() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_emergency_sheet, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnCall112).setOnClickListener {
            dialog.dismiss()
            openDialer()
        }

        view.findViewById<View>(R.id.btnAnsm).setOnClickListener {
            dialog.dismiss()
            openWebsite()
        }

        dialog.show()
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:112".toUri()
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openWebsite() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://signalement.social-sante.gouv.fr/".toUri()
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}