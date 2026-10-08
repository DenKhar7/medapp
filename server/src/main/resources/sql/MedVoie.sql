CREATE TABLE med_voie (
    identifiant VARCHAR(50) PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    code_rms VARCHAR(50) NULL,
    code_edqm VARCHAR(50) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Voies d’administration - source Voies.csv';