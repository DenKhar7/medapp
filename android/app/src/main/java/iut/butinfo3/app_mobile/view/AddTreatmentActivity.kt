package iut.butinfo3.app_mobile.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import iut.butinfo3.app_mobile.R
import iut.butinfo3.app_mobile.databinding.AddTreatmentActivityBinding
import iut.butinfo3.app_mobile.model.TreatmentFormData
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.utils.ViewModelFactory
import iut.butinfo3.app_mobile.utils.toReadableDate
import iut.butinfo3.app_mobile.utils.toTimeParts
import iut.butinfo3.app_mobile.view_model.AddTreatmentState
import iut.butinfo3.app_mobile.view_model.AddTreatmentViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Écran d'Ajout ou de Modification d'un traitement.
 *
 * Fonctionnalités techniques :
 * 1. Interface Dynamique : Ajout/Suppression de lignes d'horaires (Rappels) à la volée via [addView] et [removeView].
 * 2. Mode Édition : Si un ID est passé en paramètre, l'écran pré-charge les données existantes.
 * 3. Gestion des Dates : Utilisation des Vues pour stocker les Timestamps (Long) derrière le texte affiché.
 */
class AddTreatmentActivity : AppCompatActivity() {

    private lateinit var binding: AddTreatmentActivityBinding
    private val viewModel: AddTreatmentViewModel by viewModels {
        ViewModelFactory(this)
    }

    private val recurrenceMap = mapOf(
        "Jours" to RecurrenceUnit.DAY,
        "Lundis" to RecurrenceUnit.MONDAY,
        "Mardis" to RecurrenceUnit.TUESDAY,
        "Mercredis" to RecurrenceUnit.WEDNESDAY,
        "Jeudis" to RecurrenceUnit.THURSDAY,
        "Vendredis" to RecurrenceUnit.FRIDAY,
        "Samedis" to RecurrenceUnit.SATURDAY,
        "Dimanches" to RecurrenceUnit.SUNDAY
    )

    // Mapping inverse pour la modification
    private val reverseRecurrenceMap = recurrenceMap.entries.associate { (k, v) -> v to k }

    private lateinit var cipDrug: String
    private var treatmentId: Long = 0L
    private var selectedUserId: Int = -1
    private var navigateToHomeAfter: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddTreatmentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cipDrug = intent.getStringExtra("EXTRA_CIP") ?: ""
        val nameDrug = intent.getStringExtra("EXTRA_NAME") ?: "Médicament"
        treatmentId = intent.getLongExtra("EXTRA_TREATMENT_ID", 0L)
        selectedUserId = intent.getIntExtra("EXTRA_TARGET_USER_ID", -1)
        navigateToHomeAfter = intent.getBooleanExtra("EXTRA_NAVIGATE_TO_HOME_AFTER", false)
        if (selectedUserId <= 0) {
            val activeId = viewModel.getActiveUserId()
            if (activeId > 0) selectedUserId = activeId
        }
        if (selectedUserId <= 0) {
            android.widget.Toast.makeText(this, "Utilisateur introuvable.", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDrugName.text = nameDrug
        setupListeners()

        if (treatmentId != 0L) {
            setupEditMode()
        } else {
            setupCreateMode()
        }
        setupObservers()
        setupSpinners()
    }

    private fun setupCreateMode() {
        updateDateDisplay(binding.etStartDate, System.currentTimeMillis())
        addReminderRow()
    }

    private fun setupEditMode() {
        binding.tvTitlePage.text = getString(R.string.edit_treatment_title)
        binding.btnValidate.text = getString(R.string.edit_treatment_btn)
        viewModel.loadTreatmentData(treatmentId)
    }

    private fun setupListeners() {
        binding.etStartDate.setOnClickListener { showDatePicker(binding.etStartDate) }

        binding.etEndDate.setOnClickListener {
            val minDate = binding.etStartDate.tag as? Long ?: System.currentTimeMillis()
            showDatePicker(binding.etEndDate, minDate)
        }

        binding.btnAddReminder.setOnClickListener { addReminderRow() }
        binding.btnValidate.setOnClickListener { validateAndSave() }
    }

    /**
     * Observe les flux de données (Flows) du ViewModel.
     * Utilise 3 coroutines distinctes:
     *
     * 1. Mettre à jour les "chips" de sélection des utilisateur (pour leur ajouter un traitement)
     * 2. Vérifier l'état de l'UI (des valeurs du viewModel)
     * 3. Vérifier s'il existe des données (si on est dans l'écran de modification),
     *    si oui, mettre à jour la vue avec les valeurs stockées.
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.familyMembers.collect { users ->
                        setupFamilyChips(users)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.existingData.collect { data ->
                        data?.let { populateExistingData(it) }
                    }
                }
            }
        }
    }

    private fun populateExistingData(data: Pair<UserTreatment, List<TreatmentReminder>>) {
        val (treatment, reminders) = data

        reminders.firstOrNull()?.let { populateRecurrenceFields(it) }

        updateDateDisplay(binding.etStartDate, treatment.startDate.time)
        treatment.endDate?.let { updateDateDisplay(binding.etEndDate, it.time) }

        binding.etInstructions.setText(treatment.instructions)

        populateReminderRows(reminders)
    }

    private fun populateRecurrenceFields(reminder: TreatmentReminder) {
        binding.etRecurrenceInterval.setText(reminder.recurrenceInterval.toString())
        val unitInUserLanguage = reverseRecurrenceMap[reminder.recurrenceUnit] ?: "Jours"
        @Suppress("UNCHECKED_CAST")
        val adapter = binding.spRecurrenceUnit.adapter as ArrayAdapter<String>
        binding.spRecurrenceUnit.setSelection(adapter.getPosition(unitInUserLanguage))
    }

    private fun populateReminderRows(reminders: List<TreatmentReminder>) {
        binding.llRemindersContainer.removeAllViews()
        if (reminders.isEmpty()) {
            addReminderRow()
        } else {
            reminders.forEach { addReminderRow(it.timeOfDay, it.doseQuantity) }
        }
    }

    private fun handleUiState(state: AddTreatmentState) {
        when (state) {
            is AddTreatmentState.Loading -> {
                binding.btnValidate.isEnabled = false
                binding.btnValidate.text = "Sauvegarde..."
            }
            is AddTreatmentState.Success -> {
                val msg = if (treatmentId == 0L) "Traitement ajouté" else "Modifié avec succès"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                if (navigateToHomeAfter) {
                    // Navigate to HomeActivity and clear the back stack
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    finish()
                }
            }
            is AddTreatmentState.Error -> {
                binding.btnValidate.isEnabled = true
                binding.btnValidate.text = getString(R.string.add_treatment_btn_validate)
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    /**
     * Ajoute une ligne (Heure + Dose + Bouton Supprimer) dans le conteneur vertical.
     * C'est ici qu'on fait de la manipulation de vue "à la main" (sans RecyclerView).
     */
    private fun addReminderRow(existingTime: String? = null, existingDose: String? = null) {
        val view = layoutInflater.inflate(R.layout.item_reminder_input, binding.llRemindersContainer, false)

        val btnTime = view.findViewById<Button>(R.id.btnTime)
        val etDose = view.findViewById<EditText>(R.id.etDose)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

        // Pré-remplissage
        existingTime?.let { btnTime.text = it }
        existingDose?.let { etDose.setText(it) }

        btnTime.setOnClickListener {
            val (hour, minute) = btnTime.text.toString().toTimeParts() // Utilisation de l'extension
            TimePickerDialog(this, { _, h, m ->
                btnTime.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            }, hour, minute, true).show()
        }

        btnDelete.setOnClickListener {
            binding.llRemindersContainer.removeView(view)
        }

        binding.llRemindersContainer.addView(view)
    }

    /**
     * Permet d'afficher un calendrier pour choisir la date de début et de fin du traitement.
     */
    private fun showDatePicker(editText: EditText, minDateMillis: Long? = null) {
        val currentMillis = editText.tag as? Long ?: System.currentTimeMillis()
        val c = Calendar.getInstance().apply { timeInMillis = currentMillis }

        val picker = DatePickerDialog(this, { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d) }

            if (editText == binding.etStartDate) {
                val currentEnd = binding.etEndDate.tag as? Long
                if (currentEnd != null && currentEnd < newCal.timeInMillis) {
                    binding.etEndDate
                        .text
                        ?.clear()
                    binding.etEndDate.tag = null
                    Toast.makeText(this, "Date de fin réinitialisée", Toast.LENGTH_SHORT).show()
                }
            }
            updateDateDisplay(editText, newCal.timeInMillis)
        }, c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH])

        minDateMillis?.let { picker.datePicker.minDate = it }
        picker.show()
    }

    /**
     * Met à jour l'affichage ET stocke la vraie valeur dans le Tag.
     */
    private fun updateDateDisplay(editText: EditText, millis: Long) {
        editText.setText(millis.toReadableDate()) // Utilisation de l'extension
        editText.tag = millis
    }

    /**
     * Génère les "Chips" (Pastilles) pour choisir à quel membre de la famille attribuer le médicament.
     */
    private fun setupFamilyChips(users: List<User>) {
        binding.chipGroupFamily.removeAllViews()
        users.forEach { user ->
            val chip = Chip(this).apply {
                text = user.username
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedUserId = user.id
                }
            }
            if (user.id == selectedUserId || (selectedUserId == -1 && user == users.firstOrNull())) {
                chip.isChecked = true
                selectedUserId = user.id
            }
            binding.chipGroupFamily.addView(chip)
        }
    }

    /**
     * Fonction pour valider et sauvegarder le traitement, pour chaque ligne de rappels on génère une entité spécifique.
     * On vérifie que les dates sont correctes
     */
    private fun validateAndSave() {
        val startMillis = binding.etStartDate.tag as? Long ?: return run {
            Toast.makeText(this, "Date de début requise", Toast.LENGTH_SHORT).show()
        }
        val endMillis = binding.etEndDate.tag as? Long

        if (endMillis != null && endMillis <= startMillis) {
            binding.etEndDate.error = "Doit être après le début"
            return
        }

        val remindersList = collectReminders()
        if (remindersList.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins une prise", Toast.LENGTH_SHORT).show()
            return
        }

        val formData = TreatmentFormData(
            treatmentId = treatmentId,
            userId = selectedUserId,
            cipDrug = cipDrug,
            drugName = binding.tvDrugName.text.toString(),
            startDate = Date(startMillis),
            endDate = endMillis?.let { Date(it) },
            instructions = binding.etInstructions.text.toString(),
            rawReminders = remindersList,
            recurrenceUnit = getSelectedRecurrenceUnit(),
            recurrenceInterval = getRecurrenceInterval()
        )

        viewModel.saveOrUpdateTreatment(formData)
    }

    private fun collectReminders(): List<Pair<String, String>> {
        val remindersList = mutableListOf<Pair<String, String>>()
        for (i in 0 until binding.llRemindersContainer.childCount) {
            val row = binding.llRemindersContainer.getChildAt(i)
            val time = row.findViewById<Button>(R.id.btnTime).text.toString()
            val dose = row.findViewById<EditText>(R.id.etDose).text.toString()
            if (dose.isNotBlank()) {
                remindersList.add(time to dose)
            }
        }
        return remindersList
    }

    private fun getRecurrenceInterval(): Int {
        val intervalStr = binding.etRecurrenceInterval.text.toString()
        return if (intervalStr.isBlank() || intervalStr == "0") 1 else intervalStr.toInt()
    }

    private fun getSelectedRecurrenceUnit(): RecurrenceUnit {
        val selectedLangUnit = binding.spRecurrenceUnit.selectedItem.toString()
        return recurrenceMap[selectedLangUnit] ?: RecurrenceUnit.DAY
    }
    private fun setupSpinners() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            recurrenceMap.keys.toList()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRecurrenceUnit.adapter = adapter
    }
}
