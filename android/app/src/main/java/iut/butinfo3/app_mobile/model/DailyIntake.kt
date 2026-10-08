package iut.butinfo3.app_mobile.model

import iut.butinfo3.app_mobile.model.entity.IntakeStatus
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import java.util.Date

data class DailyIntake(
    val reminderId: Int,
    val medicamentName: String,
    val medicamentCip: String,
    val dosage: String,
    val time: String,
    val isTaken: Boolean,
    val treatmentId: Int,
    val recurrenceUnit: RecurrenceUnit,
    val recurrenceInterval: Int,
    val startDate: Date,
    val intakeStatus: IntakeStatus? = null
)

fun DailyIntake.toTreatmentReminder() = TreatmentReminder(
    id = reminderId.toLong(),
    timeOfDay = time,
    doseQuantity = dosage,
    treatmentId = treatmentId,
    recurrenceUnit = recurrenceUnit,
    recurrenceInterval = recurrenceInterval
)
