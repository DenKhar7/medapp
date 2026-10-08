package iut.butinfo3.app_mobile.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Permet de stocker l'historique des prises de médicament d'un utilisateur.
 */
@Entity(
    tableName = "treatment_history",
    foreignKeys = [
        ForeignKey(
            entity = TreatmentReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reminderId"), Index("date_taken")]
)
data class TreatmentHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Int,

    @ColumnInfo(name = "date_taken")
    val dateTaken: Date,

    @ColumnInfo(name = "taken_at")
    val takenAt: Date,

    @ColumnInfo(name = "status", defaultValue = "TAKEN")
    val status: IntakeStatus = IntakeStatus.TAKEN
)