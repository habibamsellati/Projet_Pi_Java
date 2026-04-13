-- Table commentaire complète (nouvelle installation). Les noms de tables référencées doivent exister (article, users).

CREATE TABLE IF NOT EXISTS commentaire (
    id INT AUTO_INCREMENT PRIMARY KEY,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    parent_id INT NULL,
    contenu VARCHAR(255) NOT NULL,
    date_pub DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    likes INT NOT NULL DEFAULT 0,
    dislikes INT NOT NULL DEFAULT 0,
    note INT NULL,
    CONSTRAINT fk_commentaire_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    CONSTRAINT fk_commentaire_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_commentaire_parent FOREIGN KEY (parent_id) REFERENCES commentaire(id) ON DELETE CASCADE
);
