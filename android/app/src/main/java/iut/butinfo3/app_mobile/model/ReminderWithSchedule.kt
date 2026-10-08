package iut.butinfo3.app_mobile.model

import java.util.Date

data class ReminderWithSchedule(
    val reminderId: Int,
    val timeOfDay: String,
    val doseQuantity: String,
    val recurrenceUnit: String,
    val recurrenceInterval: Int,
    val drugName: String,
    val drugCip: String,
    val treatmentId: Int,
    val startDate: Date
)