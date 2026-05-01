package org.example.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.models.Article;
import org.example.models.Commande;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromEmail;
    private final String fromName;
    private final boolean smtpEnabled;

    public EmailService() {
        Properties props = loadProperties();

        this.smtpHost     = props.getProperty("email.smtp.host",     "smtp.gmail.com");
        this.smtpPort     = props.getProperty("email.smtp.port",     "587");
        this.smtpUsername = props.getProperty("email.smtp.username", "");
        this.smtpPassword = props.getProperty("email.smtp.password", "");
        this.fromEmail    = props.getProperty("email.from",          smtpUsername);
        this.fromName     = props.getProperty("email.from.name",     "AfkArt");

        // SMTP actif uniquement si username ET password sont renseignés et non vides
        this.smtpEnabled = !smtpUsername.isBlank() && !smtpPassword.isBlank()
                && !smtpPassword.equals("VOTRE_MOT_DE_PASSE_APPLICATION");

        if (smtpEnabled) {
            System.out.println("[EmailService] Mode SMTP actif -> " + fromEmail);
        } else {
            System.out.println("[EmailService] SMTP non configure -> mode preview console");
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────

    public boolean envoyerConfirmationCommande(Commande commande, String emailClient) {
        if (commande == null || emailClient == null || emailClient.isBlank()) return false;
        String subject = "Confirmation de commande n°" + nvl(commande.getNumero(), "--") + " - AfkArt";
        String html    = buildConfirmationHtml(commande);
        return send(emailClient, subject, html);
    }

    public boolean envoyerInvitationMeetReclamation(String emailClient, String clientNom,
                                                    String titreReclamation, String meetUrl) {
        if (emailClient == null || emailClient.isBlank()) return false;
        if (meetUrl == null || meetUrl.isBlank()) return false;
        String subject = "Invitation visioconference - Reclamation AfkArt";
        String html    = buildMeetHtml(clientNom, titreReclamation, meetUrl);
        return send(emailClient, subject, html);
    }

    public boolean envoyerEmailWarning(String emailAdmin, String subject, String html) {
        if (emailAdmin == null || emailAdmin.isBlank()) return false;
        return send(emailAdmin, subject, html);
    }

    public boolean envoyerStatutProposition(String emailClient, String titreProposition,
                                             String nomProduit, String statut, double prix) {
        if (emailClient == null || emailClient.isBlank()) return false;
        String subject = switch (statut) {
            case "acceptee" -> "Votre proposition a été acceptée - AfkArt";
            case "refusee"  -> "Votre proposition a été refusée - AfkArt";
            case "terminee" -> "Votre oeuvre est terminée - AfkArt";
            default         -> "Mise à jour de votre proposition - AfkArt";
        };
        String html = buildStatutPropositionHtml(titreProposition, nomProduit, statut, prix);
        return send(emailClient, subject, html);
    }

    public boolean envoyerReponseReclamation(String emailClient, String nomClient,
                                              String titreReclamation, String contenuReponse,
                                              String nomAdmin) {
        if (emailClient == null || emailClient.isBlank()) return false;
        String subject = "Réponse à votre réclamation - AfkArt";
        String html    = buildReponseReclamationHtml(nomClient, titreReclamation, contenuReponse, nomAdmin);
        return send(emailClient, subject, html);
    }

    public boolean isSmtpEnabled() { return smtpEnabled; }

    // ── Envoi ─────────────────────────────────────────────────────────────────

    private boolean send(String to, String subject, String html) {
        if (smtpEnabled) {
            try {
                sendViaSmtp(to, subject, html);
                System.out.println("[EmailService] Email envoye a " + to);
                return true;
            } catch (Exception e) {
                System.err.println("[EmailService] Echec SMTP: " + e.getMessage());
                System.err.println("[EmailService] Verifiez votre mot de passe d'application Gmail dans email.properties");
            }
        }
        // Toujours afficher en console (preview) si SMTP inactif ou en erreur
        printPreview(to, subject, html);
        return false;
    }

    private void sendViaSmtp(String to, String subject, String html) throws Exception {
        Properties p = new Properties();
        p.put("mail.smtp.host",                 smtpHost);
        p.put("mail.smtp.port",                 smtpPort);
        p.put("mail.smtp.auth",                 "true");
        p.put("mail.smtp.starttls.enable",      "true");
        p.put("mail.smtp.connectiontimeout",    "10000");
        p.put("mail.smtp.timeout",              "10000");
        p.put("mail.smtp.writetimeout",         "10000");

        Session session = Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setContent(html, "text/html; charset=UTF-8");
        Transport.send(msg);
    }

    // ── Templates HTML ────────────────────────────────────────────────────────

    private String buildConfirmationHtml(Commande commande) {
        String nom     = nvl(commande.getNomClient(), "Client");
        String numero  = nvl(commande.getNumero(), "--");
        double total   = commande.getTotal() == null ? 0.0 : commande.getTotal();
        String message = nvl(commande.getMessagePersonnalise(), "Merci pour votre confiance !");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:24px;color:#2f2f2f;'>");
        sb.append("<div style='max-width:680px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #e5e7ef;'>");

        // Header
        sb.append("<div style='background:#1f3b73;color:#fff;padding:24px;'>");
        sb.append("<h1 style='margin:0;font-size:22px;'>Commande confirmee !</h1>");
        sb.append("<p style='margin:6px 0 0;font-size:13px;opacity:.9;'>Commande n° ").append(esc(numero)).append("</p>");
        sb.append("</div>");

        // Corps
        sb.append("<div style='padding:24px;'>");
        sb.append("<p>Bonjour <strong>").append(esc(nom)).append("</strong>,</p>");

        // Message personnalise
        sb.append("<div style='background:#f0f4ff;border-left:4px solid #1f3b73;padding:14px 16px;border-radius:6px;margin:16px 0;'>");
        sb.append("<p style='margin:0;line-height:1.7;'>").append(esc(message)).append("</p>");
        sb.append("</div>");

        // Details
        sb.append("<h3 style='color:#1f3b73;margin:20px 0 10px;'>Details de la commande</h3>");
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:14px;'>");
        sb.append("<tr><td style='padding:6px 0;color:#666;'>Numero</td><td style='padding:6px 0;font-weight:bold;'>").append(esc(numero)).append("</td></tr>");
        sb.append("<tr><td style='padding:6px 0;color:#666;'>Total</td><td style='padding:6px 0;font-weight:bold;'>").append(String.format("%.2f DT", total)).append("</td></tr>");
        sb.append("<tr><td style='padding:6px 0;color:#666;'>Adresse</td><td style='padding:6px 0;'>").append(esc(nvl(commande.getAdresseLivraison(), "--"))).append("</td></tr>");
        sb.append("</table>");

        // Articles
        if (commande.getArticles() != null && !commande.getArticles().isEmpty()) {
            sb.append("<h3 style='color:#1f3b73;margin:20px 0 10px;'>Articles commandes</h3>");
            sb.append("<ul style='padding-left:18px;margin:0;'>");
            for (Article a : commande.getArticles()) {
                if (a != null) {
                    sb.append("<li style='margin-bottom:4px;'>")
                      .append(esc(nvl(a.getTitre(), "Article")))
                      .append(" — <strong>")
                      .append(a.getPrix() == null ? "0.00 DT" : String.format("%.2f DT", a.getPrix()))
                      .append("</strong></li>");
                }
            }
            sb.append("</ul>");
        }

        sb.append("<p style='margin-top:24px;color:#555;'>Merci pour votre achat !<br/><strong>Equipe AfkArt</strong></p>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private String buildStatutPropositionHtml(String titre, String nomProduit, String statut, double prix) {
        String couleur = switch (statut) {
            case "acceptee" -> "#1e8e3e";
            case "refusee"  -> "#d93025";
            case "terminee" -> "#7a5c3a";
            default         -> "#1f3b73";
        };
        String emoji = switch (statut) {
            case "acceptee" -> "✅";
            case "refusee"  -> "❌";
            case "terminee" -> "🎉";
            default         -> "ℹ️";
        };
        String message = switch (statut) {
            case "acceptee" -> "Votre proposition <strong>" + esc(titre) + "</strong> pour le produit <strong>"
                    + esc(nomProduit) + "</strong> a été <strong>ACCEPTÉE</strong>. "
                    + "Prix convenu : <strong>" + String.format("%.2f TND", prix) + "</strong>. "
                    + "L'artisan vous contactera prochainement.";
            case "refusee"  -> "Votre proposition <strong>" + esc(titre) + "</strong> pour le produit <strong>"
                    + esc(nomProduit) + "</strong> a été <strong>REFUSÉE</strong>. "
                    + "Vous pouvez soumettre une nouvelle proposition sur AfkArt.";
            case "terminee" -> "Bonne nouvelle ! Votre oeuvre réalisée à partir de <strong>"
                    + esc(nomProduit) + "</strong> est <strong>TERMINÉE</strong>. "
                    + "Contactez l'artisan pour la récupération.";
            default         -> "Le statut de votre proposition <strong>" + esc(titre) + "</strong> a été mis à jour.";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:24px;color:#2f2f2f;'>");
        sb.append("<div style='max-width:620px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #e5e7ef;'>");
        sb.append("<div style='background:").append(couleur).append(";color:#fff;padding:22px 24px;'>");
        sb.append("<h1 style='margin:0;font-size:20px;'>").append(emoji).append(" Mise à jour de votre proposition</h1>");
        sb.append("</div>");
        sb.append("<div style='padding:24px;'>");
        sb.append("<p style='line-height:1.7;font-size:15px;'>").append(message).append("</p>");
        sb.append("<p style='margin-top:20px;color:#555;'>Merci pour votre confiance.<br/><strong>Équipe AfkArt</strong></p>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private String buildReponseReclamationHtml(String nomClient, String titreReclamation,
                                                String contenuReponse, String nomAdmin) {
        String nom    = nvl(nomClient, "Client");
        String titre  = nvl(titreReclamation, "votre réclamation");
        String reponse = nvl(contenuReponse, "");
        String admin  = nvl(nomAdmin, "L'équipe AfkArt");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:24px;color:#2f2f2f;'>");
        sb.append("<div style='max-width:660px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #e5e7ef;'>");

        // Header
        sb.append("<div style='background:#1f3b73;color:#fff;padding:22px 24px;'>");
        sb.append("<h1 style='margin:0;font-size:20px;'>Réponse à votre réclamation</h1>");
        sb.append("<p style='margin:6px 0 0;font-size:13px;opacity:.9;'>").append(esc(titre)).append("</p>");
        sb.append("</div>");

        // Corps
        sb.append("<div style='padding:24px;'>");
        sb.append("<p style='margin:0 0 16px;'>Bonjour <strong>").append(esc(nom)).append("</strong>,</p>");
        sb.append("<p style='margin:0 0 20px;color:#555;line-height:1.6;'>");
        sb.append("Notre équipe a traité votre réclamation <strong>").append(esc(titre)).append("</strong>.");
        sb.append(" Voici la réponse de notre administrateur :</p>");

        // Réponse encadrée
        sb.append("<div style='background:#f0f4ff;border-left:4px solid #1f3b73;padding:16px 18px;border-radius:6px;margin:0 0 24px;'>");
        sb.append("<p style='margin:0 0 8px;font-size:12px;color:#888;font-weight:bold;text-transform:uppercase;letter-spacing:.5px;'>Réponse de ").append(esc(admin)).append("</p>");
        sb.append("<p style='margin:0;line-height:1.7;font-size:15px;color:#2f2f2f;'>").append(esc(reponse)).append("</p>");
        sb.append("</div>");

        sb.append("<p style='margin:0;color:#555;line-height:1.6;'>Si vous avez d'autres questions, n'hésitez pas à nous contacter.</p>");
        sb.append("<p style='margin-top:20px;color:#555;'>Cordialement,<br/><strong>").append(esc(admin)).append("</strong><br/>");
        sb.append("<span style='color:#888;font-size:13px;'>Équipe AfkArt</span></p>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private String buildMeetHtml(String clientNom, String titreReclamation, String meetUrl) {
        String nom   = nvl(clientNom, "Client");
        String titre = nvl(titreReclamation, "votre reclamation");
        String url   = nvl(meetUrl, "#");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4faf7;padding:24px;color:#333;'>");
        sb.append("<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #c8e6c9;'>");
        sb.append("<div style='background:#0f9d58;color:#fff;padding:22px;text-align:center;'>");
        sb.append("<h1 style='margin:0;font-size:22px;'>Invitation visioconference</h1>");
        sb.append("</div>");
        sb.append("<div style='padding:24px;'>");
        sb.append("<p>Bonjour <strong>").append(esc(nom)).append("</strong>,</p>");
        sb.append("<p>Un rendez-vous a ete planifie pour traiter <em>").append(esc(titre)).append("</em>.</p>");
        sb.append("<p style='text-align:center;margin:24px 0;'>");
        sb.append("<a href='").append(esc(url)).append("' style='background:#0f9d58;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold;'>Rejoindre la visioconference</a>");
        sb.append("</p>");
        sb.append("<p style='font-size:12px;color:#888;word-break:break-all;'>Lien : ").append(esc(url)).append("</p>");
        sb.append("<p>Merci,<br/><strong>Equipe AfkArt</strong></p>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    // ── Preview console ───────────────────────────────────────────────────────

    private void printPreview(String to, String subject, String html) {
        System.out.println("\n========== EMAIL AFKART (PREVIEW) ==========");
        System.out.println("TO      : " + to);
        System.out.println("SUBJECT : " + subject);
        System.out.println("--------------------------------------------");
        System.out.println(html);
        System.out.println("============================================\n");
        System.out.println("[EmailService] Pour envoyer de vrais emails :");
        System.out.println("  1. Allez sur https://myaccount.google.com/security");
        System.out.println("  2. Activez la validation en 2 etapes");
        System.out.println("  3. Creez un mot de passe d'application (16 caracteres)");
        System.out.println("  4. Mettez-le dans email.properties -> email.smtp.password=xxxxxxxxxxxx");
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("[EmailService] email.properties charge");
            } else {
                System.err.println("[EmailService] email.properties introuvable dans les resources");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Erreur lecture email.properties: " + e.getMessage());
        }
        return props;
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
