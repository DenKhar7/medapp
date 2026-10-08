package medicapp.server.infrastructure.csv.importers

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import medicapp.server.domain.models.MedPresentation
import medicapp.server.infrastructure.db.requests.BatchExecutor
import java.io.File
import java.sql.Connection

object PresentationImporter {

    private val sql = """
        INSERT INTO med_presentation (
            cip13, cip7, uri, cis, libelle, alt_label,
            quantite_conditionnement, unite_conditionnement,
            nb_unite_disp, type_dispositif
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            cis = VALUES(cis),
            libelle = VALUES(libelle),
            alt_label = VALUES(alt_label),
            quantite_conditionnement = VALUES(quantite_conditionnement),
            unite_conditionnement = VALUES(unite_conditionnement),
            nb_unite_disp = VALUES(nb_unite_disp),
            type_dispositif = VALUES(type_dispositif);
    """.trimIndent()

    fun run(
        cipFile: File,
        correspondanceFile: File,
        connection: Connection
    ) {

        val cipToCis = loadCipToCis(correspondanceFile)

        val presentations = csvReader {
            delimiter = ','
            quoteChar = '"'
            escapeChar = '\\'
        }.readAllWithHeader(cipFile)
            .mapNotNull { row ->
                val cip13 = row["CIP13"]?.trim() ?: return@mapNotNull null
                val cis = cipToCis[cip13] ?: return@mapNotNull null

                MedPresentation(
                    cip13 = cip13,
                    cip7 = row["CIP7"],
                    uri = row["URI"] ?: "",
                    cis = cis,
                    libelle = row["libelle"] ?: "",
                    altLabel = row["altLabel"],
                    quantiteConditionnement = row["quantiteConditionnement"]?.toFloatOrNull(),
                    uniteConditionnement = row["uniteQuantiteConditionnement"],
                    nbUniteDisp = row["nombreUniteDeDispensation"]?.toIntOrNull(),
                    typeDispositif = row["typeDispositif"]
                )
            }

        BatchExecutor(connection, batchSize = 500)
            .execute(sql, presentations) { stmt, p ->
                stmt.setString(1, p.cip13)
                stmt.setString(2, p.cip7)
                stmt.setString(3, p.uri)
                stmt.setInt(4, p.cis)
                stmt.setString(5, p.libelle)
                stmt.setString(6, p.altLabel)
                stmt.setObject(7, p.quantiteConditionnement)
                stmt.setString(8, p.uniteConditionnement)
                stmt.setObject(9, p.nbUniteDisp)
                stmt.setString(10, p.typeDispositif)
            }
    }

    private fun loadCipToCis(file: File): Map<String, Int> =
        csvReader {
            delimiter = ','
            quoteChar = '"'
            escapeChar = '\\'
        }.readAllWithHeader(file)
            .mapNotNull { row ->
                val cip13 = row["CIP13"]?.trim() ?: return@mapNotNull null
                val cis = row["CIS"]?.toIntOrNull() ?: return@mapNotNull null
                cip13 to cis
            }
            .toMap()
}
