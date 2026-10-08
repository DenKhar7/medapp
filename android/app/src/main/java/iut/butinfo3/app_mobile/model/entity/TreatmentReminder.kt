package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stock les rappels journaliers des médicaments, en fonction de l'heure du jour.
 */
@Entity(
    tableName = "treatment_reminder",
    foreignKeys = [
        ForeignKey(
            entity = UserTreatment::class,
            parentColumns = ["id"],
            childColumns = ["treatmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("treatmentId")]
)
data class TreatmentReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val treatmentId: Int,

    val timeOfDay: String,

    val doseQuantity: String,

    val label: String? = null,

    val recurrenceInterval: Int = 1,

    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAY
)