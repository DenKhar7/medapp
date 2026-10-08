package medicapp.server.infrastructure.csv.importers

import medicapp.server.config.logger
import medicapp.server.domain.models.MedElementPresentation
import medicapp.server.infrastructure.csv.core.CsvImporter
import medicapp.server.infrastructure.csv.core.ReferenceLookup
import java.io.File
import java.sql.Connection

object ElementPresentationImporter {
    private val log = logger()

    private val sql = """
        INSERT INTO med_element_presentation (
            uri, cip13, identifiant_element, libelle,
            type_contenant_litteral, type_contenant,
            forme_administrable, unite_presentation, type_dose
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle = VALUES(libelle),
            type_contenant_litteral = VALUES(type_contenant_litteral),
            type_contenant = VALUES(type_contenant),
            forme_administrable = VALUES(forme_administrable),
            unite_presentation = VALUES(unite_presentation),
            type_dose = VALUES(type_dose);
    """.trimIndent()

    fun run(csvFile : File, connection : Connection) {
        // Seules les présentations réellement importées peuvent être référencées (clé étrangère cip13).
        val knownCip13 = ReferenceLookup.presentationCip13s(connection)
        var skippedOrphans = 0

        CsvImporter(
            mapper = { row ->
                try {
                    val cip13 = row["CIP13"]?.trim() ?: return@CsvImporter null
                    if (cip13 !in knownCip13) {
                        skippedOrphans++
                        return@CsvImporter null
                    }
                    MedElementPresentation(
                        uri = row["URI"] ?: "",
                        cip13 = cip13,
                        identifiantElement = row["identifiantElement"]?.toIntOrNull() ?: return@CsvImporter null,
                        libelle = row["libelle"] ?: "",
                        typeContenantLitteral = row["typeContenantLitteral"] ?: "",
                        typeContenant = row["typeContenant"],
                        formeAdministrable = row["formeAdministrable"],
                        unitePresentation = row["uniteDePresentation"],
                        typeDose = row["typeDose"]
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setString(1, s.uri)
                stmt.setString(2, s.cip13)
                stmt.setInt(3, s.identifiantElement)
                stmt.setString(4, s.libelle)
                stmt.setString(5, s.typeContenantLitteral)
                stmt.setString(6, s.typeContenant)
                stmt.setString(7, s.formeAdministrable)
                stmt.setString(8, s.unitePresentation)
                stmt.setString(9, s.typeDose)

            },
            batchSize = 500
        ).run(csvFile, connection)

        if (skippedOrphans > 0) {
            log.warn("[RUIM] med_element_presentation : $skippedOrphans ligne(s) ignorée(s), CIP13 absent des présentations")
        }
    }
}
