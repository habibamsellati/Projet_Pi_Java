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
    produit_id INT NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DOUBLE NOT NULL,
    CONSTRAINT fk_ligne_commande_commande
        FOREIGN KEY (commande_id) REFERENCES commande(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ligne_commande_produit
        FOREIGN KEY (produit_id) REFERENCES produit(id)
        ON DELETE RESTRICT
);

ALTER TABLE commande
    ADD COLUMN IF NOT EXISTS adresse_livraison VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS telephone VARCHAR(30) NOT NULL DEFAULT '';
