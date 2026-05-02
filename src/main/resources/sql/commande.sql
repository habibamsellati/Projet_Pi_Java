CREATE TABLE IF NOT EXISTS commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(32) NOT NULL UNIQUE,
    datecommande DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    adresselivraison VARCHAR(255) NOT NULL,
    telephone VARCHAR(20) NULL,
    modepaiement VARCHAR(20) NOT NULL,
    client_id INT NULL,
    message_personnalise LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT false
);

-- Table de liaison optionnelle pour la relation ManyToMany Commande <-> Article
CREATE TABLE IF NOT EXISTS commande_article (
    commande_id INT NOT NULL,
    article_id INT NOT NULL,
    PRIMARY KEY (commande_id, article_id),
    CONSTRAINT fk_commande_article_commande FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE,
    CONSTRAINT fk_commande_article_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
);

