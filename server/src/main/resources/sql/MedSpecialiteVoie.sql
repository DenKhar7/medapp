CREATE TABLE med_specialite_voie (
    cis INT,
    voie_id VARCHAR(50),
    PRIMARY KEY (cis, voie_id),
    INDEX idx_msv_voie (voie_id),
    FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE,
    FOREIGN KEY (voie_id) REFERENCES med_voie(identifiant) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Lien entre spécialité et voies d’administration';
-- Ici on applique une contrainte d'index sur voie_id puisque qu'à cause de la règle
-- du left-most prefix, l'index est appliqué seulement sur cis et (cis, voie_id) mais pas sur
-- voie_id seule. Pour corriger ce problème on applique un INDEX sur le champ voie_id seul.