package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medicament_status_cis")
data class MedicamentStatusCis(
    @PrimaryKey val cis: Int,
    val dateEffet : LocalDate?,
    val typeEvenement : String?
)