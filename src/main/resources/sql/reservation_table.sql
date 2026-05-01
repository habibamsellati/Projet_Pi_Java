-- Exécuter après evenement_table.sql et users_table.sql (FK user_id optionnelle)
CREATE TABLE IF NOT EXISTS reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evenement_id INT NOT NULL,
    user_id INT NULL,
    date_reservation DATETIME NOT NULL,
    nb_places INT NOT NULL DEFAULT 1,
    statut VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nb_places_positive CHECK (nb_places >= 1),
    CONSTRAINT chk_statut_reservation CHECK (statut IN ('en_attente', 'confirme', 'annule')),
    CONSTRAINT fk_reservation_evenement FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
