package medicapp.server.infrastructure.db.requests.repository

import medicapp.server.config.dbViewQuery
import medicapp.server.domain.businessclass.medicament.MedicamentLibelleCompletion
import medicapp.server.domain.businessclass.medicament.MedicamentPresDetail
import medicapp.server.domain.businessclass.medicament.MedicamentPresStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSpeResume
import medicapp.server.domain.businessclass.medicament.MedicamentSpeStatus
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceDetail
import medicapp.server.domain.businessclass.medicament.MedicamentSubstanceResume
import medicapp.server.domain.repository.MedicamentRepository
import medicapp.server.infrastructure.db.tables.views.DictionnaireSmsTable
import medicapp.server.infrastructure.db.tables.views.MedCompositionQualitativeTable
import medicapp.server.infrastructure.db.tables.views.MedCompositionQuantitativeTable
import medicapp.server.infrastructure.db.tables.views.MedEvenementCipTable
import medicapp.server.infrastructure.db.tables.views.MedEvenementCisTable
import medicapp.server.infrastructure.db.tables.views.MedOrganisationTable
import medicapp.server.infrastructure.db.tables.views.MedPresentationTable
import medicapp.server.infrastructure.db.tables.views.MedSpecialiteTable
import medicapp.server.infrastructure.db.tables.views.MedSpecialiteVoieTable
import medicapp.server.infrastructure.db.tables.views.MedSubstanceTable
import medicapp.server.infrastructure.db.tables.views.MedVoieTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.LikePattern
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

/**
 * Motif LIKE "contient [text]" où `%`, `_` et `\` saisis par l'utilisateur sont traités littéralement
 * (sinon `libelle=%` ou `libelle=_` forcent un parcours complet de la table).
 */
internal fun containsPattern(text: String): LikePattern {
    val escaped = text
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return LikePattern("%$escaped%", '\\')
}

class MedicamentRepositoryMariaDb : MedicamentRepository {

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSpeResume
    // ─────────────────────────────────────────────────────────────

    override suspend fun getSpeResumeByCis(cis: Int): MedicamentSpeResume? = dbViewQuery {
        val rows = (MedSpecialiteTable
            .join(
                MedOrganisationTable,
                JoinType.LEFT,
                onColumn = MedSpecialiteTable.titulaireId,
                otherColumn = MedOrganisationTable.identifiant
            )
            .leftJoin(MedSpecialiteVoieTable)
            .leftJoin(MedVoieTable))
            .selectAll()
            .where { MedSpecialiteTable.cis eq cis }

        val voies = rows.map { it[MedVoieTable.libelle] }.distinct()

        val row = rows.firstOrNull() ?: return@dbViewQuery null

        return@dbViewQuery MedicamentSpeResume(
            cis = row[MedSpecialiteTable.cis],
            nom = row[MedSpecialiteTable.libelle],
            nomOrganisation = row[MedOrganisationTable.libelle],
            codeAtc = row[MedSpecialiteTable.codeAtc],
            libelleAtc = row[MedSpecialiteTable.libelleAtc],
            voies = voies
        )
    }

    override suspend fun searchSpeResumeByLibelle(libelle: String, limit: Int): List<MedicamentSpeResume> =
        dbViewQuery {
            val rows = (
                    MedSpecialiteTable
                        .join(
                            MedOrganisationTable,
                            JoinType.LEFT,
                            onColumn = MedSpecialiteTable.titulaireId,
                            otherColumn = MedOrganisationTable.identifiant
                        )
                        .leftJoin(MedSpecialiteVoieTable)
                        .leftJoin(MedVoieTable)
                    )
                .selectAll()
                .where { MedSpecialiteTable.libelle like containsPattern(libelle) }
                .limit(limit)
                .toList()

            rows
                .groupBy { it[MedSpecialiteTable.cis] }
                .map { (_, group) ->
                    val first = group.first()

                    val medicamentSpeResume = MedicamentSpeResume(
                        cis = first[MedSpecialiteTable.cis],
                        nom = first[MedSpecialiteTable.libelle],
                        nomOrganisation = first.getOrNull(MedOrganisationTable.libelle),
                        codeAtc = first[MedSpecialiteTable.codeAtc],
                        libelleAtc = first[MedSpecialiteTable.libelleAtc],
                        voies = group
                            .mapNotNull { it.getOrNull(MedVoieTable.libelle) }
                            .distinct()
                    )
                    medicamentSpeResume
                }
        }

    override suspend fun getLibelleCompletion(libelleExtract: String, limit: Int): List<MedicamentLibelleCompletion> =
        dbViewQuery {
            val rows = MedSpecialiteTable.select(
                MedSpecialiteTable.cis,
                MedSpecialiteTable.libelle
            )
                .where { MedSpecialiteTable.libelle like containsPattern(libelleExtract) }
                .limit(limit)

            rows
                .groupBy { it[MedSpecialiteTable.cis] }
                .map { (_, group) ->
                    val first = group.first()
                    MedicamentLibelleCompletion(
                        cis = first[MedSpecialiteTable.cis],
                        libelle = first[MedSpecialiteTable.libelle]
                    )
                }
        }

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentPresDetail
    // ─────────────────────────────────────────────────────────────

    override suspend fun getAllPresDetailByCis(cis: Int): List<MedicamentPresDetail> =
        dbViewQuery {
            val rows = (
                    MedPresentationTable
                        .innerJoin(MedSpecialiteTable)
                        .join(
                            MedOrganisationTable,
                            JoinType.LEFT,
                            onColumn = MedSpecialiteTable.titulaireId,
                            otherColumn = MedOrganisationTable.identifiant
                        )
                        .leftJoin(MedSpecialiteVoieTable)
                        .leftJoin(MedVoieTable)
                    )
                .selectAll()
                .where { MedPresentationTable.cis eq cis }
                .toList()

            // Groupement métier : une présentation (CIP13) peut avoir plusieurs voies
            rows
                .groupBy { it[MedPresentationTable.cip13] }
                .map { (_, group) ->
                    val first = group.first()

                    MedicamentPresDetail(
                        cip13 = first[MedPresentationTable.cip13],
                        label = first[MedPresentationTable.libelle],

                        cis = first[MedSpecialiteTable.cis],
                        nomSpecialite = first[MedSpecialiteTable.libelle],
                        codeAtc = first[MedSpecialiteTable.codeAtc],
                        libelleAtc = first[MedSpecialiteTable.libelleAtc],

                        nomOrganisation = first.getOrNull(MedOrganisationTable.libelle),

                        voie = group
                            .mapNotNull { it.getOrNull(MedVoieTable.libelle) }
                            .distinct(),

                        // Champs optionnels / conditionnels
                        dosesParBoite = first.getOrNull(MedPresentationTable.nbUniteDisp),
                        quantiteConditionnement = first.getOrNull(MedPresentationTable.quantiteConditionnement)
                            ?.toDouble(),
                        uniteConditionnement = first.getOrNull(MedPresentationTable.uniteConditionnement),
                        typeDispositif = first.getOrNull(MedPresentationTable.typeDispositif),
                    )
                }
        }

    override suspend fun getPresDetailByCip(cip13: String): MedicamentPresDetail? =
        dbViewQuery {
            val rows = (
                    MedPresentationTable
                        .innerJoin(MedSpecialiteTable)
                        .join(
                            MedOrganisationTable,
                            JoinType.LEFT,
                            onColumn = MedSpecialiteTable.titulaireId,
                            otherColumn = MedOrganisationTable.identifiant
                        )
                        .leftJoin(MedSpecialiteVoieTable)
                        .leftJoin(MedVoieTable)
                    )
                .selectAll()
                .where { MedPresentationTable.cip13 eq cip13 }

            val voies = rows.map { it[MedVoieTable.libelle] }.distinct()

            val row = rows.firstOrNull() ?: return@dbViewQuery null

            return@dbViewQuery MedicamentPresDetail(
                cip13 = row[MedPresentationTable.cip13],
                label = row[MedPresentationTable.libelle],
                nomSpecialite = row[MedSpecialiteTable.libelle],
                cis = row[MedPresentationTable.cis],
                nomOrganisation = row[MedOrganisationTable.libelle],
                codeAtc = row[MedSpecialiteTable.codeAtc],
                libelleAtc = row[MedSpecialiteTable.libelleAtc],
                voie = voies,
                dosesParBoite = row[MedPresentationTable.nbUniteDisp],
                quantiteConditionnement = row[MedPresentationTable.quantiteConditionnement]?.toDouble(),
                uniteConditionnement = row[MedPresentationTable.uniteConditionnement],
                typeDispositif = row[MedPresentationTable.typeDispositif]
            )
        }

    override suspend fun searchPresDetailByLibelle(libelle: String, limit: Int): List<MedicamentPresDetail> =
        dbViewQuery {
            val rows = (
                    MedPresentationTable
                        .innerJoin(MedSpecialiteTable)
                        .join(
                            MedOrganisationTable,
                            JoinType.LEFT,
                            onColumn = MedSpecialiteTable.titulaireId,
                            otherColumn = MedOrganisationTable.identifiant
                        )
                        .leftJoin(MedSpecialiteVoieTable)
                        .leftJoin(MedVoieTable)
                    )
                .selectAll()
                .where { MedPresentationTable.libelle like containsPattern(libelle) }
                .limit(limit)
                .toList()

            // Groupement métier : une présentation (CIP13) peut avoir plusieurs voies
            rows
                .groupBy { it[MedPresentationTable.cip13] }
                .map { (_, group) ->
                    val first = group.first()

                    MedicamentPresDetail(
                        cip13 = first[MedPresentationTable.cip13],
                        label = first[MedPresentationTable.libelle],

                        cis = first[MedSpecialiteTable.cis],
                        nomSpecialite = first[MedSpecialiteTable.libelle],
                        codeAtc = first[MedSpecialiteTable.codeAtc],
                        libelleAtc = first[MedSpecialiteTable.libelleAtc],

                        nomOrganisation = first.getOrNull(MedOrganisationTable.libelle),

                        voie = group
                            .mapNotNull { it.getOrNull(MedVoieTable.libelle) }
                            .distinct(),

                        // Champs optionnels / conditionnels
                        dosesParBoite = first.getOrNull(MedPresentationTable.nbUniteDisp),
                        quantiteConditionnement = first.getOrNull(MedPresentationTable.quantiteConditionnement)
                            ?.toDouble(),
                        uniteConditionnement = first.getOrNull(MedPresentationTable.uniteConditionnement),
                        typeDispositif = first.getOrNull(MedPresentationTable.typeDispositif),
                    )
                }
        }

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSubstanceResume
    // ─────────────────────────────────────────────────────────────

    override suspend fun getSubstancesResumeByCis(cis: Int): List<MedicamentSubstanceResume> =
        dbViewQuery {
            val rows = (
                    MedCompositionQualitativeTable
                        .join(
                            MedCompositionQuantitativeTable,
                            JoinType.LEFT,
                            additionalConstraint = {
                                (MedCompositionQualitativeTable.codeSubstance eq MedCompositionQuantitativeTable.codeSubstance)
                                (MedCompositionQualitativeTable.cis eq MedCompositionQuantitativeTable.cis) and
                                        (MedCompositionQualitativeTable.identifiantElement eq MedCompositionQuantitativeTable.identifiantElement)
                            }
                        )
                        .join(
                            MedSubstanceTable,
                            JoinType.INNER,
                            MedCompositionQualitativeTable.codeSubstance,
                            MedSubstanceTable.codeSubstance
                        )
                    )
                .selectAll()
                .where {
                    (MedCompositionQualitativeTable.cis eq cis)
                }
                .toList()

            rows
                .groupBy { it[MedCompositionQualitativeTable.codeSubstance] }
                .map { (_, group) ->
                    val first = group.first()
                    MedicamentSubstanceResume(
                        codeSubstance = first[MedSubstanceTable.codeSubstance],
                        libelle = first[MedSubstanceTable.libelleFr],
                        relationSubstance = first[MedCompositionQualitativeTable.relationSubstance],
                        expressionQuantite = first[MedCompositionQuantitativeTable.expressionQuantite],
                        substanceActive = first[MedCompositionQuantitativeTable.substanceActive],
                        fractionTherapeutique = first[MedCompositionQuantitativeTable.fractionTherapeutique]
                    )
                }
        }

    override suspend fun getSubstanceResumeByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceResume? =
        dbViewQuery {
            val row = (
                    MedCompositionQualitativeTable
                        .join(
                            MedCompositionQuantitativeTable,
                            JoinType.LEFT,
                            additionalConstraint = {
                                (MedCompositionQualitativeTable.codeSubstance eq MedCompositionQuantitativeTable.codeSubstance)
                                (MedCompositionQualitativeTable.cis eq MedCompositionQuantitativeTable.cis) and
                                        (MedCompositionQualitativeTable.identifiantElement eq MedCompositionQuantitativeTable.identifiantElement)
                            }
                        )
                        .join(
                            MedSubstanceTable,
                            JoinType.INNER,
                            MedCompositionQualitativeTable.codeSubstance,
                            MedSubstanceTable.codeSubstance
                        )
                    )
                .selectAll()
                .where {
                    (MedSubstanceTable.codeSubstance eq codeSubstance) and
                            (MedCompositionQualitativeTable.cis eq cis)
                }
                .firstOrNull() ?: return@dbViewQuery null

            MedicamentSubstanceResume(
                codeSubstance = row[MedSubstanceTable.codeSubstance],
                libelle = row[MedSubstanceTable.libelleFr],
                relationSubstance = row[MedCompositionQualitativeTable.relationSubstance],
                expressionQuantite = row[MedCompositionQuantitativeTable.expressionQuantite],
                substanceActive = row[MedCompositionQuantitativeTable.substanceActive],
                fractionTherapeutique = row[MedCompositionQuantitativeTable.fractionTherapeutique]
            )
        }

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentSubstanceDetail
    // ─────────────────────────────────────────────────────────────

    override suspend fun getSubstancesDetailByCis(cis: Int): List<MedicamentSubstanceDetail> =
        dbViewQuery {
            val rows = MedCompositionQualitativeTable
                .join(
                    MedSubstanceTable,
                    JoinType.INNER,
                    onColumn = MedCompositionQualitativeTable.codeSubstance,
                    otherColumn = MedSubstanceTable.codeSubstance
                )
                .join(
                    DictionnaireSmsTable,
                    JoinType.INNER,
                    onColumn = MedSubstanceTable.codeSms,
                    otherColumn = DictionnaireSmsTable.codeSms
                )
                .select(
                    MedCompositionQualitativeTable.codeSubstance,
                    DictionnaireSmsTable.nomSubstance,
                    DictionnaireSmsTable.typeSubstance,
                    DictionnaireSmsTable.formuleMoleculaire,
                    DictionnaireSmsTable.poidMoleculaire
                )
                .where {
                    MedCompositionQualitativeTable.cis eq cis
                }
                .toList()

            rows
                .groupBy { it[MedCompositionQualitativeTable.codeSubstance] }
                .map { (_, group) ->
                    val first = group.first()
                    MedicamentSubstanceDetail(
                        nomSubstance = first[DictionnaireSmsTable.nomSubstance],
                        typeSubstance = first[DictionnaireSmsTable.typeSubstance],
                        formuleMoleculaire = first[DictionnaireSmsTable.formuleMoleculaire],
                        poidMoleculaire = first[DictionnaireSmsTable.poidMoleculaire]
                    )
                }
        }

    override suspend fun getSubstanceDetailByCisAndCode(cis : Int, codeSubstance: String): MedicamentSubstanceDetail? =
        dbViewQuery {
            val row = MedCompositionQualitativeTable
                .join(
                    MedSubstanceTable,
                    JoinType.INNER,
                    MedCompositionQualitativeTable.codeSubstance,
                    MedSubstanceTable.codeSubstance
                )
                .join(
                    DictionnaireSmsTable,
                    JoinType.LEFT,
                    MedSubstanceTable.codeSms,
                    DictionnaireSmsTable.codeSms
                )
                .select(
                    DictionnaireSmsTable.nomSubstance,
                    DictionnaireSmsTable.typeSubstance,
                    DictionnaireSmsTable.formuleMoleculaire,
                    DictionnaireSmsTable.poidMoleculaire
                )
                .where {
                    (MedSubstanceTable.codeSubstance eq codeSubstance) and
                            (MedCompositionQualitativeTable.cis eq cis)
                }
                .firstOrNull() ?: return@dbViewQuery null

            MedicamentSubstanceDetail(
                nomSubstance = row[DictionnaireSmsTable.nomSubstance],
                typeSubstance = row[DictionnaireSmsTable.typeSubstance],
                formuleMoleculaire = row[DictionnaireSmsTable.formuleMoleculaire],
                poidMoleculaire = row[DictionnaireSmsTable.poidMoleculaire]
            )
        }

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentStatusCip
    // ─────────────────────────────────────────────────────────────

    override suspend fun getLatestEventByCip(cip13: String): MedicamentPresStatus? =
        dbViewQuery {
            val row = MedEvenementCipTable
                .selectAll()
                .where { MedEvenementCipTable.cip13 eq cip13 }
                .orderBy(MedEvenementCipTable.dateEffet, SortOrder.DESC)
                .firstOrNull() ?: return@dbViewQuery null

            MedicamentPresStatus(
                cip13 = cip13,
                dateEffet = row[MedEvenementCipTable.dateEffet],
                typeEvenement = row[MedEvenementCipTable.typeEvenement]
            )
        }


    override suspend fun getAllEventsByCip(cip13: String): List<MedicamentPresStatus> =
        dbViewQuery {
            val rows = MedEvenementCipTable
                .select(
                    MedEvenementCipTable.evenement,
                    MedEvenementCipTable.cip13,
                    MedEvenementCipTable.dateEffet,
                    MedEvenementCipTable.typeEvenement
                )
                .where { MedEvenementCipTable.cip13 eq cip13 }

            rows
                .groupBy { it[MedEvenementCipTable.evenement] }
                .map { (_, group) ->
                    val first = group.first()
                    MedicamentPresStatus(
                        cip13 = cip13,
                        dateEffet = first[MedEvenementCipTable.dateEffet],
                        typeEvenement = first[MedEvenementCipTable.typeEvenement]
                    )
                }
        }

    override suspend fun isPresMarketed(cip13: String): Boolean {
        return this.getLatestEventByCip(cip13)?.typeEvenement == "Déclaration de commercialisation"
    }

    // ─────────────────────────────────────────────────────────────
    // Méthodes associées à la data class MedicamentStatusCis
    // ─────────────────────────────────────────────────────────────

    override suspend fun getLatestEventByCis(cis: Int): MedicamentSpeStatus? =
        dbViewQuery {
            val row = MedEvenementCisTable
                .select(
                    MedEvenementCisTable.cis,
                    MedEvenementCisTable.dateEffet,
                    MedEvenementCisTable.typeEvenement
                )
                .where { MedEvenementCisTable.cis eq cis }
                .orderBy(MedEvenementCisTable.dateEffet, SortOrder.DESC)
                .firstOrNull() ?: return@dbViewQuery null

            MedicamentSpeStatus(
                cis = cis,
                dateEffet = row[MedEvenementCisTable.dateEffet],
                typeEvenement = row[MedEvenementCisTable.typeEvenement]
            )
        }

    override suspend fun getAllEventsByCis(cis: Int): List<MedicamentSpeStatus> =
        dbViewQuery {
            val rows = MedEvenementCisTable
                .select(
                    MedEvenementCisTable.evenement,
                    MedEvenementCisTable.cis,
                    MedEvenementCisTable.dateEffet,
                    MedEvenementCisTable.typeEvenement
                )
                .where { MedEvenementCisTable.cis eq cis }

            rows
                .groupBy { it[MedEvenementCisTable.evenement] }
                .map { (_, group) ->
                    val first = group.first()
                    MedicamentSpeStatus(
                        cis = cis,
                        dateEffet = first[MedEvenementCisTable.dateEffet],
                        typeEvenement = first[MedEvenementCisTable.typeEvenement]
                    )
                }
        }

    override suspend fun isSpeMarketed(cis: Int): Boolean {
        return this.getLatestEventByCis(cis)?.typeEvenement == "Autorisation"
    }
}