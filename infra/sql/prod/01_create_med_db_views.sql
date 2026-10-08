-- ============================================================
-- VUE DES SPÉCIALITÉS
-- ============================================================
CREATE DATABASE med_db_views;
USE med_db_views;

CREATE OR REPLACE VIEW med_specialite_view AS
SELECT
    cis,
    uri,
    libelle,
    actif,
    date_debut,
    date_fin,
    code_atc,
    libelle_atc,
    type_procedure,
    statut_courant,
    titulaire_id,
    niveau_virtualisation,
    prescriptibilite_dc,
    forme_manufacturee
FROM med_db_active.med_specialite;


-- ============================================================
-- VUE DES PRÉSENTATIONS
-- ============================================================

CREATE OR REPLACE VIEW med_presentation_view AS
SELECT
    cip13,
    cip7,
    uri,
    cis,
    libelle,
    alt_label,
    quantite_conditionnement,
    unite_conditionnement,
    nb_unite_disp,
    type_dispositif
FROM med_db_active.med_presentation;


-- ============================================================
-- VUE DES SUBSTANCES
-- ============================================================

CREATE OR REPLACE VIEW med_substance_view AS
SELECT
    code_substance,
    uri,
    libelle_fr,
    code_sms,
    synonymes_fr
FROM med_db_active.med_substance;


-- ============================================================
-- VUE DES VOIES D’ADMINISTRATION
-- ============================================================

CREATE OR REPLACE VIEW med_voie_view AS
SELECT
    identifiant,
    uri,
    libelle,
    code_rms,
    code_edqm
FROM med_db_active.med_voie;


-- ============================================================
-- VUE SPÉCIALITÉ ↔ VOIE
-- ============================================================

CREATE OR REPLACE VIEW med_specialite_voie_view AS
SELECT
    cis,
    voie_id
FROM med_db_active.med_specialite_voie;


-- ============================================================
-- VUE DES ÉVÉNEMENTS CIS
-- ============================================================

CREATE OR REPLACE VIEW med_evenement_cis_view AS
SELECT
    evenement,
    uri,
    cis,
    date_effet,
    date_notification,
    type_evenement,
    description
FROM med_db_active.med_evenement_cis;


-- ============================================================
-- VUE DES ÉVÉNEMENTS CIP
-- ============================================================

CREATE OR REPLACE VIEW med_evenement_cip_view AS
SELECT
    evenement,
    uri,
    cip13,
    date_effet,
    date_notification,
    type_evenement,
    description
FROM med_db_active.med_evenement_cip;


-- ============================================================
-- VUE DES COMPOSITIONS QUANTITATIVES
-- ============================================================

CREATE OR REPLACE VIEW med_composition_quantitative_view AS
SELECT
    cis,
    identifiant_element,
    code_substance,
    expression_quantite,
    reference_dosage,
    substance_active,
    fraction_therapeutique
FROM med_db_active.med_composition_quantitative;


-- ============================================================
-- VUE DES COMPOSITIONS QUALITATIVES
-- ============================================================

CREATE OR REPLACE VIEW med_composition_qualitative_view AS
SELECT
    cis,
    identifiant_element,
    relation_substance,
    code_substance
FROM med_db_active.med_composition_qualitative;


-- ============================================================
-- VUE DU DICTIONNAIRE SMS
-- ============================================================

CREATE OR REPLACE VIEW dictionnaire_sms_view AS
SELECT
    code_sms,
    nom_substance,
    is_terme_pref,
    source_nom,
    status_substance,
    type_substance,
    formule_moleculaire,
    poid_moleculaire,
    inchikey,
    last_update_date
FROM med_db_active.dictionnaire_sms;


-- ============================================================
-- VUE DES ORGANISATIONS
-- ============================================================

CREATE OR REPLACE VIEW med_organisation_view AS
SELECT
    identifiant,
    uri,
    libelle,
    pays
FROM med_db_active.med_organisation;


-- ============================================================
-- VUE DES GROUPES GÉNÉRIQUES
-- ============================================================

CREATE OR REPLACE VIEW med_groupe_generique_view AS
SELECT
    identifiant,
    uri,
    libelle
FROM med_db_active.med_groupe_generique;


-- ============================================================
-- VUE DES MEMBRES DE GROUPES GÉNÉRIQUES
-- ============================================================

CREATE OR REPLACE VIEW med_groupe_generique_membre_view AS
SELECT
    identifiant_groupe,
    cis,
    statut
FROM med_db_active.med_groupe_generique_membre;


-- ============================================================
-- VUE DES ÉLÉMENTS DE SPÉCIALITÉ
-- ============================================================

CREATE OR REPLACE VIEW med_element_specialite_view AS
SELECT
    uri,
    cis,
    identifiant_element,
    libelle,
    forme_manufacturee_litterale
FROM med_db_active.med_element_specialite;


-- ============================================================
-- VUE DES ÉLÉMENTS DE PRÉSENTATION
-- ============================================================

CREATE OR REPLACE VIEW med_element_presentation_view AS
SELECT
    uri,
    cip13,
    identifiant_element,
    libelle,
    type_contenant_litteral,
    type_contenant,
    forme_administrable,
    unite_presentation,
    type_dose
FROM med_db_active.med_element_presentation;

-- ============================================================
-- TABLE HISTORIQUES MIGRATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
