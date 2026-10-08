package medicapp.server.infrastructure.db.requests

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BatchExecutorTest {

    private lateinit var mockConnection: Connection
    private lateinit var mockStatement: PreparedStatement

    @BeforeEach
    fun setup() {
        mockConnection = mockk(relaxed = true)
        mockStatement = mockk(relaxed = true)
        every { mockConnection.prepareStatement(any()) } returns mockStatement
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ─────────────────────────────────────────────────────────────
    // Tests execute() - cas nominaux
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute avec liste non vide appelle addBatch pour chaque element`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf("A", "B", "C")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert
        verify(exactly = 3) { mockStatement.addBatch() }
    }

    @Test
    fun `execute appelle executeBatch au moins une fois`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf("A", "B")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert
        verify(atLeast = 1) { mockStatement.executeBatch() }
    }

    @Test
    fun `execute avec liste vide n appelle pas addBatch mais appelle executeBatch`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = emptyList<String>()

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert
        verify(exactly = 0) { mockStatement.addBatch() }
        verify(exactly = 1) { mockStatement.executeBatch() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests execute() - batchSize
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute avec batchSize 2 et 5 elements appelle executeBatch 3 fois`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 2)
        val items = listOf("A", "B", "C", "D", "E")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert - executeBatch est appele a count=2, count=4, puis final
        verify(exactly = 5) { mockStatement.addBatch() }
        verify(exactly = 3) { mockStatement.executeBatch() }
    }

    @Test
    fun `execute avec batchSize 3 et 6 elements appelle executeBatch 3 fois`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 3)
        val items = listOf("A", "B", "C", "D", "E", "F")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert - executeBatch a count=3, count=6, puis final
        verify(exactly = 6) { mockStatement.addBatch() }
        verify(exactly = 3) { mockStatement.executeBatch() }
    }

    @Test
    fun `execute avec batchSize egal au nombre d elements appelle executeBatch 2 fois`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 3)
        val items = listOf("A", "B", "C")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert - executeBatch a count=3 (multiple de batchSize) puis final
        verify(exactly = 3) { mockStatement.addBatch() }
        verify(exactly = 2) { mockStatement.executeBatch() }
    }

    @Test
    fun `execute avec batchSize superieur au nombre d elements appelle executeBatch 1 fois`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 100)
        val items = listOf("A", "B", "C")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert
        verify(exactly = 3) { mockStatement.addBatch() }
        verify(exactly = 1) { mockStatement.executeBatch() }
    }

    @Test
    fun `execute avec batchSize 1 appelle executeBatch pour chaque element plus final`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 1)
        val items = listOf("A", "B", "C")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert - executeBatch a count=1, count=2, count=3, puis final
        verify(exactly = 3) { mockStatement.addBatch() }
        verify(exactly = 4) { mockStatement.executeBatch() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests binder
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute appelle binder avec PreparedStatement et item correct`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf("TestValue")
        var capturedStmt: PreparedStatement? = null
        var capturedItem: String? = null

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { stmt, item ->
            capturedStmt = stmt
            capturedItem = item
        }

        // Assert
        assertEquals(mockStatement, capturedStmt)
        assertEquals("TestValue", capturedItem)
    }

    @Test
    fun `execute appelle binder dans l ordre des elements`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf("First", "Second", "Third")
        val capturedItems = mutableListOf<String>()

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, item ->
            capturedItems.add(item)
        }

        // Assert
        assertEquals(listOf("First", "Second", "Third"), capturedItems)
    }

    @Test
    fun `execute appelle binder pour chaque element meme avec batchSize petit`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 2)
        val items = listOf("A", "B", "C", "D", "E")
        val binderCallCount = mutableListOf<Int>()
        var count = 0

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ ->
            count++
            binderCallCount.add(count)
        }

        // Assert
        assertEquals(listOf(1, 2, 3, 4, 5), binderCallCount)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests SQL
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute utilise le SQL fourni pour prepareStatement`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val expectedSql = "INSERT INTO custom_table (col1, col2) VALUES (?, ?)"

        // Act
        executor.execute(expectedSql, listOf("item")) { _, _ -> }

        // Assert
        verify(exactly = 1) { mockConnection.prepareStatement(expectedSql) }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests fermeture des ressources
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute ferme le PreparedStatement apres execution`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf("A", "B")

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert
        verify(exactly = 1) { mockStatement.close() }
    }

    @Test
    fun `execute ferme le PreparedStatement meme avec liste vide`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)

        // Act
        executor.execute("INSERT INTO test VALUES (?)", emptyList<String>()) { _, _ -> }

        // Assert
        verify(exactly = 1) { mockStatement.close() }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests gestion des erreurs
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute propage SQLException de executeBatch`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        every { mockStatement.executeBatch() } throws SQLException("DB Error")

        // Act & Assert
        assertFailsWith<SQLException> {
            executor.execute("INSERT INTO test VALUES (?)", listOf("A")) { _, _ -> }
        }
    }

    @Test
    fun `execute propage SQLException de addBatch`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        every { mockStatement.addBatch() } throws SQLException("Batch Error")

        // Act & Assert
        assertFailsWith<SQLException> {
            executor.execute("INSERT INTO test VALUES (?)", listOf("A")) { _, _ -> }
        }
    }

    @Test
    fun `execute propage exception du binder`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)

        // Act & Assert
        assertFailsWith<RuntimeException> {
            executor.execute("INSERT INTO test VALUES (?)", listOf("A")) { _, _ ->
                throw RuntimeException("Binder error")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tests avec types generiques
    // ─────────────────────────────────────────────────────────────

    data class Entity(val id: Int, val name: String)

    @Test
    fun `execute fonctionne avec des objets complexes`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items = listOf(
            Entity(1, "Alice"),
            Entity(2, "Bob")
        )
        val capturedEntities = mutableListOf<Entity>()

        // Act
        executor.execute("INSERT INTO users (id, name) VALUES (?, ?)", items) { _, entity ->
            capturedEntities.add(entity)
        }

        // Assert
        assertEquals(2, capturedEntities.size)
        assertEquals(Entity(1, "Alice"), capturedEntities[0])
        assertEquals(Entity(2, "Bob"), capturedEntities[1])
    }

    @Test
    fun `execute fonctionne avec valeurs nullables`() {
        // Arrange
        val executor = BatchExecutor(mockConnection, batchSize = 500)
        val items: List<String?> = listOf("A", null, "C")
        val capturedItems = mutableListOf<String?>()

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, item ->
            capturedItems.add(item)
        }

        // Assert
        assertEquals(listOf("A", null, "C"), capturedItems)
    }

    // ─────────────────────────────────────────────────────────────
    // Tests valeurs par defaut
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `execute avec batchSize par defaut utilise 500`() {
        // Arrange
        val executor = BatchExecutor(mockConnection)  // batchSize par defaut = 500
        val items = (1..501).map { "item$it" }

        // Act
        executor.execute("INSERT INTO test VALUES (?)", items) { _, _ -> }

        // Assert - avec 501 elements et batchSize=500, on attend 2 executeBatch
        verify(exactly = 501) { mockStatement.addBatch() }
        verify(exactly = 2) { mockStatement.executeBatch() }
    }
}
