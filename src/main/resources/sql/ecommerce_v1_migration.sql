-- Migration V1 e-commerce (exécuter sur la base `piprojet1` ou équivalent après sauvegarde).
-- Adapter / ignorer les lignes qui échouent si la colonne existe déjà.

-- Commande : numéro de suivi + mode de paiement
ALTER TABLE commande ADD COLUMN numero VARCHAR(64) NULL;
ALTER TABLE commande ADD COLUMN mode_paiement VARCHAR(64) NOT NULL DEFAULT 'non spécifié';

-- Article : date de publication catalogue
ALTER TABLE article ADD COLUMN date_publication DATETIME NULL;
UPDATE article SET date_publication = NOW() WHERE date_publication IS NULL;

-- Commentaires (si table déjà créée à la main — sinon utiliser commentaire_table_v1.sql)
ALTER TABLE commentaire ADD COLUMN parent_id INT NULL;
ALTER TABLE commentaire ADD COLUMN date_pub DATETIME NULL;
ALTER TABLE commentaire ADD COLUMN likes INT NOT NULL DEFAULT 0;
ALTER TABLE commentaire ADD COLUMN dislikes INT NOT NULL DEFAULT 0;

-- Optionnel : index pour filtrage artisan
-- CREATE INDEX idx_ligne_commande_article ON ligne_commande(article_id);
