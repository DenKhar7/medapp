CREATE TABLE med_substance (
    code_substance VARCHAR(50) PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    libelle_fr VARCHAR(255) NOT NULL,
    code_sms VARCHAR(50) NULL,
    synonymes_fr TEXT NULL,
    INDEX idx_med_substance_code_sms (code_sms)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Référentiel des substances - source Substances.csv';
-- Pas de clé étrangère vers dictionnaire_sms : les référentiels RUIM et SMS sont mis à jour par des jobs
-- indépendants (le dictionnaire peut être vide ou plus ancien au moment de l'import des substances).
-- La jointure se fait à la lecture (INNER JOIN sur code_sms).
-- On applique la contrainte INDEX sur code_sms puisque nous allons être amenée à faire des jointure
-- entre les tables med_substance et dictionnaire_sms. Nous ferons ces jointures sur code_sms ainsi,
-- pour optimiser ces jointures on peut appliquer une contrainte d'index sur code_sms.
