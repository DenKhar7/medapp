package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicament_substance_resume")
data class MedicamentSubstanceResume (
    @PrimaryKey val codeSubstance : String,
    val libelle : String,
    val relationSubstance : String,
    val expressionQuantite : String?,
    val substanceActive : Int?,
    val fractionTherapeutique : Int?
)
