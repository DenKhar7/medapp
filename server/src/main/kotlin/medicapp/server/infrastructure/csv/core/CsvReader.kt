package medicapp.server.infrastructure.csv.core

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

object CsvReader {
    fun read(
        file: File
    ): List<Map<String, String>> =
        csvReader {
            delimiter = ','
            quoteChar = '"'
            charset = Charsets.UTF_8.toString()
        }.readAllWithHeader(file)
}