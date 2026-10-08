package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date


/**
 * Entité permettant de stocker les treatments( médicament + reminder) associé à un utilisateur.
 */
@Entity(
    tableName = "user_treatment",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(entity = MedicamentPresDetail::class, parentColumns = ["cip13"], childColumns = ["cipRef"])
    ],
    indices = [Index("userId"), Index("cipRef")]
)
data class UserTreatment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val cipRef: String,
    val startDate: Date,
    val endDate: Date?,
    val isFromPrescription: Boolean = false, //TODO: Champ non utilisé a voir pour de futurs améliorations
    val prescriptionImagePath: String? = null,
    val instructions: String? = null
)