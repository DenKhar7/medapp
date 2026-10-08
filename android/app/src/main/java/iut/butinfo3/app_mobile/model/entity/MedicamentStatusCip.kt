package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medicament_status_cip")
data class MedicamentStatusCip(
    @PrimaryKey val cip13: String,
    val dateEffet : LocalDate?,
    val typeEvenement : String?
)