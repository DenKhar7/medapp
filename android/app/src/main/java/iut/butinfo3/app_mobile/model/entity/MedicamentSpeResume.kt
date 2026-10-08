package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicament_specialite")
data class MedicamentSpeResume(
    @PrimaryKey val cis: Int,
    val nom: String,
    val nomOrganisation: String?,
    val codeAtc: String?,
    val libelleAtc: String?,
    val voies: List<String>
)