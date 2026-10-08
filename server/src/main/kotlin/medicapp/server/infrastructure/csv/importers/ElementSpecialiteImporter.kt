package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedElementSpecialite
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection

object ElementSpecialiteImporter {
    private val sql = """
        INSERT INTO med_element_specialite (
            uri, cis, identifiant_element, libelle, forme_manufacturee_litterale
        ) VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle = VALUES(libelle),
            forme_manufacturee_litterale = VALUES(forme_manufacturee_litterale);
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedElementSpecialite(
                        uri = row["URI"] ?: "",
                        cis = row["CIS"]?.toIntOrNull() ?: return@CsvImporter null,
                        identifiantElement = row["identifiantElement"]?.toIntOrNull() ?: return@CsvImporter null,
                        libelle = row["libelle"] ?: "",
                        formeManufactureeLitterale = row["formeManufactureeLitterale"] ?: ""
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.uri)
                stmt.setInt(2, s.cis)
                stmt.setInt(3, s.identifiantElement)
                stmt.setString(4, s.libelle)
                stmt.setString(5, s.formeManufactureeLitterale)

            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}