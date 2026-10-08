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