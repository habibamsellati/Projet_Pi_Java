package tn.esprit.services;

import tn.esprit.models.User;

import java.sql.*;

public class UserService {

    private Connection connection;

    public UserService() {
        try {
            String url = "jdbc:mysql://localhost:3306/pi_projet?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String password = "";

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion DB OK");

        } catch (SQLException e) {
            throw new RuntimeException("Erreur connexion DB : " + e.getMessage(), e);
        }
    }

    public void addUser(User user) {
        String query = "INSERT INTO user (nom, prenom, email, motdepasse, role, datecreation, statut, telephone, sexe) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getRole());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setString(7, "actif");
            ps.setInt(8, user.getTelephone());
            ps.setString(9, user.getSexe());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur addUser : " + e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) {
        String query = "SELECT id FROM user WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email.trim());
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur emailExists : " + e.getMessage(), e);
        }
    }

    public User login(String email, String password) {
        String query = "SELECT * FROM user WHERE email = ? AND motdepasse = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur login : " + e.getMessage(), e);
        }

        return null;
    }

    public User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByEmail : " + e.getMessage(), e);
        }

        return null;
    }

    public User findById(int id) {
        String query = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById : " + e.getMessage(), e);
        }

        return null;
    }

    public void updateProfile(User user) {
        String query = "UPDATE user SET nom = ?, prenom = ?, email = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur updateProfile : " + e.getMessage(), e);
        }
    }

    public void updatePassword(int userId, String newPassword) {
        String query = "UPDATE user SET motdepasse = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur updatePassword : " + e.getMessage(), e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("motdepasse"));
        u.setRole(rs.getString("role"));
        u.setTelephone(rs.getInt("telephone"));
        u.setSexe(rs.getString("sexe"));
        u.setStatut(rs.getString("statut"));
        u.setDateCreation(rs.getTimestamp("datecreation"));
        return u;
    }

    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        String query = "SELECT * FROM user ORDER BY id DESC";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAllUsers : " + e.getMessage(), e);
        }

        return users;
    }

    public void deleteUser(int id) {
        String query = "DELETE FROM user WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur deleteUser : " + e.getMessage(), e);
        }
    }

    public void updateUserByAdmin(User user, String newPassword) {
        String queryWithPassword = "UPDATE user SET prenom=?, nom=?, email=?, motdepasse=?, role=?, statut=?, sexe=?, telephone=? WHERE id=?";
        String queryWithoutPassword = "UPDATE user SET prenom=?, nom=?, email=?, role=?, statut=?, sexe=?, telephone=? WHERE id=?";

        try {
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(queryWithPassword)) {
                    ps.setString(1, user.getPrenom());
                    ps.setString(2, user.getNom());
                    ps.setString(3, user.getEmail());
                    ps.setString(4, newPassword.trim());
                    ps.setString(5, user.getRole());
                    ps.setString(6, user.getStatut());
                    ps.setString(7, user.getSexe());
                    ps.setInt(8, user.getTelephone());
                    ps.setInt(9, user.getId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement(queryWithoutPassword)) {
                    ps.setString(1, user.getPrenom());
                    ps.setString(2, user.getNom());
                    ps.setString(3, user.getEmail());
                    ps.setString(4, user.getRole());
                    ps.setString(5, user.getStatut());
                    ps.setString(6, user.getSexe());
                    ps.setInt(7, user.getTelephone());
                    ps.setInt(8, user.getId());
                    ps.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur updateUserByAdmin : " + e.getMessage(), e);
        }
    }
}