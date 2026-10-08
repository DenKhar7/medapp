package medicapp.server.infrastructure.db.requests

import java.sql.Connection
import java.sql.PreparedStatement

class BatchExecutor(
    private val connection: Connection,
    private val batchSize: Int = 500
) {
    fun <T> execute(
        sql: String,
        items: List<T>,
        binder: (PreparedStatement, T) -> Unit
    ) {
        connection.prepareStatement(sql).use { stmt ->
            var count = 0
            for (item in items) {
                binder(stmt, item)
                stmt.addBatch()
                count++
                if (count % batchSize == 0) {
                    stmt.executeBatch()
                }
            }
            stmt.executeBatch()
        }
    }
}
