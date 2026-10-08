package iut.butinfo3.app_mobile.model

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import java.util.Date

/**
 * Données nécessaires pour créer ou modifier un traitement.
 */
data class TreatmentFormData(
    val treatmentId: Long,
    val userId: Int,
    val cipDrug: String,
    val drugName: String,
    val startDate: Date,
    val endDate: Date?,
    val instructions: String,
    val rawReminders: List<Pair<String, String>>,
    val recurrenceUnit: RecurrenceUnit,
    val recurrenceInterval: Int
)