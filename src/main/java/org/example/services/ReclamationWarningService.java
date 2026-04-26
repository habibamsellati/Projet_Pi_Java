package org.example.services;

import org.example.models.Reclamation;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Surveille les réclamations non traitées depuis plus de 5 heures
 * et envoie un email d'alerte à tous les admins.
 */
public class ReclamationWarningService {

    private static final int SEUIL_HEURES = 5;
    private static final int INTERVALLE_VERIFICATION_MINUTES = 30; // vérifie toutes les 30 min

    private final EmailService emailService;
    private final Connection connection;
    private ScheduledExecutorService scheduler;

    public ReclamationWarningService() {
        this.emailService = new EmailService();
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ── Démarrage / Arrêt ────────────────────────────────────────────────────

    public void demarrer() {
        if (scheduler != null && !scheduler.isShutdown()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reclamation-warning-thread");
            t.setDaemon(true); // ne bloque pas la fermeture de l'app
            return t;
        });

        // Première vérification après 1 minute, puis toutes les 30 minutes
        scheduler.scheduleAtFixedRate(
                this::verifierEtAlerter,
                1,
                INTERVALLE_VERIFICATION_MINUTES,
                TimeUnit.MINUTES
        );

        System.out.println("[ReclamationWarning] Surveillance démarrée (seuil=" + SEUIL_HEURES + "h, intervalle=" + INTERVALLE_VERIFICATION_MINUTES + "min)");
    }

    public void arreter() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            System.out.println("[ReclamationWarning] Surveillance arrêtée.");
        }
    }

    // ── Logique principale ───────────────────────────────────────────────────

    public void verifierEtAlerter() {
        try {
            List<Reclamation> nonTraitees = obtenirReclamationsNonTraitees();
            if (nonTraitees.isEmpty()) {
                System.out.println("[ReclamationWarning] Aucune réclamation en attente > " + SEUIL_HEURES + "h.");
                return;
            }

            List<String> emailsAdmins = obtenirEmailsAdmins();
            if (emailsAdmins.isEmpty()) {
                System.err.println("[ReclamationWarning] Aucun admin trouvé en base de données.");
                return;
            }

            System.out.println("[ReclamationWarning] " + nonTraitees.size() + " réclamation(s) non traitée(s) > " + SEUIL_HEURES + "h → envoi alerte à " + emailsAdmins.size() + " admin(s).");

            for (String emailAdmin : emailsAdmins) {
                String subject = "⚠️ Alerte - " + nonTraitees.size() + " réclamation(s) non traitée(s) depuis +" + SEUIL_HEURES + "h";
                String html = buildWarningHtml(nonTraitees);
                emailService.envoyerEmailWarning(emailAdmin, subject, html);
            }

        } catch (Exception e) {
            System.err.println("[ReclamationWarning] Erreur lors de la vérification: " + e.getMessage());
        }
    }

    // ── Requêtes SQL ─────────────────────────────────────────────────────────

    /**
     * Réclamations en_attente ou en_cours, sans aucune réponse, depuis plus de SEUIL_HEURES.
     */
    private List<Reclamation> obtenirReclamationsNonTraitees() throws SQLException {
        List<Reclamation> liste = new ArrayList<>();

        String sql = "SELECT r.id, r.titre, r.description, r.statut, r.date_creation, " +
                     "CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient, " +
                     "u.email AS emailClient, " +
                     "TIMESTAMPDIFF(HOUR, r.date_creation, NOW()) AS heures_ecoulees " +
                     "FROM reclamation r " +
                     "LEFT JOIN user u ON r.client_id = u.id " +
                     "LEFT JOIN reponse_reclamation rep ON r.id = rep.reclamation_id " +
                     "WHERE r.statut IN ('en_attente', 'en_cours') " +
                     "AND rep.id IS NULL " +
                     "AND TIMESTAMPDIFF(HOUR, r.date_creation, NOW()) >= ? " +
                     "ORDER BY r.date_creation ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, SEUIL_HEURES);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reclamation r = new Reclamation();
                    r.setId(rs.getInt("id"));
                    r.setTitre(rs.getString("titre"));
                    r.setDescription(rs.getString("description"));
                    r.setStatut(rs.getString("statut"));
                    r.setDateCreation(rs.getTimestamp("date_creation"));

                    // Infos client
                    org.example.models.User client = new org.example.models.User();
                    client.setNom(rs.getString("nomClient"));
                    client.setEmail(rs.getString("emailClient"));
                    r.setClient(client);

                    // Stocker les heures écoulées dans le titre temporairement pour l'affichage
                    int heures = rs.getInt("heures_ecoulees");
                    r.setTitre(rs.getString("titre") + " [" + heures + "h sans réponse]");

                    liste.add(r);
                }
            }
        }
        return liste;
    }

    /**
     * Récupère les emails de tous les admins en base.
     */
    private List<String> obtenirEmailsAdmins() throws SQLException {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT email FROM user WHERE role = 'ADMIN' AND email IS NOT NULL AND email != ''";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String email = rs.getString("email");
                    if (email != null && !email.isBlank()) {
                        emails.add(email.trim());
                    }
                }
            }
        }
        return emails;
    }

    // ── Template HTML ────────────────────────────────────────────────────────

    private String buildWarningHtml(List<Reclamation> reclamations) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#fff8f0;padding:24px;color:#333;'>");
        sb.append("<div style='max-width:700px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:2px solid #ff6b35;'>");

        // Header alerte
        sb.append("<div style='background:#ff6b35;color:#fff;padding:20px 24px;'>");
        sb.append("<h1 style='margin:0;font-size:20px;'>⚠️ Alerte Réclamations Non Traitées</h1>");
        sb.append("<p style='margin:6px 0 0;font-size:13px;opacity:.9;'>")
          .append(reclamations.size()).append(" réclamation(s) sans réponse depuis plus de ").append(SEUIL_HEURES).append(" heures</p>");
        sb.append("</div>");

        // Corps
        sb.append("<div style='padding:24px;'>");
        sb.append("<p style='margin:0 0 16px;'>Bonjour Admin,</p>");
        sb.append("<p style='margin:0 0 20px;line-height:1.6;'>Les réclamations suivantes n'ont reçu <strong>aucune réponse</strong> depuis plus de <strong>")
          .append(SEUIL_HEURES).append(" heures</strong>. Veuillez les traiter dès que possible.</p>");

        // Tableau des réclamations
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:14px;'>");
        sb.append("<thead>");
        sb.append("<tr style='background:#fff3ee;'>");
        sb.append("<th style='padding:10px;text-align:left;border-bottom:2px solid #ff6b35;'>ID</th>");
        sb.append("<th style='padding:10px;text-align:left;border-bottom:2px solid #ff6b35;'>Titre</th>");
        sb.append("<th style='padding:10px;text-align:left;border-bottom:2px solid #ff6b35;'>Client</th>");
        sb.append("<th style='padding:10px;text-align:left;border-bottom:2px solid #ff6b35;'>Statut</th>");
        sb.append("<th style='padding:10px;text-align:left;border-bottom:2px solid #ff6b35;'>Date création</th>");
        sb.append("</tr>");
        sb.append("</thead><tbody>");

        for (Reclamation r : reclamations) {
            String nomClient = (r.getClient() != null && r.getClient().getNom() != null)
                    ? r.getClient().getNom().trim() : "Inconnu";
            String dateStr = r.getFormattedDate();
            String statutColor = "en_attente".equals(r.getStatut()) ? "#e67e22" : "#2980b9";

            sb.append("<tr style='border-bottom:1px solid #f0e0d6;'>");
            sb.append("<td style='padding:10px;font-weight:bold;color:#ff6b35;'>#").append(r.getId()).append("</td>");
            sb.append("<td style='padding:10px;'>").append(esc(r.getTitre())).append("</td>");
            sb.append("<td style='padding:10px;'>").append(esc(nomClient)).append("</td>");
            sb.append("<td style='padding:10px;'><span style='background:").append(statutColor)
              .append(";color:#fff;padding:3px 8px;border-radius:4px;font-size:12px;'>")
              .append(esc(r.getStatut())).append("</span></td>");
            sb.append("<td style='padding:10px;color:#888;font-size:13px;'>").append(dateStr).append("</td>");
            sb.append("</tr>");
        }

        sb.append("</tbody></table>");

        sb.append("<div style='margin-top:24px;padding:14px;background:#fff3ee;border-left:4px solid #ff6b35;border-radius:4px;'>");
        sb.append("<p style='margin:0;font-size:13px;color:#c0392b;'>");
        sb.append("<strong>Action requise :</strong> Connectez-vous à l'application AfkArt et traitez ces réclamations.");
        sb.append("</p></div>");

        sb.append("<p style='margin-top:20px;color:#888;font-size:12px;'>Cet email est envoyé automatiquement par le système AfkArt.</p>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
