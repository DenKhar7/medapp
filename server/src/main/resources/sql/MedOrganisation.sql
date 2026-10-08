CREATE TABLE med_organisation (
    identifiant VARCHAR(50) PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    pays VARCHAR(100) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Organisations titulaires - source Organisations.csv';