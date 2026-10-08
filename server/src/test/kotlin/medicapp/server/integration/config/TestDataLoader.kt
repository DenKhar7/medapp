package medicapp.server.integration.config

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

/**
 * Charge les données de test depuis les fichiers CSV dans la base de données de test.
 *
 * Les fichiers CSV sont situés dans src/test/resources/data_test/
 * Les données sont chargées dans l'ordre des dépendances pour respecter les relations de clés étrangères.
 */
object TestDataLoader {

    private val csvReader = csvReader {
        delimiter = ','
        quoteChar = '"'
        escapeChar = '\\'
    }

    /**
     * Charge toutes les données de test depuis les fichiers CSV dans la base de données.
     */
    fun loadAllTestData(dataSource: DataSource) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                // Charger dans l'ordre des dépendances
                loadOrganisations(conn)
                loadVoies(conn)
                loadSmsDictionnaire(conn)
                loadSubstances(conn)
                loadSpecialites(conn)
                loadCorrespondance(conn) // Nécessaire pour mapper CIP vers CIS
                loadPresentations(conn)
                loadSpecialiteVoies(conn)
                loadCompositionsQualitatives(conn)
                loadCompositionsQuantitatives(conn)
                loadEvenementsCis(conn)
                loadEvenementsCip(conn)
                loadElements(conn)
                loadElementsCip(conn)

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    // Mapping CIS -> CIP chargé depuis le CSV Correspondance
    private val cisCipMap = mutableMapOf<String, Int>()

    private fun loadCorrespondance(conn: Connection) {
        val rows = readCsv("Correspondance-UCD-CIP-CIS.csv")
        rows.forEach { row ->
            val cip13 = row["CIP13"]
            val cis = row["CIS"]?.toIntOrNull()
            if (cip13 != null && cis != null) {
                cisCipMap[cip13] = cis
            }
        }
    }

    private fun loadOrganisations(conn: Connection) {
        val sql = """
            INSERT INTO med_organisation_view (identifiant, uri, libelle, pays)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Organisations.csv")
            val seen = mutableSetOf<String>()
            rows.forEach { row ->
                val id = row["identifiant"] ?: return@forEach
                if (id in seen) return@forEach
                seen.add(id)

                stmt.setString(1, id)
                stmt.setString(2, row["URI"])
                stmt.setString(3, row["libelle"])
                stmt.setString(4, row["nomPays"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadVoies(conn: Connection) {
        val sql = """
            INSERT INTO med_voie_view (identifiant, uri, libelle, code_rms, code_edqm)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Voies.csv")
            rows.forEach { row ->
                stmt.setString(1, row["identifiant"])
                stmt.setString(2, row["URI"])
                stmt.setString(3, row["libelle"])
                stmt.setString(4, row["codeRMS"])
                stmt.setString(5, row["codeEDQM"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadSmsDictionnaire(conn: Connection) {
        val sql = """
            INSERT INTO dictionnaire_sms_view (code_sms, nom_substance, is_terme_pref, source_nom,
                status_substance, type_substance, formule_moleculaire, poid_moleculaire, inchikey, last_update_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE nom_substance = VALUES(nom_substance)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("SMS_Dictionnaire.csv")
            val seen = mutableSetOf<Pair<String, String>>()
            rows.forEach { row ->
                val codeSms = row["#SMS_ID"] ?: return@forEach
                val nomSubstance = row["Substance_Name"] ?: return@forEach
                val key = codeSms to nomSubstance
                if (key in seen) return@forEach
                seen.add(key)

                stmt.setString(1, codeSms)
                stmt.setString(2, nomSubstance)
                stmt.setBoolean(3, row["Is_Preferred_Name"]?.equals("True", ignoreCase = true) ?: false)
                stmt.setString(4, row["Name_Source"])
                stmt.setString(5, row["Status"])
                stmt.setString(6, row["Substance_Type"])
                stmt.setString(7, row["Molecular_Formula"])
                stmt.setObject(8, row["Molecular_Weight"]?.toFloatOrNull())
                stmt.setObject(9, null) // inchikey is float in table but string in CSV - set null
                stmt.setDate(10, parseDate(row["Last_Updated_Date"]?.substringBefore(" ")))
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadSubstances(conn: Connection) {
        val sql = """
            INSERT INTO med_substance_view (code_substance, uri, libelle_fr, code_sms, synonymes_fr)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle_fr = VALUES(libelle_fr)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Substances.csv")
            val seen = mutableSetOf<String>()
            rows.forEach { row ->
                val codeSubstance = row["codeSubstance"] ?: return@forEach
                if (codeSubstance in seen) return@forEach
                seen.add(codeSubstance)

                stmt.setString(1, codeSubstance)
                stmt.setString(2, row["URI"])
                stmt.setString(3, row["libelle_fr"])
                stmt.setString(4, row["codeSMS"])
                stmt.setString(5, row["synonymes_fr"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadSpecialites(conn: Connection) {
        val sql = """
            INSERT INTO med_specialite_view (cis, uri, libelle, actif, date_debut, date_fin,
                code_atc, libelle_atc, type_procedure, statut_courant, titulaire_id,
                niveau_virtualisation, prescriptibilite_dc, forme_manufacturee)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("CIS.csv")
            val seen = mutableSetOf<Int>()
            rows.forEach { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@forEach
                if (cis in seen) return@forEach
                seen.add(cis)

                stmt.setInt(1, cis)
                stmt.setString(2, row["URI"])
                stmt.setString(3, row["libelle"])
                stmt.setBoolean(4, row["actif"]?.equals("true", ignoreCase = true) ?: true)
                stmt.setDate(5, parseDate(row["dateDebut"]))
                stmt.setDate(6, parseDate(row["dateFin"]))
                stmt.setString(7, row["codeATC"])
                stmt.setString(8, row["libelleATC"])
                stmt.setString(9, row["typeProcedureCourante"])
                stmt.setString(10, row["statutCourant"])
                stmt.setString(11, row["titulaireCourant"])
                stmt.setString(12, row["niveauDeVirtualisation"])
                stmt.setString(13, row["prescriptibiliteEnDC"])
                stmt.setString(14, row["formeStandardisee"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadSpecialiteVoies(conn: Connection) {
        val sql = """
            INSERT INTO med_specialite_voie_view (cis, voie_id)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE cis = VALUES(cis)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("CIS.csv")
            val seen = mutableSetOf<Pair<Int, String>>()
            rows.forEach { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@forEach
                val voies = row["voie"]?.split(";") ?: return@forEach

                voies.filter { it.isNotBlank() }.forEach { voieId ->
                    val key = cis to voieId.trim()
                    if (key in seen) return@forEach
                    seen.add(key)

                    stmt.setInt(1, cis)
                    stmt.setString(2, voieId.trim())
                    stmt.addBatch()
                }
            }
            stmt.executeBatch()
        }
    }

    private fun loadPresentations(conn: Connection) {
        val sql = """
            INSERT INTO med_presentation_view (cip13, cip7, uri, cis, libelle, alt_label,
                quantite_conditionnement, unite_conditionnement, nb_unite_disp, type_dispositif)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("CIP.csv")
            val seen = mutableSetOf<String>()
            rows.forEach { row ->
                val cip13 = row["CIP13"] ?: return@forEach
                if (cip13 in seen) return@forEach
                seen.add(cip13)

                val cis = cisCipMap[cip13] ?: return@forEach

                stmt.setString(1, cip13)
                stmt.setString(2, row["CIP7"]?.takeIf { it.isNotBlank() })
                stmt.setString(3, row["URI"])
                stmt.setInt(4, cis)
                stmt.setString(5, row["libelle"])
                stmt.setString(6, row["altLabel"])
                stmt.setBigDecimal(7, row["quantiteConditionnement"]?.toBigDecimalOrNull())
                stmt.setString(8, row["uniteQuantiteConditionnement"])
                stmt.setObject(9, row["nombreUniteDeDispensation"]?.toIntOrNull())
                stmt.setString(10, row["typeDispositif"]?.takeIf { it.isNotBlank() })
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadCompositionsQualitatives(conn: Connection) {
        val sql = """
            INSERT INTO med_composition_qualitative_view (cis, identifiant_element, relation_substance, code_substance)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE relation_substance = VALUES(relation_substance)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Compositions_qualitatives.csv")
            val seen = mutableSetOf<Triple<Int, String, String>>()
            rows.forEach { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@forEach
                val identifiantElement = row["identifiantElement"] ?: return@forEach
                val codeSubstance = row["codeSubstance"] ?: return@forEach
                val key = Triple(cis, identifiantElement, codeSubstance)
                if (key in seen) return@forEach
                seen.add(key)

                stmt.setInt(1, cis)
                stmt.setString(2, identifiantElement)
                stmt.setString(3, row["relationSubstance"])
                stmt.setString(4, codeSubstance)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadCompositionsQuantitatives(conn: Connection) {
        val sql = """
            INSERT INTO med_composition_quantitative_view (cis, identifiant_element, code_substance,
                expression_quantite, reference_dosage, substance_active, fraction_therapeutique)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE expression_quantite = VALUES(expression_quantite)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Compositions_quantitatives.csv")
            val seen = mutableSetOf<Triple<Int, String, String>>()
            rows.forEach { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@forEach
                val identifiantElement = row["identifiantElement"] ?: return@forEach
                val codeSubstance = row["substanceDeReference"] ?: return@forEach
                val key = Triple(cis, identifiantElement, codeSubstance)
                if (key in seen) return@forEach
                seen.add(key)

                stmt.setInt(1, cis)
                stmt.setString(2, identifiantElement)
                stmt.setString(3, codeSubstance)
                stmt.setString(4, row["expressionQuantite"])
                stmt.setString(5, row["referenceDosage"])
                stmt.setObject(6, row["substanceActive"]?.toIntOrNull())
                stmt.setObject(7, row["fractionTherapeutique"]?.toIntOrNull())
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadEvenementsCis(conn: Connection) {
        val sql = """
            INSERT INTO med_evenement_cis_view (evenement, uri, cis, date_effet, date_notification, type_evenement)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE type_evenement = VALUES(type_evenement)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Evenements_CIS.csv")
            val seen = mutableSetOf<String>()
            rows.forEach { row ->
                val evenement = row["evenement"] ?: return@forEach
                if (evenement in seen) return@forEach
                seen.add(evenement)

                stmt.setString(1, evenement)
                stmt.setString(2, row["URI"])
                stmt.setInt(3, row["CIS"]?.toIntOrNull() ?: return@forEach)
                stmt.setDate(4, parseDate(row["dateEffet"]))
                stmt.setDate(5, parseDate(row["dateNotification"]))
                stmt.setString(6, row["typeEvenement"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadEvenementsCip(conn: Connection) {
        val sql = """
            INSERT INTO med_evenement_cip_view (evenement, uri, cip13, date_effet, date_notification, type_evenement)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE type_evenement = VALUES(type_evenement)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Evenements_CIP.csv")
            val seen = mutableSetOf<String>()
            rows.forEach { row ->
                val evenement = row["evenement"] ?: return@forEach
                if (evenement in seen) return@forEach
                seen.add(evenement)

                stmt.setString(1, evenement)
                stmt.setString(2, row["URI"])
                stmt.setString(3, row["CIP13"])
                stmt.setDate(4, parseDate(row["dateEffet"]))
                stmt.setDate(5, parseDate(row["dateNotification"]))
                stmt.setString(6, row["typeEvenement"])
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadElements(conn: Connection) {
        val sql = """
            INSERT INTO med_element_specialite_view (uri, cis, identifiant_element, libelle, forme_manufacturee_litterale)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Elements.csv")
            val seen = mutableSetOf<Pair<Int, Int>>()
            rows.forEach { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@forEach
                val identifiantElement = row["identifiantElement"]?.toIntOrNull() ?: return@forEach
                val key = cis to identifiantElement
                if (key in seen) return@forEach
                seen.add(key)

                stmt.setString(1, row["URI"])
                stmt.setInt(2, cis)
                stmt.setInt(3, identifiantElement)
                stmt.setString(4, row["libelle"])
                stmt.setString(5, row["formeManufactureeLitterale"] ?: "")
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun loadElementsCip(conn: Connection) {
        val sql = """
            INSERT INTO med_element_presentation_view (uri, cip13, identifiant_element, libelle,
                type_contenant_litteral, type_contenant, forme_administrable, unite_presentation, type_dose)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE libelle = VALUES(libelle)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            val rows = readCsv("Elements_CIP.csv")
            val seen = mutableSetOf<Pair<String, Int>>()
            rows.forEach { row ->
                val cip13 = row["CIP13"] ?: return@forEach
                val identifiantElement = row["identifiantElement"]?.toIntOrNull() ?: return@forEach
                val key = cip13 to identifiantElement
                if (key in seen) return@forEach
                seen.add(key)

                stmt.setString(1, row["URI"])
                stmt.setString(2, cip13)
                stmt.setInt(3, identifiantElement)
                stmt.setString(4, row["libelle"])
                stmt.setString(5, row["typeContenantLitteral"] ?: "")
                stmt.setString(6, row["typeContenant"] ?: "")
                stmt.setString(7, row["formeAdministrable"] ?: "")
                stmt.setString(8, row["uniteDePresentation"] ?: "")
                stmt.setString(9, row["typeDose"] ?: "")
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun readCsv(filename: String): List<Map<String, String>> {
        val inputStream = this::class.java.classLoader.getResourceAsStream("data_test/$filename")
            ?: throw IllegalArgumentException("CSV file not found: data_test/$filename")

        return csvReader.readAllWithHeader(inputStream)
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            Date.valueOf(LocalDate.parse(dateStr))
        } catch (e: Exception) {
            null
        }
    }
}
