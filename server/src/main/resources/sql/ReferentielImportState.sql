CREATE TABLE referentiel_import_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    version VARCHAR(100) NOT NULL,
    file_hash CHAR(64) NOT NULL,
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    message TEXT NULL,  -- message d'erreur ou info complémentaire

    UNIQUE KEY uk_source_version (source, version),
    UNIQUE KEY uk_source_hash (source, file_hash),
    INDEX idx_source_imported_at (source, imported_at)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;