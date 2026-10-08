package medicapp.server.infrastructure.csv.core

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvImporterTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockConnection: Connection
    private lateinit var mockStatement: PreparedStatement
    private lateinit var csvFile: File

    @BeforeEach
    fun setup() {
        mockConnection = mockk(relaxed = true)
        mockStatement = mockk(relaxed = true)
        csvFile = tempDir.resolve("test.csv").toFile()

        every { mockConnection.prepareStatement(any()) } returns mockStatement
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        csvFile.delete()
    }

    // ─────────────────────────────────────────────────────────────
    // Data class de test
    // ─────────────────────────────────────────────────────────────

    data class TestEntity(val id: Int, val name: String)

    // ─────────────────────────────────────────────────────────────
    // Tests run() avec fichier CSV
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run avec fichier CSV valide execute batch correctement`() {
        // Arrange
        csvFile.writeText("""
            id,name
            1,Alice
            2,Bob
        """.trimIndent())

        val binderCalls = mutableListOf<TestEntity>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> binderCalls.add(entity) },
            batchSize = 500
        )

        // Act
        importer.run(csvFile, mockConnection)

        // Assert
        assertEquals(2, binderCalls.size)
        assertEquals(TestEntity(1, "Alice"), binderCalls[0])
        assertEquals(TestEntity(2, "Bob"), binderCalls[1])
        verify(exactly = 2) { mockStatement.addBatch() }
        verify(atLeast = 1) { mockStatement.executeBatch() }
    }

    @Test
    fun `run avec fichier CSV vide n execute pas de batch`() {
        // Arrange
        csvFile.writeText("id,name")

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> }
        )

        // Act
        importer.run(csvFile, mockConnection)

        // Assert
        verify(exactly = 0) { mockStatement.addBatch() }
        verify(exactly = 1) { mockStatement.executeBatch() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests run() avec liste de rows
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run avec liste de rows valide execute batch correctement`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "Alice"),
            mapOf("id" to "2", "name" to "Bob"),
            mapOf("id" to "3", "name" to "Charlie")
        )

        val binderCalls = mutableListOf<TestEntity>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> binderCalls.add(entity) }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        assertEquals(3, binderCalls.size)
        verify(exactly = 3) { mockStatement.addBatch() }
    }

    @Test
    fun `run avec liste vide n execute aucun addBatch`() {
        // Arrange
        val rows = emptyList<Map<String, String>>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        verify(exactly = 0) { mockStatement.addBatch() }
        verify(exactly = 1) { mockStatement.executeBatch() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests mapper avec exceptions
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run ignore les lignes ou mapper lance exception`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "Alice"),
            mapOf("id" to "invalid", "name" to "Bob"),  // Cette ligne va echouer
            mapOf("id" to "3", "name" to "Charlie")
        )

        val binderCalls = mutableListOf<TestEntity>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> binderCalls.add(entity) }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert - seules 2 entites sont traitees (la ligne "invalid" est ignoree)
        assertEquals(2, binderCalls.size)
        assertEquals(TestEntity(1, "Alice"), binderCalls[0])
        assertEquals(TestEntity(3, "Charlie"), binderCalls[1])
    }

    @Test
    fun `run ignore les lignes ou mapper retourne null`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "Alice"),
            mapOf("id" to "2", "name" to "SKIP"),  // mapper retourne null
            mapOf("id" to "3", "name" to "Charlie")
        )

        val binderCalls = mutableListOf<TestEntity>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row ->
                if (row["name"] == "SKIP") null
                else TestEntity(row["id"]!!.toInt(), row["name"]!!)
            },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> binderCalls.add(entity) }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        assertEquals(2, binderCalls.size)
        assertEquals(TestEntity(1, "Alice"), binderCalls[0])
        assertEquals(TestEntity(3, "Charlie"), binderCalls[1])
    }

    @Test
    fun `run gere toutes les exceptions du mapper sans arreter le traitement`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "bad1", "name" to "A"),
            mapOf("id" to "bad2", "name" to "B"),
            mapOf("id" to "1", "name" to "Valid")
        )

        val binderCalls = mutableListOf<TestEntity>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> binderCalls.add(entity) }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert - seule la derniere ligne valide est traitee
        assertEquals(1, binderCalls.size)
        assertEquals(TestEntity(1, "Valid"), binderCalls[0])
    }

    // ─────────────────────────────────────────────────────────────
    // Tests batchSize
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run avec batchSize 2 execute batch plusieurs fois`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "A"),
            mapOf("id" to "2", "name" to "B"),
            mapOf("id" to "3", "name" to "C"),
            mapOf("id" to "4", "name" to "D"),
            mapOf("id" to "5", "name" to "E")
        )

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> },
            batchSize = 2
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert - 5 elements avec batchSize 2 = 2 executeBatch intermediaires (2, 4) + 1 final
        verify(exactly = 5) { mockStatement.addBatch() }
        verify(exactly = 3) { mockStatement.executeBatch() }
    }

    @Test
    fun `run avec batchSize egal au nombre d elements execute 2 batch`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "A"),
            mapOf("id" to "2", "name" to "B")
        )

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> },
            batchSize = 2
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert - 2 elements avec batchSize 2 = 1 executeBatch a count=2 + 1 executeBatch final
        verify(exactly = 2) { mockStatement.addBatch() }
        verify(exactly = 2) { mockStatement.executeBatch() }
    }

    @Test
    fun `run avec batchSize superieur au nombre d elements execute 1 batch final`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "A"),
            mapOf("id" to "2", "name" to "B")
        )

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> },
            batchSize = 500
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        verify(exactly = 2) { mockStatement.addBatch() }
        verify(exactly = 1) { mockStatement.executeBatch() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests binder
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `binder est appele avec PreparedStatement et entite correcte`() {
        // Arrange
        val rows = listOf(mapOf("id" to "42", "name" to "Test"))
        var capturedStatement: PreparedStatement? = null
        var capturedEntity: TestEntity? = null

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { stmt, entity ->
                capturedStatement = stmt
                capturedEntity = entity
            }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        assertEquals(mockStatement, capturedStatement)
        assertEquals(TestEntity(42, "Test"), capturedEntity)
    }

    @Test
    fun `binder est appele dans l ordre des lignes`() {
        // Arrange
        val rows = listOf(
            mapOf("id" to "1", "name" to "First"),
            mapOf("id" to "2", "name" to "Second"),
            mapOf("id" to "3", "name" to "Third")
        )
        val capturedOrder = mutableListOf<Int>()

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, entity -> capturedOrder.add(entity.id) }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        assertEquals(listOf(1, 2, 3), capturedOrder)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests SQL
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run utilise le SQL fourni pour prepareStatement`() {
        // Arrange
        val rows = listOf(mapOf("id" to "1", "name" to "Test"))
        val expectedSql = "INSERT INTO custom_table (col1, col2) VALUES (?, ?)"

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = expectedSql,
            binder = { _, _ -> }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        verify(exactly = 1) { mockConnection.prepareStatement(expectedSql) }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests fermeture des ressources
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `run ferme le PreparedStatement apres execution`() {
        // Arrange
        val rows = listOf(mapOf("id" to "1", "name" to "Test"))

        val importer = CsvImporter<TestEntity>(
            mapper = { row -> TestEntity(row["id"]!!.toInt(), row["name"]!!) },
            sql = "INSERT INTO test (id, name) VALUES (?, ?)",
            binder = { _, _ -> }
        )

        // Act
        importer.run(rows, mockConnection)

        // Assert
        verify(exactly = 1) { mockStatement.close() }
    }
}
