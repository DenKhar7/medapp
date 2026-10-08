package medicapp.server.domain.service

import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceDetail
import medicapp.server.domain.businessclass.medicament.MedicamentLibelleCompletion
import medicapp.server.domain.businessclass.medicament.MedicamentPresDetail
import medicapp.server.domain.businessclass.medicament.MedicamentSpeResume
import medicapp.server.domain.businessclass.medicament.MedicamentPresStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSpeStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceResume
import medicapp.server.domain.repository.MedicamentRepository


class MedicamentService(
    private val repository: MedicamentRepository
) {


    // ─────────────────────────────────────────────────────────────
    // Spécialités (CIS)
    // ─────────────────────────────────────────────────────────────


    suspend fun getSpeResumeByCis(cis: Int): MedicamentSpeResume? =
        repository.getSpeResumeByCis(cis)


    suspend fun searchSpeResumeByLibelle(
        libelle: String,
        limit: Int = 10
    ): List<MedicamentSpeResume> =
        if (libelle.isBlank()) emptyList()
        else repository.searchSpeResumeByLibelle(libelle, limit)


    suspend fun getLibelleCompletion(
        libelleExtract: String,
        limit: Int = 10
    ): List<MedicamentLibelleCompletion> =
        if (libelleExtract.isBlank()) emptyList()
        else repository.getLibelleCompletion(libelleExtract, limit)


    suspend fun isSpeMarketed(cis: Int): Boolean =
        repository.isSpeMarketed(cis)


// ─────────────────────────────────────────────────────────────
// Présentations (CIP)
// ─────────────────────────────────────────────────────────────


    suspend fun getAllPresDetailByCis(cis: Int): List<MedicamentPresDetail> =
        repository.getAllPresDetailByCis(cis)


    suspend fun getPresDetailByCip(cip13: String): MedicamentPresDetail? =
        repository.getPresDetailByCip(cip13)


    suspend fun searchPresDetailByLibelle(
        libelle: String,
        limit: Int = 10
    ): List<MedicamentPresDetail> =
        if (libelle.isBlank()) emptyList()
        else repository.searchPresDetailByLibelle(libelle, limit)


    suspend fun isPresMarketed(cip13: String): Boolean =
        repository.isPresMarketed(cip13)


// ─────────────────────────────────────────────────────────────
// Substances
// ─────────────────────────────────────────────────────────────


    suspend fun getSubstancesResumeByCis(cis: Int): List<MedicamentSubstanceResume> =
        repository.getSubstancesResumeByCis(cis)


    suspend fun getSubstanceResumeByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceResume? =
        repository.getSubstanceResumeByCisAndCode(cis, codeSubstance)


    suspend fun getSubstancesDetailByCis(cis: Int): List<MedicamentSubstanceDetail> =
        repository.getSubstancesDetailByCis(cis)


    suspend fun getSubstanceDetailByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceDetail? =
        repository.getSubstanceDetailByCisAndCode(cis, codeSubstance)


// ─────────────────────────────────────────────────────────────
// Événements réglementaires
// ─────────────────────────────────────────────────────────────


    suspend fun getLatestEventByCip(cip13: String): MedicamentPresStatus? =
        repository.getLatestEventByCip(cip13)

    suspend fun getAllEventsByCip(cip13: String): List<MedicamentPresStatus> =
        repository.getAllEventsByCip(cip13)

    suspend fun getLatestEventByCis(cis: Int): MedicamentSpeStatus? =
        repository.getLatestEventByCis(cis)

    suspend fun getAllEventsByCis(cis: Int): List<MedicamentSpeStatus> =
        repository.getAllEventsByCis(cis)
}
