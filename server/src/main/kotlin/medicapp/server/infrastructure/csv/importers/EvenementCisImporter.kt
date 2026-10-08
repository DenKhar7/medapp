package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedEvenementCis
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection
import java.sql.Date

object EvenementCisImporter {
    private val sql = """
        INSERT INTO med_evenement_cis (
            evenement, cis, uri, date_effet, date_notification,
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
        CsvImporter(
            mapper = { row ->
                try {
                    MedEvenementCis(
                        evenement = row["evenement"]!!,
                        cis = row["CIS"]!!.toInt(),
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
                stmt.setInt(2, s.cis)
                stmt.setString(3, s.uri)
                stmt.setObject(4, s.dateEffet)
                stmt.setObject(5, s.dateNotification)
                stmt.setString(6, s.typeEvenement)
                stmt.setString(7, s.description)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}