CREATE TABLE IF NOT EXISTS bad_words (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mot VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    langue VARCHAR(10) NOT NULL DEFAULT 'fr',
    gravite VARCHAR(10) NOT NULL DEFAULT 'MOYEN',
    ajoute_par VARCHAR(120) NULL,
    date_ajout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS commentaire_moderation_flag (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commentaire_id INT NOT NULL,
    user_id INT NOT NULL,
    mots_detectes VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    date_flag DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flag_commentaire FOREIGN KEY (commentaire_id) REFERENCES commentaire(id) ON DELETE CASCADE,
    CONSTRAINT fk_flag_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

