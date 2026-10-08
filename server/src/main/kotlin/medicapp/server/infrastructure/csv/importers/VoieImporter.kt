package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedVoie
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection

object VoieImporter {
    private val sql = """
        INSERT INTO med_voie (
            identifiant, uri, libelle, code_rms, code_edqm
        ) VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle = VALUES(libelle),
            code_rms = VALUES(code_rms),
            code_edqm = VALUES(code_edqm);
    """.trimIndent()

    fun run(csvFile: File, connection : Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedVoie(
                        identifiant = row["identifiant"] ?: return@CsvImporter null,
                        uri = row["URI"] ?: "",
                        libelle = row["libelle"] ?: "",
                        codeRms = row["codeRMS"],
                        codeEdqm = row["codeEDQM"]
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.identifiant)
                stmt.setString(2, s.uri)
                stmt.setString(3, s.libelle)
                stmt.setString(4, s.codeRms)
                stmt.setString(5, s.codeEdqm)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}