CREATE TABLE med_presentation (
    cip13 VARCHAR(13) PRIMARY KEY,
    cip7 VARCHAR(7) NULL,
    uri VARCHAR(255) NOT NULL,
    cis INT NOT NULL,
    libelle VARCHAR(510) NOT NULL,
    alt_label VARCHAR(255) NULL,
    quantite_conditionnement DECIMAL(10,2) NULL,
    unite_conditionnement VARCHAR(50) NULL,
    nb_unite_disp INT NULL,
    type_dispositif VARCHAR(200) NULL,
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Présentations (niveau CIP13) - source CIP.csv';