package medicapp.server.infrastructure.csv.core

import medicapp.server.infrastructure.db.requests.BatchExecutor
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement

class CsvImporter<T>(
    private val mapper: (Map<String, String>) -> T?,
    private val sql: String,
    private val binder: (PreparedStatement, T) -> Unit,
    private val batchSize: Int = 500
) {

    fun run(csvFile: File, connection: Connection) {
        val rows = CsvReader.read(csvFile)
        run(rows, connection)
    }

    fun run(rows: List<Map<String, String>>, connection: Connection) {
        val elements = rows.mapNotNull {
            try {
                mapper(it)
            } catch (_: Exception) {
                null
            }
        }

        BatchExecutor(connection, batchSize)
            .execute(sql, elements, binder)
    }
}
