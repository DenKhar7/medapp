package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.DictionnaireSms
import medicapp.server.infrastructure.csv.core.CsvImporter
import medicapp.server.infrastructure.csv.core.CsvReader
import medicapp.server.infrastructure.csv.prepare.SmsPreparator
import java.io.File
import java.sql.Connection
import java.sql.Date

object DictionnaireSmsImporter {
    private val sql = """
        INSERT INTO dictionnaire_sms (
            code_sms, nom_substance, is_terme_pref,
            source_nom, status_substance, type_substance,
            formule_moleculaire, poid_moleculaire,
            inchikey, last_update_date
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            is_terme_pref = VALUES(is_terme_pref),
            source_nom = VALUES(source_nom),
            status_substance = VALUES(status_substance),
            type_substance = VALUES(type_substance),
            formule_moleculaire = VALUES(formule_moleculaire),
            poid_moleculaire = VALUES(poid_moleculaire),
            inchikey = VALUES(inchikey),
            last_update_date = VALUES(last_update_date);
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        val rawRows = CsvReader.read(csvFile)

        val preparation = SmsPreparator().prepare(rawRows)

        CsvImporter(
            mapper = { row ->
                try {
                    DictionnaireSms(
                        codeSms = row["#SMS_ID"]!!,
                        nomSubstance = row["Substance_Name"]!!,
                        isTermePref = row["Is_Preferred_Name"]
                            ?.lowercase()
                            ?.toBooleanStrictOrNull(),
                        sourceNom = row["Name_Source"],
                        statusSubstance = row["Status"],
                        typeSubstance = row["Substance_Type"],
                        formuleMoleculaire = row["Molecular_Formula"],
                        poidsMoleculaire = row["Molecular_Weight"]?.toFloatOrNull(),
                        inchikey = row["Inchikey"],
                        lastUpdateDate = row["Last_Updated_Date"]
                            ?.takeIf { it.isNotBlank() }
                            ?.substring(0, 10)
                            ?.let { Date.valueOf(it) }
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.codeSms.trim())
                stmt.setString(2,s.nomSubstance)
                stmt.setObject(3,s.isTermePref)
                stmt.setString(4,s.sourceNom)
                stmt.setString(5,s.statusSubstance)
                stmt.setString(6,s.typeSubstance)
                stmt.setString(7,s.formuleMoleculaire)
                stmt.setObject(8,s.poidsMoleculaire)
                stmt.setString(9,s.inchikey)
                stmt.setObject(10,s.lastUpdateDate)
            },
            batchSize = 500
        ).run(preparation.data, connection)
    }
}