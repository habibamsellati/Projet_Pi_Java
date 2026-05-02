CREATE TABLE IF NOT EXISTS livraison (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id INT NOT NULL,
    client_id INT NOT NULL,
    livreur_id INT NULL,
    adresse_livraison VARCHAR(255) NOT NULL,
    statut VARCHAR(32) NOT NULL DEFAULT 'en_attente',
    tracking_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_livraison_client (client_id),
    INDEX idx_livraison_livreur (livreur_id),
    INDEX idx_livraison_statut (statut),
    INDEX idx_livraison_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

