-- Schéma minimal pour la gestion des produits recyclables et des propositions

CREATE TABLE IF NOT EXISTS produit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(120) NOT NULL,
    type_materiau VARCHAR(120) NULL,
    etat VARCHAR(50) NULL,
    quantite INT NOT NULL DEFAULT 0,
    origine VARCHAR(150) NULL,
    description TEXT NOT NULL,
    image VARCHAR(1024) NULL,
    impact_ecologique INT NOT NULL DEFAULT 50,
    artisan_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proposition (
    id INT AUTO_INCREMENT PRIMARY KEY,
    produit_id INT NOT NULL,
    user_id INT NOT NULL,
    titre VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    prix_propose DOUBLE NOT NULL DEFAULT 0,
    telephone VARCHAR(40) NULL,
    image VARCHAR(1024) NULL,
    statut VARCHAR(40) NOT NULL DEFAULT 'en_attente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_proposition_produit (produit_id),
    INDEX idx_proposition_user (user_id)
);

