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