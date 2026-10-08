package iut.butinfo3.app_mobile

import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import java.util.Date

/**
 * Factory utilitaire pour créer des données de test partagées entre les tests d'intégration.
 */
object TestDataFactory {

    fun createUser(
        id: Int = 0,
        username: String = "TestUser",
        email: String? = "test@example.com",
        passwordHash: String? = null,
        passwordSalt: String? = null,
        isActive: Boolean = true
    ) = User(
        id = id,
        username = username,
        email = email,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        isActive = isActive,
        createdAt = Date()
    )

    fun createSpecialite(
        cis: Int = 60001234,
        nom: String = "DOLIPRANE",
        nomOrganisation: String? = "SANOFI",
        codeAtc: String? = "N02BE01",
        libelleAtc: String? = "Paracétamol",
        voies: List<String> = listOf("orale")
    ) = MedicamentSpeResume(
        cis = cis,
        nom = nom,
        nomOrganisation = nomOrganisation,
        codeAtc = codeAtc,
        libelleAtc = libelleAtc,
        voies = voies
    )

    fun createPresentation(
        cip13: String = "3400930000001",
        label: String = "DOLIPRANE 1000mg, comprimé",
        nomSpecialite: String = "DOLIPRANE",
        cis: Int = 60001234,
        nomOrganisation: String? = "SANOFI",
        codeAtc: String? = "N02BE01",
        libelleAtc: String? = "Paracétamol",
        voie: List<String> = listOf("orale"),
        dosesParBoite: Int? = 8,
        quantiteConditionnement: Double? = 1000.0,
        uniteConditionnement: String? = "mg",
        typeDispositif: String? = null
    ) = MedicamentPresDetail(
        cip13 = cip13,
        label = label,
        nomSpecialite = nomSpecialite,
        cis = cis,
        nomOrganisation = nomOrganisation,
        codeAtc = codeAtc,
        libelleAtc = libelleAtc,
        voie = voie,
        dosesParBoite = dosesParBoite,
        quantiteConditionnement = quantiteConditionnement,
        uniteConditionnement = uniteConditionnement,
        typeDispositif = typeDispositif
    )

    fun createTreatment(
        id: Int = 0,
        userId: Int = 1,
        cipRef: String = "3400930000001",
        startDate: Date = Date(),
        endDate: Date? = null
    ) = UserTreatment(
        id = id,
        userId = userId,
        cipRef = cipRef,
        startDate = startDate,
        endDate = endDate
    )

    fun createReminder(
        id: Long = 0,
        treatmentId: Int = 1,
        timeOfDay: String = "08:00",
        doseQuantity: String = "1 comprimé",
        label: String? = null,
        recurrenceInterval: Int = 1,
        recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAY
    ) = TreatmentReminder(
        id = id,
        treatmentId = treatmentId,
        timeOfDay = timeOfDay,
        doseQuantity = doseQuantity,
        label = label,
        recurrenceInterval = recurrenceInterval,
        recurrenceUnit = recurrenceUnit
    )
}
