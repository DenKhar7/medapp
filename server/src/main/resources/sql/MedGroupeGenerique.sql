CREATE TABLE med_groupe_generique (
    identifiant VARCHAR(50) PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    libelle VARCHAR(510) NOT NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Groupes génériques - source GroupeGenerique.csv';