package medicapp.server.infrastructure.csv.importers

import medicapp.server.config.logger
import medicapp.server.domain.models.MedEvenementCip
import medicapp.server.infrastructure.csv.core.CsvImporter
import medicapp.server.infrastructure.csv.core.ReferenceLookup
import java.io.File
import java.sql.Connection
import java.sql.Date

object EvenementCipImporter {
    private val log = logger()

    private val sql = """
        INSERT INTO med_evenement_cip (
            evenement, cip13, uri, date_effet, date_notification,
            type_evenement, description
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            date_effet = VALUES(date_effet),
            date_notification = VALUES(date_notification),
            type_evenement = VALUES(type_evenement),
            description = VALUES(description);
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        // Seules les présentations réellement importées peuvent être référencées (clé étrangère cip13).
        val knownCip13 = ReferenceLookup.presentationCip13s(connection)
        var skippedOrphans = 0

        CsvImporter(
            mapper = { row ->
                try {
                    val cip13 = row["CIP13"]!!
                    if (cip13.trim() !in knownCip13) {
                        skippedOrphans++
                        return@CsvImporter null
                    }
                    MedEvenementCip(
                        evenement = row["evenement"]!!,
                        cip13 = cip13,
                        uri = row["URI"]!!,
                        dateEffet = row["dateEffet"]?.takeIf { it.isNotBlank() }?.let { Date.valueOf(it) },
                        dateNotification = row["dateNotification"]?.takeIf { it.isNotBlank() }?.let { Date.valueOf(it) },
                        typeEvenement = row["typeEvenement"],
                        description = row["description"]
                    )
                } catch (e: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.evenement)
                stmt.setString(2, s.cip13)
                stmt.setString(3, s.uri)
                stmt.setObject(4, s.dateEffet)
                stmt.setObject(5, s.dateNotification)
                stmt.setString(6, s.typeEvenement)
                stmt.setString(7, s.description)
            },
            batchSize = 500
        ).run(csvFile, connection)

        if (skippedOrphans > 0) {
            log.warn("[RUIM] med_evenement_cip : $skippedOrphans ligne(s) ignorée(s), CIP13 absent des présentations")
        }
    }
}
