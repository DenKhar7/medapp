CREATE TABLE med_composition_quantitative (
    cis INT NOT NULL,
    identifiant_element VARCHAR(50) NOT NULL,
    code_substance VARCHAR(50) NOT NULL,
    expression_quantite VARCHAR(255) NULL,
    reference_dosage VARCHAR(255) NULL,
    substance_active INT NULL,
    fraction_therapeutique INT NULL,
    PRIMARY KEY (cis, identifiant_element, code_substance),
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Composition quantitative - source Compositions_quantitatives.csv';