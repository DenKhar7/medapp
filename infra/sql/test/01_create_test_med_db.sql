-- CREATE DATABASE 
CREATE DATABASE test_med_db;
USE test_med_db;

-- ============================================================
-- TABLE DES SPÉCIALITÉS
-- ============================================================

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


-- ============================================================
-- TABLE DES VOIES D’ADMINISTRATION
-- ============================================================

CREATE TABLE med_voie (
identifiant VARCHAR(50) PRIMARY KEY,
uri VARCHAR(255) NOT NULL,
libelle VARCHAR(255) NOT NULL,
code_rms VARCHAR(50) NULL,
code_edqm VARCHAR(50) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Voies d’administration - source Voies.csv';


-- ============================================================
-- RELATION N-N SPÉCIALITÉ ↔ VOIE
-- ============================================================

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

-- ============================================================
-- TABLE DES PRÉSENTATIONS
-- ============================================================

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


-- ============================================================
-- TABLE DES ÉVÉNEMENTS RÉGLEMENTAIRES CONCERNANT DES SPÉCIALITÉS
-- ============================================================

CREATE TABLE med_evenement_cis (
evenement VARCHAR(255) PRIMARY KEY,
uri VARCHAR(255),
cis INT NOT NULL,
date_effet DATE NULL,
date_notification DATE NULL,
type_evenement VARCHAR(100) NULL,
description TEXT NULL,
INDEX idx_evenement_cis (cis), -- contrainte d'index sur le champ de filtrage
FOREIGN KEY (cis) REFERENCES med_specialite(cis) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historique des événements CIS - sources Evenements_CIS.csv';

-- ============================================================
-- TABLE DES ÉVÉNEMENTS RÉGLEMENTAIRES CONCERNANT DES PRÉSENTATIONS
-- ============================================================

CREATE TABLE med_evenement_cip (
evenement VARCHAR(255) PRIMARY KEY,
uri VARCHAR(255),
cip13 VARCHAR(13) NOT NULL,
date_effet DATE NULL,
date_notification DATE NULL,
type_evenement VARCHAR(100) NULL,
description TEXT NULL,
INDEX idx_evenement_cis (cip13), -- contrainte d'index sur le champ de filtrage
FOREIGN KEY (cip13) REFERENCES med_presentation(cip13) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Historique des événements CIP - sources Evenements_CIP.csv';


-- ============================================================
-- TABLE DES COMPOSITIONS QUALITATIVES
-- ============================================================

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


-- ============================================================
-- TABLE DES COMPOSITIONS QUANTITATIVES
-- ============================================================

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

-- ============================================================
-- TABLE DICTIONNAIRE CODE SMS
-- ============================================================

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

-- ============================================================
-- TABLE DES SUBSTANCES
-- ============================================================

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
-- On applique la contrainte INDEX sur code_sms puisque nous allons être amenée à faire des jointure
-- entre les tables med_substance et dictionnaire_sms. Nous ferons ces jointures sur code_sms ainsi,
-- pour optimiser ces jointures on peut appliquer une contrainte d'index sur code_sms.


-- ============================================================
-- TABLE DES ORGANISATIONS (LABORATOIRES)
-- ============================================================

CREATE TABLE med_organisation (
identifiant VARCHAR(50) PRIMARY KEY,
uri VARCHAR(255) NOT NULL,
libelle VARCHAR(255) NOT NULL,
pays VARCHAR(100) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Organisations titulaires - source Organisations.csv';


-- ============================================================
-- TABLE DES GROUPES GÉNÉRIQUES
-- ============================================================

CREATE TABLE med_groupe_generique (
identifiant VARCHAR(50) PRIMARY KEY,
uri VARCHAR(255) NOT NULL,
libelle VARCHAR(510) NOT NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Groupes génériques - source GroupeGenerique.csv';


-- ============================================================
-- ASSOCIATION CIS ↔ GROUPE GÉNÉRIQUE
-- ============================================================

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

-- ============================================================
-- TABLE ELEMENT SPECIALITE
-- ============================================================

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


-- ============================================================
-- TABLE ELEMENT PRESENTATION
-- ============================================================

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

-- ============================================================
-- TABLE VERSION REFERENTIEL
-- ============================================================

CREATE TABLE referentiel_import_state (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(50) NOT NULL,
  version VARCHAR(100) NOT NULL,
  file_hash CHAR(64) NOT NULL,
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('SUCCESS', 'FAILED') NOT NULL,
  message TEXT NULL,  -- message d'erreur ou info complémentaire

  UNIQUE KEY uk_source_version (source, version),
  UNIQUE KEY uk_source_hash (source, file_hash),
  INDEX idx_source_imported_at (source, imported_at)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;