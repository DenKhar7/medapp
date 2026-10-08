// Databases.kt
package medicapp.server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.Connection
import javax.sql.DataSource

// Attente maximale d'une connexion. Les jobs d'import gardent le délai par défaut (leurs transactions sont longues) ;
// l'API doit échouer vite quand la base est indisponible, sinon chaque requête (et /health) reste bloquée 30 s.
private const val DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L
private const val VIEWS_CONNECTION_TIMEOUT_MS = 3_000L

enum class MedDatabase {
    VIEWS,
    ACTIVE,
    ARCHIVE,
    TEMP
}

object DbRegistry {
    private val databases = mutableMapOf<MedDatabase, Database>()
    private val dataSources = mutableMapOf<MedDatabase, DataSource>()
    private var isConfigured = false

    fun register(
        type: MedDatabase,
        database: Database,
        dataSource: DataSource
    ) {
        databases[type] = database
        dataSources[type] = dataSource
    }

    fun database(type: MedDatabase): Database =
        databases[type] ?: error("Database $type not registered")

    fun dataSource(type: MedDatabase): DataSource =
        dataSources[type] ?: error("DataSource $type not registered")

    fun isConfigured(): Boolean = isConfigured

    fun markAsConfigured() {
        isConfigured = true
    }
}

// Nouvelle fonction standalone qui accepte ApplicationConfig
fun configureDatabases(config: ApplicationConfig) {
    if (DbRegistry.isConfigured()) {
        println("[DB] Bases de données déjà configurées, skip")
        return
    }

    println("[DB] Configuration des bases de données...")

    fun cfg(path: String) = config.config(path)

    val driver = config.property("db.driver").getString()
    val user = config.property("db.username").getString()
    val password = config.property("db.password").getString()

    // Les requêtes de l'API ne lisent que les vues : elles utilisent un compte en LECTURE SEULE dédié
    // (db.views.username / password). À défaut, repli sur le compte principal (environnement de test).
    val viewsConfig = cfg("db.views")
    val viewsUser = viewsConfig.propertyOrNull("username")?.getString() ?: user
    val viewsPassword = viewsConfig.propertyOrNull("password")?.getString() ?: password

    val (viewsDb, viewsDs) = buildDatabaseWithDataSource(
        viewsConfig.property("jdbcUrl").getString(),
        driver,
        viewsUser,
        viewsPassword,
        viewsConfig.property("maximumPoolSize").getString().toInt(),
        VIEWS_CONNECTION_TIMEOUT_MS
    )
    DbRegistry.register(MedDatabase.VIEWS, viewsDb, viewsDs)

    val (activeDb, activeDs) = buildDatabaseWithDataSource(
        cfg("db.active").property("jdbcUrl").getString(),
        driver,
        user,
        password,
        cfg("db.active").property("maximumPoolSize").getString().toInt()
    )
    DbRegistry.register(MedDatabase.ACTIVE, activeDb, activeDs)

    val (archiveDb, archiveDs) = buildDatabaseWithDataSource(
        cfg("db.archive").property("jdbcUrl").getString(),
        driver,
        user,
        password,
        cfg("db.archive").property("maximumPoolSize").getString().toInt()
    )
    DbRegistry.register(MedDatabase.ARCHIVE, archiveDb, archiveDs)

    val (tempDb, tempDs) = buildDatabaseWithDataSource(
        cfg("db.temp").property("jdbcUrl").getString(),
        driver,
        user,
        password,
        cfg("db.temp").property("maximumPoolSize").getString().toInt()
    )
    DbRegistry.register(MedDatabase.TEMP, tempDb, tempDs)

    DbRegistry.markAsConfigured()
    println("[DB] Bases de données configurées avec succès")
}

// Extension function pour Application (garde la compatibilité)
fun Application.configureDatabases() {
    configureDatabases(environment.config)
}

private fun provideDataSource(
    url: String,
    driverClass: String,
    username: String,
    password: String,
    maxPoolSize: Int,
    connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS
): HikariDataSource {

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = url
        this.driverClassName = driverClass
        this.username = username
        this.password = password
        this.maximumPoolSize = maxPoolSize
        this.connectionTimeout = connectionTimeoutMs
        this.isAutoCommit = false
        this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    return HikariDataSource(hikariConfig)
}

private fun buildDatabaseWithDataSource(
    jdbcUrl: String,
    driverClass: String,
    username: String,
    password: String,
    maxPoolSize: Int,
    connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS
): Pair<Database, DataSource> {

    val ds = provideDataSource(
        jdbcUrl,
        driverClass,
        username,
        password,
        maxPoolSize,
        connectionTimeoutMs
    )

    val db = Database.connect(ds)
    return db to ds
}

fun JdbcConnection(db: MedDatabase): Connection {
    return DbRegistry
        .dataSource(db)
        .connection
}

suspend fun <T> dbViewQuery(block: suspend () -> T): T =
    newSuspendedTransaction(
        Dispatchers.IO,
        db = DbRegistry.database(MedDatabase.VIEWS)
    ) {
        block()
    }

suspend fun <T> dbQuery(
    database: MedDatabase,
    block: suspend () -> T
): T =
    newSuspendedTransaction(
        Dispatchers.IO,
        db = DbRegistry.database(database)
    ) {
        block()
    }