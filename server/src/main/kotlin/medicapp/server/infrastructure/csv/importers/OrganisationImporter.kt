package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedOrganisation
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection

object OrganisationImporter {
    private val sql = """
        INSERT INTO med_organisation (
            identifiant, uri, libelle, pays
        ) VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle = VALUES(libelle),
            pays = VALUES(pays);
    """.trimIndent()

    fun run(csvFile : File, connection: Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedOrganisation(
                        identifiant = row["identifiant"] ?: return@CsvImporter null,
                        uri = row["URI"] ?: "",
                        libelle = row["libelle"] ?: "",
                        pays = row["nomPays"]
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
                stmt.setString(4, s.pays)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}