CREATE TABLE med_groupe_generique_membre (
    identifiant_groupe VARCHAR(50) NOT NULL,
    cis INT NOT NULL,
    statut VARCHAR(100) NULL,
    PRIMARY KEY (identifiant_groupe, cis),
    FOREIGN KEY (identifiant_groupe) REFERENCES med_groupe_generique(identifiant) ON DELETE CASCADE,
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Lien entre groupes génériques et spécialités - source Correspondance-GroupeGenerique-CIS.csv';
