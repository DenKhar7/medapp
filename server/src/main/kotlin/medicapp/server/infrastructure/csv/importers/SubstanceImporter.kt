package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedSubstance
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection

object SubstanceImporter {

    private val sql = """
        INSERT INTO med_substance (
            code_substance, uri, libelle_fr, code_sms, synonymes_fr
        ) VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle_fr = VALUES(libelle_fr),
            code_sms = VALUES(code_sms),
            synonymes_fr = VALUES(synonymes_fr);
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        CsvImporter(
            mapper = { row ->
                MedSubstance(
                    codeSubstance = row["codeSubstance"]!!,
                    uri = row["URI"] ?: "",
                    libelleFr = row["libelle_fr"] ?: "",
                    codeSms = row["codeSMS"]?.trim()?.takeIf { it.isNotEmpty() },
                    synonymesFr = row["synonymes_fr"]
                )
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.codeSubstance)
                stmt.setString(2, s.uri)
                stmt.setString(3, s.libelleFr)
                stmt.setString(4, s.codeSms?.trim())
                stmt.setString(5, s.synonymesFr)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}
