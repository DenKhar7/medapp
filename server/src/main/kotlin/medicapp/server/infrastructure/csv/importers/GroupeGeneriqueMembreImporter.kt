package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedGroupeGeneriqueMembre
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection

object GroupeGeneriqueMembreImporter {
    private val sql = """
        INSERT INTO med_groupe_generique_membre (
            identifiant_groupe, cis, statut
        ) VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE
            cis = VALUES(cis),
            statut = VALUES(statut)
    """.trimIndent()

    fun run(csvFile: File, connection: Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedGroupeGeneriqueMembre(
                        identifiantGroupe = row["identifiantGroupeGenerique"] ?: return@CsvImporter null,
                        cis = row["CIS"]!!.toInt(),
                        statut = row["associationGroupeGeneriqueSpecialite"] ?: ""
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.identifiantGroupe)
                stmt.setInt(2, s.cis)
                stmt.setString(3, s.statut)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}