package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicament_presentation",
    foreignKeys = [ForeignKey(
        entity = MedicamentSpeResume::class,
        parentColumns = ["cis"],
        childColumns = ["cis"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cis")]
)
data class MedicamentPresDetail(
    @PrimaryKey val cip13: String,
    val label: String,
    val nomSpecialite : String,
    val cis: Int,
    val nomOrganisation : String?,
    val codeAtc: String?,
    val libelleAtc : String?,
    val voie : List<String>,
    val dosesParBoite: Int?,
    val quantiteConditionnement: Double?,
    val uniteConditionnement: String?,
    val typeDispositif: String?
)