CREATE TABLE med_element_presentation (
    uri VARCHAR(255) NOT NULL,
    cip13 VARCHAR(13) NOT NULL,
    identifiant_element INT NOT NULL,
    libelle VARCHAR(510) NOT NULL,
    type_contenant_litteral TEXT NOT NULL,
    type_contenant VARCHAR(255) NULL,
    forme_administrable TEXT NULL,
    unite_presentation TEXT NULL,
    type_dose VARCHAR(50) NULL,

    PRIMARY KEY (cip13, identifiant_element),
    FOREIGN KEY (cip13) REFERENCES med_presentation(cip13) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Éléments par présentation - source Elements_CIP.csv';
