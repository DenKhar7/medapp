package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicament_substance_detail")
data class MedicamentSubstanceDetail(
    @PrimaryKey val nomSubstance: String,
    val typeSubstance: String?,
    val formuleMoleculaire: String?,
    val poidMoleculaire: Float?
)