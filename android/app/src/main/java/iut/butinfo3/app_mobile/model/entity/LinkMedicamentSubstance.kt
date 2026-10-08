package iut.butinfo3.app_mobile.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "medicament_substance_link",
    primaryKeys = ["cis", "codeSubstance"],
    foreignKeys = [
        ForeignKey(
            entity = MedicamentSpeResume::class,
            parentColumns = ["cis"],
            childColumns = ["cis"]
        ),
        ForeignKey(entity = MedicamentSubstanceResume::class, parentColumns = ["codeSubstance"], childColumns = ["codeSubstance"])
    ],
    indices = [Index("codeSubstance")]
)
data class LinkMedicamentSubstance(
    val cis: Int,
    val codeSubstance: String,
    val relationSubstance: String?,
    val dosage: String?
)