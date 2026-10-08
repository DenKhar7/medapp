CREATE TABLE med_composition_qualitative (
    cis INT NOT NULL,
    identifiant_element VARCHAR(50) NOT NULL,
    relation_substance VARCHAR(50) NOT NULL,
    code_substance VARCHAR(50) NOT NULL,
    PRIMARY KEY (cis, identifiant_element, code_substance),
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Composition qualitative - source Compositions_qualitatives.csv';