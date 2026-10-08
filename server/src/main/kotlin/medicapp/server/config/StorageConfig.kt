package medicapp.server.config

import io.ktor.server.config.ApplicationConfig
import java.nio.file.Path
import java.nio.file.Paths

data class StorageConfig(
    val tempDir: Path,
    val dataDir: Path
)

fun loadStorageConfig(config: ApplicationConfig): StorageConfig =
    StorageConfig(
        tempDir = Paths.get(config.property("storage.temp-dir").getString()),
        dataDir = Paths.get(config.property("storage.data-dir").getString())
    )

