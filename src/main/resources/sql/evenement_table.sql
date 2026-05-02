CREATE TABLE IF NOT EXISTS evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    artisan VARCHAR(255) NULL,
    description TEXT NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    capacite INT NOT NULL,
    type_art VARCHAR(255) NULL,
    theme VARCHAR(255) NULL,
    prix DECIMAL(12, 2) NULL,
    image VARCHAR(500) NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'brouillon',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_evenement_capacite CHECK (capacite >= 1 AND capacite <= 10000),
    CONSTRAINT chk_evenement_dates CHECK (date_fin >= date_debut)
);
