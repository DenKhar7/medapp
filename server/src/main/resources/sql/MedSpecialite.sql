CREATE TABLE med_specialite (
    cis INT PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    date_debut DATE NULL,
    date_fin DATE NULL,
    code_atc VARCHAR(15) NULL,
    libelle_atc VARCHAR(255) NULL,
    type_procedure VARCHAR(255) NULL,
    statut_courant VARCHAR(100) NULL,
    titulaire_id VARCHAR(50) NULL,
    niveau_virtualisation VARCHAR(50) NULL,
    prescriptibilite_dc VARCHAR(100) NULL,
    forme_manufacturee VARCHAR(255) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Spécialités pharmaceutiques (niveau CIS) - source CIS.csv';