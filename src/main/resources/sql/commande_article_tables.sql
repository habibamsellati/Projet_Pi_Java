CREATE TABLE IF NOT EXISTS commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    date_commande DATETIME NOT NULL,
    total DOUBLE NOT NULL,
    statut VARCHAR(50) NOT NULL,
    adresse_livraison VARCHAR(255) NOT NULL,
    telephone VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS ligne_commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id INT NOT NULL,
    article_id INT NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DOUBLE NOT NULL,
    CONSTRAINT fk_lc_commande
        FOREIGN KEY (commande_id) REFERENCES commande(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lc_article
        FOREIGN KEY (article_id) REFERENCES article(id)
        ON DELETE RESTRICT
);

-- Optionnel (recommandé) si vous voulez gérer article pièce unique ou stock
-- ALTER TABLE article ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'disponible';
-- ALTER TABLE article ADD COLUMN quantite INT NOT NULL DEFAULT 1;
