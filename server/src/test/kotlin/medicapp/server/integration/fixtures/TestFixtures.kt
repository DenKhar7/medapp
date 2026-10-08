package medicapp.server.integration.fixtures

/**
 * Données de test contenant les valeurs attendues provenant des fichiers CSV de test.
 *
 * Ces valeurs sont dérivées de :
 * - basededonnee/src/main/resources/data_test/README.md
 * - Les fichiers CSV réels dans src/test/resources/data_test/
 */
object TestFixtures {

    // ─────────────────────────────────────────────────────────────
    // CIS (Spécialités) - Données de test
    // ─────────────────────────────────────────────────────────────

    /**
     * Spécialité active avec une seule voie (orale)
     */
    object OxomemazineActive {
        const val CIS = 60035924
        const val LIBELLE = "OXOMEMAZINE VIATRIS 0,33 mg/mL SANS SUCRE, solution buvable édulcorée à l'acésulfame potassique"
        const val CODE_ATC = "R06AD08"
        const val LIBELLE_ATC = "Oxomémazine"
        const val ORGANISATION = "VIATRIS SANTE"
        val VOIES = listOf("orale")
        const val ACTIF = true
    }

    /**
     * Spécialité archivée (AMM retirée)
     */
    object ActonelArchive {
        const val CIS = 60262879
        const val LIBELLE = "ACTONEL 35 mg, comprimé pelliculé"
        const val CODE_ATC = "M05BA07"
        const val LIBELLE_ATC = "Acide risédronique"
        const val ORGANISATION = "BB FARMA SRL"
        const val ACTIF = false
        const val DERNIER_EVENEMENT = "Archivage de l'autorisation"
    }

    /**
     * Spécialité avec plusieurs voies d'administration
     */
    object MethotrexateMultiVoies {
        const val CIS = 67217445
        const val LIBELLE = "METHOTREXATE VIATRIS 100 mg/ml, solution injectable"
        const val CODE_ATC = "L01BA01"
        const val LIBELLE_ATC = "Méthotrexate"
        val VOIES = listOf("sous-cutanée", "intramusculaire", "intra-artérielle", "intraveineuse")
        const val NB_PRESENTATIONS = 2
    }

    /**
     * Spécialité non prescriptible en DC avec plusieurs substances
     */
    object Np100MultiSubstances {
        const val CIS = 60208447
        const val LIBELLE = "NP100 PREMATURES AP-HP, solution pour perfusion"
        const val CODE_ATC = "B05BA10"
        const val NB_SUBSTANCES_MIN = 20 // Au moins 20 substances
    }

    /**
     * CIS inexistant pour tests négatifs
     */
    const val CIS_INEXISTANT = 99999999

    // ─────────────────────────────────────────────────────────────
    // CIP (Présentations) - Données de test
    // ─────────────────────────────────────────────────────────────

    /**
     * Présentation avec toutes les données renseignées
     */
    object OxomemazinePresentation {
        const val CIP13 = "3400949215539"
        const val CIP7 = "4921553"
        const val LIBELLE = "OXOMEMAZINE VIATRIS 0,33 mg/mL SANS SUCRE, solution buvable édulcorée à l'acésulfame potassique - 1 flacon(s) en verre brun de 150 ml avec gobelet(s) doseur(s) polypropylène"
        const val CIS = 60035924
        const val QUANTITE_CONDITIONNEMENT = 150.0
        const val UNITE_CONDITIONNEMENT = "mL"
        const val NB_UNITE_DISP = 1
        const val TYPE_DISPOSITIF = "avec gobelet(s) doseur(s)"
    }

    /**
     * Présentation sans CIP7
     */
    object BleomycinePresentation {
        const val CIP13 = "3400930116579"
        val CIP7: String? = null
        const val CIS = 60033389
    }

    /**
     * CIP inexistant pour tests négatifs
     */
    const val CIP_INEXISTANT = "9999999999999"

    // ─────────────────────────────────────────────────────────────
    // Substances - Données de test
    // ─────────────────────────────────────────────────────────────

    object SubstanceOxomemazine {
        const val CODE = "02543"
        const val LIBELLE = "oxomémazine"
        const val CODE_SMS = "100000083073"
    }

    object SubstanceMethotrexate {
        const val CODE = "02345"
        const val LIBELLE = "méthotrexate"
    }

    /**
     * Substance detail avec données SMS vérifiées dans CSV
     */
    object SubstanceDetailOxomemazine {
        const val TYPE_SUBSTANCE = "Chemical"
        const val FORMULE_MOLECULAIRE = "C18H22N2O2S"
        const val POID_MOLECULAIRE = 330.45f
    }

    /**
     * Code substance inexistant pour tests négatifs
     */
    const val CODE_SUBSTANCE_INEXISTANT = "99999"

    // ─────────────────────────────────────────────────────────────
    // Événements - Données de test
    // ─────────────────────────────────────────────────────────────

    object EvenementCisActonel {
        const val CIS = 60262879
        const val TYPE_EVENEMENT_RECENT = "Archivage de l'autorisation"
        const val DATE_EFFET = "2018-05-17"
        const val NB_EVENEMENTS = 2
    }

    object EvenementCipOxomemazine {
        const val CIP13 = "3400949215539"
        const val TYPE_EVENEMENT = "Déclaration de commercialisation"
        const val DATE_EFFET = "2011-08-26"
    }

    /**
     * Présentation avec plusieurs événements (3 dans le CSV)
     */
    object CosmogenMultiEvents {
        const val CIP13 = "3400956434299"
        const val NB_EVENEMENTS = 3
        const val TYPE_EVENEMENT_RECENT = "Déclaration de commercialisation"
    }

    // ─────────────────────────────────────────────────────────────
    // Statut de commercialisation - Données de test
    // ─────────────────────────────────────────────────────────────

    /** CIS actif avec Autorisation => marketed = true */
    const val CIS_MARKETED = 60035924

    /** CIS archivé => marketed = false */
    const val CIS_NOT_MARKETED = 60262879

    /** CIP commercialisé => marketed = true */
    const val CIP_MARKETED = "3400949215539"

    /** CIP non commercialisé => marketed = false (arrêt de commercialisation) */
    const val CIP_NOT_MARKETED = "3400949020539"

    // ─────────────────────────────────────────────────────────────
    // Recherche - Données de test
    // ─────────────────────────────────────────────────────────────

    /** Recherche partielle qui doit retourner au moins 1 résultat */
    const val SEARCH_TERM_WITH_RESULTS = "OXOMEMAZINE"

    /** Recherche qui ne retourne aucun résultat */
    const val SEARCH_TERM_NO_RESULTS = "ZZZZNONEXISTENT"

    /** Terme de recherche qui retourne plusieurs résultats */
    const val SEARCH_TERM_MULTIPLE_RESULTS = "mg"
}
