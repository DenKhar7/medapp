CREATE TABLE dictionnaire_sms (
    code_sms VARCHAR(50) NOT NULL,
    nom_substance VARCHAR(2048) NOT NULL,
    is_terme_pref BOOLEAN,
    source_nom VARCHAR(255),
    status_substance VARCHAR(255),
    type_substance VARCHAR(255),
    formule_moleculaire VARCHAR(50),
    poid_moleculaire FLOAT,
    inchikey VARCHAR(50),
    last_update_date DATE,
    PRIMARY KEY (code_sms),
    INDEX idx_dictionnaire_sms_nom_substance (nom_substance)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Dictionnaire des code SMS - source SMS_Dictionnaire.csv';
-- Ici on applique une contrainte d'index sur nom_substance puisque qu'à cause de la règle
-- du left-most prefix, l'index est appliqué seulement sur code_sms et (code_sms, nom_substance) mais pas sur
-- nom_substance seule. Pour corriger ce problème on applique un INDEX sur le champ nom_substance seul.
