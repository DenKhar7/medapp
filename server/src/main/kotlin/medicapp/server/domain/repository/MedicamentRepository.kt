package medicapp.server.domain.repository

import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceDetail
import medicapp.server.domain.businessclass.medicament.MedicamentLibelleCompletion
import medicapp.server.domain.businessclass.medicament.MedicamentPresDetail
import medicapp.server.domain.businessclass.medicament.MedicamentSpeResume
import medicapp.server.domain.businessclass.medicament.MedicamentPresStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSpeStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceResume

interface MedicamentRepository {

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSpeResume
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère le résumé métier d’une spécialité pharmaceutique
     * à partir de son code CIS (identifiant ANSM).
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Le résumé de la spécialité si elle existe, null sinon.
     */
    suspend fun getSpeResumeByCis(cis: Int): MedicamentSpeResume?

    /**
     * Recherche des spécialités pharmaceutiques à partir de leur
     * libellé (nom commercial).
     *
     * La recherche est de type textuelle (partielle) et peut
     * retourner plusieurs résultats.
     * Utilisée pour : OCR, recherche utilisateur, ajout manuel.
     *
     * @param libelle Libellé ou fragment de libellé recherché.
     * @param limit Nombre maximum de résultats retournés.
     * @return Liste de spécialités dont le libellé correspond
     * partiellement au paramètre fourni.
     */
    suspend fun searchSpeResumeByLibelle(
        libelle: String,
        limit: Int = 10
    ): List<MedicamentSpeResume>

    /**
     * Fournit une liste de suggestions de spécialités pharmaceutiques
     * pour des fonctionnalités de complétion automatique
     * (ex. barre de recherche).
     *
     * La recherche est basée sur un fragment de libellé et retourne
     * des résultats partiels.
     *
     * @param libelleExtract Fragment de libellé saisi par l’utilisateur.
     * @param limit Nombre maximum de résultats retournés.
     * @return Liste de suggestions de libellés de spécialités
     * correspondant partiellement au fragment fourni.
     */
    suspend fun getLibelleCompletion(
        libelleExtract: String,
        limit: Int = 10
    ): List<MedicamentLibelleCompletion>

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentPresDetail
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère la liste des présentations pharmaceutiques
     * associées à une spécialité, à partir de son code CIS
     * (identifiant ANSM).
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Liste de présentations pharmaceutiques (éventuellement vide).
     */
    suspend fun getAllPresDetailByCis(cis: Int): List<MedicamentPresDetail>

    /**
     * Récupère une présentation pharmaceutique à partir
     * de son code CIP13 (identifiant à 13 chiffres).
     *
     * @param cip13 Code identifiant de la présentation (CIP13).
     * @return Le détail de la présentation si elle existe, null sinon.
     */
    suspend fun getPresDetailByCip(cip13: String): MedicamentPresDetail?

    /**
     * Recherche des présentations pharmaceutiques à partir de leur
     * libellé (nom commercial).
     *
     * La recherche est de type textuelle (partielle) et peut
     * retourner plusieurs résultats.
     * Utilisée pour : OCR, recherche utilisateur, ajout manuel.
     *
     * @param libelle Libellé ou fragment de libellé recherché.
     * @param limit Nombre maximum de résultats retournés.
     * @return Liste de présentations dont le libellé correspond
     * partiellement au paramètre fourni.
     */
    suspend fun searchPresDetailByLibelle(
        libelle: String,
        limit: Int = 10
    ): List<MedicamentPresDetail>

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSubstanceResume
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère la liste des substances associées à une spécialité
     * pharmaceutique, à partir de son code CIS.
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Liste de substances sous forme de résumés.
     */
    suspend fun getSubstancesResumeByCis(cis: Int): List<MedicamentSubstanceResume>

    /**
     * Récupère le résumé d’une substance à partir de son
     * code identifiant de référence.
     *
     * @param codeSubstance Code identifiant de la substance.
     * @return Le résumé de la substance si elle existe, null sinon.
     */
    suspend fun getSubstanceResumeByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceResume?

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSubstanceDetail
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère la liste détaillée des substances associées
     * à une spécialité pharmaceutique, à partir de son code CIS.
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Liste de substances détaillées.
     */
    suspend fun getSubstancesDetailByCis(cis: Int): List<MedicamentSubstanceDetail>

    /**
     * Récupère le détail complet d’une substance à partir
     * de son code identifiant de référence.
     *
     * @param codeSubstance Code identifiant de la substance.
     * @return Le détail de la substance si elle existe, null sinon.
     */
    suspend fun getSubstanceDetailByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceDetail?

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentStatusCip
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère le dernier événement réglementaire
     * pour une présentation pharmaceutique, à partir de son
     * code CIP13.
     *
     * @param cip13 Code identifiant de la présentation (CIP13).
     * @return Le dernier événement réglementaire connu.
     */
    suspend fun getLatestEventByCip(cip13: String): MedicamentPresStatus?

    /**
     * Récupère l’historique complet des événements réglementaires
     * associés à une présentation pharmaceutique.
     *
     * @param cip13 Code identifiant de la présentation (CIP13).
     * @return Liste des événements réglementaires associés.
     */
    suspend fun getAllEventsByCip(cip13 : String): List<MedicamentPresStatus>

    /**
     * Indique si une présentation pharmaceutique est actuellement
     * commercialisée, selon son dernier événement réglementaire connu.
     *
     * @param cip13 Code identifiant de la présentation (CIP13).
     * @return true si la présentation est commercialisée, false sinon.
     */
    suspend fun isPresMarketed(cip13: String): Boolean

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentStatusCis
    // ─────────────────────────────────────────────────────────────

    /**
     * Récupère le dernier événement réglementaire
     * pour une spécialité pharmaceutique, à partir de son
     * code CIS.
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Le dernier événement réglementaire connu.
     */
    suspend fun getLatestEventByCis(cis: Int): MedicamentSpeStatus?

    /**
     * Récupère l’historique complet des événements réglementaires
     * associés à une spécialité pharmaceutique.
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return Liste des événements réglementaires associés.
     */
    suspend fun getAllEventsByCis(cis: Int): List<MedicamentSpeStatus>

    /**
     * Indique si une spécialité pharmaceutique est actuellement
     * commercialisée, selon son dernier événement réglementaire connu.
     *
     * @param cis Code identifiant de la spécialité (CIS).
     * @return true si la spécialité est commercialisée, false sinon.
     */
    suspend fun isSpeMarketed(cis: Int): Boolean
}