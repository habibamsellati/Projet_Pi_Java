-- ==============================
-- RECLAMATIONS MANAGEMENT TABLES
-- ==============================

-- Table RECLAMATION
CREATE TABLE IF NOT EXISTS reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NULL,
    image VARCHAR(500) NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id INT NOT NULL,
    CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_client_id (client_id),
    INDEX idx_statut (statut),
    INDEX idx_date_creation (date_creation)
);

-- Table REPONSE_RECLAMATION
CREATE TABLE IF NOT EXISTS reponse_reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT NOT NULL,
    date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reclamation_id INT NOT NULL,
    admin_id INT NOT NULL,
    CONSTRAINT fk_reponse_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE,
    CONSTRAINT fk_reponse_admin FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE RESTRICT,
    INDEX idx_reclamation_id (reclamation_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_date_reponse (date_reponse)
);

