CREATE TABLE med_element_specialite (
    uri VARCHAR(255) NOT NULL,
    cis INT NOT NULL,
    identifiant_element INT NOT NULL,
    libelle VARCHAR(510) NOT NULL,
    forme_manufacturee_litterale TEXT NOT NULL,

    PRIMARY KEY (cis, identifiant_element),
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Éléments par spécialité - source Elements.csv';