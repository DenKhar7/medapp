package iut.butinfo3.app_mobile.model.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * @Embedded permet à Room de connaître les champs de l'entité UserTreatment, cela permet ensuit d'appeler
 * simplement @Relation avec cipRef sans avoir à lister toutes les colonnes de Usertreatment.
 *
 * @Relation Permet de trouver le médicament qui à un cip13 == cipRef de UserTreatment, pour récupérer le bon médoc
 */
data class TreatmentWithMedicament(
    @Embedded val treatment: UserTreatment,
    @Relation(
        parentColumn = "cipRef",
        entityColumn = "cip13"
    )
    val medicament: MedicamentPresDetail
)