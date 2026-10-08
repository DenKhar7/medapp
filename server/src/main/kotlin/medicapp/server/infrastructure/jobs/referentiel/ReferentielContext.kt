package medicapp.server.infrastructure.jobs.referentiel

import medicapp.server.config.MedDatabase
import medicapp.server.config.StorageConfig
import medicapp.server.infrastructure.db.enums.DataSourceEnum

// Contexte utile à la création d'un job suivant le référentiel (DataSourceEnum) ciblé
data class ReferentielContext(
    val source: DataSourceEnum, // Référentiel
    val checkUrl: String, // L'url où se trouve la page HTML informant de la dernière version du référentiel
    val downloadUrl: (version: String) -> String, // Construit l'url de téléchargement du ZIP contenant les données source pour une version donnée
    val importAction: suspend (StorageConfig, MedDatabase) -> Unit, // Fonction Important les données CSV dans les tables dans la base de donnée temporaire
)