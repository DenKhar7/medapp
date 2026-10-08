package medicapp.server.infrastructure.db.enums

import medicapp.server.infrastructure.db.constants.CsvPaths
import medicapp.server.infrastructure.csv.importers.CompositionQualitativeImporter
import medicapp.server.infrastructure.csv.importers.CompositionQuantitativeImporter
import medicapp.server.infrastructure.csv.importers.ElementPresentationImporter
import medicapp.server.infrastructure.csv.importers.ElementSpecialiteImporter
import medicapp.server.infrastructure.csv.importers.EvenementCipImporter
import medicapp.server.infrastructure.csv.importers.EvenementCisImporter
import medicapp.server.infrastructure.csv.importers.GroupeGeneriqueImporter
import medicapp.server.infrastructure.csv.importers.GroupeGeneriqueMembreImporter
import medicapp.server.infrastructure.csv.importers.OrganisationImporter
import medicapp.server.infrastructure.csv.importers.PresentationImporter
import medicapp.server.infrastructure.csv.importers.SpecialiteImporter
import medicapp.server.infrastructure.csv.importers.SpecialiteVoieImporter
import medicapp.server.infrastructure.csv.importers.SubstanceImporter
import medicapp.server.infrastructure.csv.importers.VoieImporter

import java.sql.Connection

enum class TableEnum(
    val tableName: String,
    val source: DataSourceEnum,
    val sqlScriptCreate: String,
    val order: Int?,
    val importer: ((CsvPaths, Connection) -> Unit)? = null
)
{

    // ===== RUIM =====
    ORGANISATION(
        "med_organisation",
        DataSourceEnum.RUIM,
        "MedOrganisation.sql",
        order = 10,
        importer = { csvPaths, conn -> OrganisationImporter.run(csvPaths.ORGANISATION_PATH.toFile(), conn) }
    ),

    SUBSTANCE(
        "med_substance",
        DataSourceEnum.RUIM,
        "MedSubstance.sql",
        order = 20,
        importer = { csvPaths, conn -> SubstanceImporter.run(csvPaths.SUBSTANCES_PATH.toFile(), conn) }
    ),

    VOIE(
        "med_voie",
        DataSourceEnum.RUIM,
        "MedVoie.sql",
        order = 30,
        importer = { csvPaths, conn -> VoieImporter.run(csvPaths.VOIES_PATH.toFile(), conn) }
    ),

    SPECIALITE(
        "med_specialite",
        DataSourceEnum.RUIM,
        "MedSpecialite.sql",
        order = 40,
        importer = { csvPaths, conn -> SpecialiteImporter.run(csvPaths.CIS_PATH.toFile(), conn) }
    ),

    PRESENTATION(
        "med_presentation",
        DataSourceEnum.RUIM,
        "MedPresentation.sql",
        order = 50,
        importer = { csvPaths, conn ->
            PresentationImporter.run(
                csvPaths.CIP_PATH.toFile(),
                csvPaths.CORRESPONDANCE_CIP_CIS_PATH.toFile(),
                conn
            )
        }
    ),

    ELEMENT_SPECIALITE(
        "med_element_specialite",
        DataSourceEnum.RUIM,
        "MedElementSpecialite.sql",
        order = 60,
        importer = { csvPaths, conn -> ElementSpecialiteImporter.run(csvPaths.ELEMENTS_PATH.toFile(), conn) }
    ),

    ELEMENT_PRESENTATION(
        "med_element_presentation",
        DataSourceEnum.RUIM,
        "MedElementPresentation.sql",
        order = 70,
        importer = { csvPaths, conn -> ElementPresentationImporter.run(csvPaths.ELEMENTS_CIP_PATH.toFile(), conn) }
    ),

    COMPOSITION_QUALITATIVE(
        "med_composition_qualitative",
        DataSourceEnum.RUIM,
        "MedCompositionQualitative.sql",
        order = 80,
        importer = { csvPaths, conn ->
            CompositionQualitativeImporter.run(csvPaths.COMPOSITION_QUALITATIVE_PATH.toFile(), conn)
        }
    ),

    COMPOSITION_QUANTITATIVE(
        "med_composition_quantitative",
        DataSourceEnum.RUIM,
        "MedCompositionQuantitative.sql",
        order = 90,
        importer = { csvPaths, conn ->
            CompositionQuantitativeImporter.run(csvPaths.COMPOSITION_QUANTITATIVE_PATH.toFile(), conn)
        }
    ),

    SPECIALITE_VOIE(
        "med_specialite_voie",
        DataSourceEnum.RUIM,
        "MedSpecialiteVoie.sql",
        order = 100,
        importer = { csvPaths, conn -> SpecialiteVoieImporter.run(csvPaths.CIS_PATH.toFile(), conn) }
    ),

    GROUPE_GENERIQUE(
        "med_groupe_generique",
        DataSourceEnum.RUIM,
        "MedGroupeGenerique.sql",
        order = 110,
        importer = { csvPaths, conn -> GroupeGeneriqueImporter.run(csvPaths.GROUPE_GENERIQUE_PATH.toFile(), conn) }
    ),

    GROUPE_GENERIQUE_MEMBRE(
        "med_groupe_generique_membre",
        DataSourceEnum.RUIM,
        "MedGroupeGeneriqueMembre.sql",
        order = 120,
        importer = { csvPaths, conn ->
            GroupeGeneriqueMembreImporter.run(
                csvPaths.CORRESPONDANCE_GROUPE_GENERIQUE_CIS_PATH.toFile(),
                conn
            )
        }
    ),

    EVENEMENT_CIP(
        "med_evenement_cip",
        DataSourceEnum.RUIM,
        "MedEvenementCip.sql",
        order = 130,
        importer = { csvPaths, conn -> EvenementCipImporter.run(csvPaths.EVENEMENTS_CIP_PATH.toFile(), conn) }
    ),

    EVENEMENT_CIS(
        "med_evenement_cis",
        DataSourceEnum.RUIM,
        "MedEvenementCis.sql",
        order = 140,
        importer = { csvPaths, conn -> EvenementCisImporter.run(csvPaths.EVENEMENTS_CIS_PATH.toFile(), conn) }
    ),


    // ===== SMS =====
    DICTIONNAIRE_SMS(
        "dictionnaire_sms",
        DataSourceEnum.SMS,
        "DictionnaireSms.sql",
        order = null
    ),
}
