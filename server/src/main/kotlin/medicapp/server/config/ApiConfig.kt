package medicapp.server.config

import io.ktor.server.config.ApplicationConfig

data class ApiConfig(
    val scheme: String,
    val host: String,
    val port: String
) {
    val baseUrl: String get() = "$scheme://$host:$port"
}

fun loadApiConfig(config: ApplicationConfig): ApiConfig = ApiConfig(
    scheme = config.propertyOrNull("ktor.deployment.scheme")?.getString() ?: "http",
    host = config.propertyOrNull("ktor.deployment.host")?.getString() ?: "localhost",
    port = config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8080"
)
