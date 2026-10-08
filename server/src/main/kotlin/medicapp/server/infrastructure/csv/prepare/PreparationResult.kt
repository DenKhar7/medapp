package medicapp.server.infrastructure.csv.prepare

data class PreparationResult<T>(
    val data: List<T>,
    val dropped: Int,
    val warnings: List<String> = emptyList()
)