package medicapp.server.infrastructure.db.constants

import java.nio.file.Path
import kotlin.io.path.div

class CsvPaths(private val dataDir: Path) {
    val CIS_PATH: Path get() = dataDir / "CIS.csv"
    val CIP_PATH: Path get() = dataDir / "CIP.csv"
    val CORRESPONDANCE_CIP_CIS_PATH: Path get() = dataDir / "Correspondance-UCD-CIP-CIS.csv"
    val VOIES_PATH: Path get() = dataDir / "Voies.csv"
    val GROUPE_GENERIQUE_PATH: Path get() = dataDir / "GroupeGenerique.csv"
    val CORRESPONDANCE_GROUPE_GENERIQUE_CIS_PATH: Path get() = dataDir / "Correspondance-GroupeGenerique-CIS.csv"
    val COMPOSITION_QUANTITATIVE_PATH: Path get() = dataDir / "Compositions_quantitatives.csv"
    val COMPOSITION_QUALITATIVE_PATH: Path get() = dataDir / "Compositions_qualitatives.csv"
    val ORGANISATION_PATH: Path get() = dataDir / "Organisations.csv"
    val SUBSTANCES_PATH: Path get() = dataDir / "Substances.csv"
    val EVENEMENTS_CIP_PATH: Path get() = dataDir / "Evenements_CIP.csv"
    val EVENEMENTS_CIS_PATH: Path get() = dataDir / "Evenements_CIS.csv"
    val ELEMENTS_PATH: Path get() = dataDir / "Elements.csv"
    val ELEMENTS_CIP_PATH: Path get() = dataDir / "Elements_CIP.csv"
    val DICTIONNAIRE_SMS_PATH: Path get() = dataDir / "ema_sms_substance.csv"
}