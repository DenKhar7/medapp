package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedCompositionQuantitative
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection


object CompositionQuantitativeImporter {
    private val sql = """
        INSERT INTO med_composition_quantitative (
            cis, identifiant_element, code_substance, expression_quantite, reference_dosage, substance_active, fraction_therapeutique
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            expression_quantite = VALUES(expression_quantite),
            reference_dosage = VALUES(reference_dosage),
            substance_active = VALUES(substance_active),
            fraction_therapeutique = VALUES(fraction_therapeutique)
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedCompositionQuantitative(
                        cis = row["CIS"]!!.toInt(),
                        identifiantElement = row["identifiantElement"] ?: return@CsvImporter null,
                        codeSubstance = row["substanceDeReference"] ?: "",
                        expressionQuantite = row["expressionQuantite"],
                        refereneceDosage = row["referenceDosage"],
                        substanceActive = row["substanceActive"]?.toIntOrNull(),
                        fractionTherapeutique = row["fractionTherapeutique"]?.toIntOrNull()
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
                stmt.setString(4, s.expressionQuantite)
                stmt.setString(5, s.refereneceDosage)
                stmt.setObject(6, s.substanceActive)
                stmt.setObject(7, s.fractionTherapeutique)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}