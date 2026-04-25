package org.example.services;

import org.example.models.Reservation;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation {
    public static final String STATUT_EN_ATTENTE = "en_attente";
    public static final String STATUT_CONFIRME = "confirme";
    public static final String STATUT_ANNULE = "annule";

    private final Connection connection;

    public ServiceReservation() {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Connexion MySQL non disponible.");
        }
        // Auto-clean expired pending reservations
        annulerReservationsExpirees();
    }

    public void annulerReservationsExpirees() {
        String sql = "UPDATE reservation SET statut = ? WHERE statut = ? AND TIMESTAMPDIFF(HOUR, created_at, NOW()) >= 3";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, STATUT_ANNULE);
            ps.setString(2, STATUT_EN_ATTENTE);
            ps.executeUpdate();
        } catch (SQLException ignore) {
        }
    }

    /**
     * Règle métier : somme(nb_places) pour en_attente + confirme + nouvelle demande
     * <= capacité événement.
     */
    public void ajouter(Reservation r) throws SQLException {
        validerChamps(r);
        r.setStatut(normalizeStatut(r.getStatut()));

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String lockSql = "SELECT id, capacite FROM evenement WHERE id = ? FOR UPDATE";
            int capacite;
            try (PreparedStatement ps = connection.prepareStatement(lockSql)) {
                ps.setInt(1, r.getEvenementId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Événement introuvable.");
                    }
                    capacite = rs.getInt("capacite");
                }
            }

            int placesPrises = sommePlacesReserveesActives(r.getEvenementId());
            if (placesPrises + r.getNbPlaces() > capacite) {
                throw new IllegalStateException(
                        "Capacité insuffisante. Places déjà réservées (en attente / confirmées): " + placesPrises
                                + ", capacité: " + capacite + ".");
            }

            String sql = "INSERT INTO reservation (evenement_id, user_id, date_reservation, nb_places, statut, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, r.getEvenementId());
                if (r.getUserId() == null) {
                    ps.setNull(2, Types.INTEGER);
                } else {
                    ps.setInt(2, r.getUserId());
                }
                ps.setTimestamp(3, Timestamp.valueOf(r.getDateReservation()));
                ps.setInt(4, r.getNbPlaces());
                ps.setString(5, r.getStatut());
                ps.executeUpdate();
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            if (e instanceof SQLException sqlEx)
                throw sqlEx;
            throw new SQLException(e.getMessage(), e);
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    /** Places comptées pour en_attente et confirme uniquement. */
    public int sommePlacesReserveesActives(int evenementId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(nb_places), 0) AS s FROM reservation WHERE evenement_id = ? AND statut IN (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ps.setString(2, STATUT_EN_ATTENTE);
            ps.setString(3, STATUT_CONFIRME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("s");
                return 0;
            }
        }
    }

    public void mettreAJourStatut(int reservationId, String statut) throws SQLException {
        String normalized = normalizeStatut(statut);
        String sql = "UPDATE reservation SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, reservationId);
            int n = ps.executeUpdate();
            if (n == 0)
                throw new IllegalArgumentException("Réservation introuvable.");
        }
    }

    /**
     * Annulation client : passe en statut annule (libère les places pour le
     * calcul).
     */
    public void annulerParClient(int reservationId, int userId) throws SQLException {
        String sql = "UPDATE reservation SET statut = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, STATUT_ANNULE);
            ps.setInt(2, reservationId);
            ps.setInt(3, userId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new IllegalArgumentException("Réservation introuvable ou n'appartient pas à cet utilisateur.");
            }
        }
    }

    public void supprimerPhysiqueParClient(int reservationId, int userId) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, userId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new IllegalArgumentException("Réservation introuvable ou n'appartient pas à cet utilisateur.");
            }
        }
    }

    public void supprimer(int reservationId) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    public List<Reservation> afficherParEvenement(int evenementId) throws SQLException {
        return lister("WHERE evenement_id = ? ORDER BY created_at DESC", evenementId);
    }

    public List<Reservation> listerTous() throws SQLException {
        List<Reservation> list = new ArrayList<Reservation>();
        String sql = "SELECT * FROM reservation ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        }
        return list;
    }

    public List<Reservation> listerParUtilisateur(int userId) throws SQLException {
        List<Reservation> list = new ArrayList<Reservation>();
        String sql = "SELECT * FROM reservation WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        }
        return list;
    }

    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    public int compterParStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation WHERE statut = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return 0;
    }

    public int sommePlacesReserveesTotales() throws SQLException {
        String sql = "SELECT SUM(nb_places) FROM reservation WHERE statut != 'annule'";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    public double calculerRevenuTotal() throws SQLException {
        String sql = "SELECT SUM(r.nb_places * e.prix) FROM reservation r " +
                "JOIN evenement e ON r.evenement_id = e.id " +
                "WHERE r.statut = 'confirme'";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return rs.getDouble(1);
        }
        return 0;
    }

    private List<Reservation> lister(String whereAndOrder, int evenementId) throws SQLException {
        List<Reservation> list = new ArrayList<Reservation>();
        String sql = "SELECT * FROM reservation " + whereAndOrder;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        }
        return list;
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setEvenementId(rs.getInt("evenement_id"));
        int uid = rs.getInt("user_id");
        r.setUserId(rs.wasNull() ? null : uid);
        Timestamp ts = rs.getTimestamp("date_reservation");
        if (ts != null)
            r.setDateReservation(ts.toLocalDateTime());
        r.setNbPlaces(rs.getInt("nb_places"));
        r.setStatut(rs.getString("statut"));
        ts = rs.getTimestamp("created_at");
        if (ts != null)
            r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }

    private void validerChamps(Reservation r) {
        if (r.getEvenementId() <= 0) {
            throw new IllegalArgumentException("evenement_id invalide.");
        }
        if (r.getDateReservation() == null) {
            throw new IllegalArgumentException("La date de réservation est obligatoire.");
        }
        if (r.getDateReservation().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de réservation ne peut pas être dans le passé.");
        }
        if (r.getNbPlaces() < 1) {
            throw new IllegalArgumentException("Le nombre de places doit être au minimum 1.");
        }
    }

    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank())
            return STATUT_EN_ATTENTE;
        String s = statut.trim().toLowerCase();
        if (!s.equals(STATUT_EN_ATTENTE) && !s.equals(STATUT_CONFIRME) && !s.equals(STATUT_ANNULE)) {
            throw new IllegalArgumentException("Statut invalide. Valeurs: en_attente, confirme, annule.");
        }
        return s;
    }
}
