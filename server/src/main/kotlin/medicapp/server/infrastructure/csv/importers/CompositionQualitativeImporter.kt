package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedCompositionQualitative
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection


object CompositionQualitativeImporter {
    private val sql = """
        INSERT INTO med_composition_qualitative (
            cis, identifiant_element, code_substance, relation_substance
        ) VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            relation_substance = VALUES(relation_substance)
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedCompositionQualitative(
                        cis = row["CIS"]!!.toInt(),
                        identifiantElement = row["identifiantElement"] ?: "",
                        codeSubstance = row["codeSubstance"] ?: "",
                        relationSubstance = row["relationSubstance"] ?: "",
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setInt(1, s.cis)
                stmt.setString(2, s.identifiantElement)
                stmt.setString(3, s.codeSubstance)
                stmt.setString(4, s.relationSubstance)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}