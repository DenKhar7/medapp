package medicapp.server.infrastructure.csv.prepare


fun interface DataPreparator<I, O> {
    fun prepare(input: List<I>): PreparationResult<O>
}
