package medicapp.server.infrastructure.csv.importers

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.sql.Connection
import medicapp.server.infrastructure.db.requests.BatchExecutor

object SpecialiteVoieImporter {

    private val sql = """
        INSERT IGNORE INTO med_specialite_voie (cis, voie_id)
        VALUES (?, ?);
    """.trimIndent()

    fun run(csvFile: File, connection: Connection) {

        val relations = csvReader {
            delimiter = ','
            quoteChar = '"'
            escapeChar = '\\'
        }.readAllWithHeader(csvFile)
            .flatMap { row ->
                val cis = row["CIS"]?.toIntOrNull() ?: return@flatMap emptyList()

                row["voie"]
                    ?.split(";")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.map { voieId ->
                        cis to voieId
                    }
                    ?: emptyList()
            }

        BatchExecutor(connection, batchSize = 1000)
            .execute(sql, relations) { stmt, (cis, voieId) ->
                stmt.setInt(1, cis)
                stmt.setString(2, voieId)
            }
    }
}