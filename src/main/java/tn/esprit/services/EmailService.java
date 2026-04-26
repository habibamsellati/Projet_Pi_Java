package tn.esprit.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "noreply.afkart@gmail.com";
    private static final String APP_PASSWORD = "iosa vuxn jdcm lveo";

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    public void sendConfirmationEmail(String toEmail, String code) throws Exception {
        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "AfkArt"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Code de confirmation");

        String html =
                "<div style='margin:0; padding:0; background:#f4f2ef; width:100%;'>" +
                        "<div style='max-width:900px; margin:0 auto; padding:40px 20px; background:#f7f3ee;'>" +
                        "<div style='max-width:600px; margin:0 auto; background:#ffffff; border-radius:24px; padding:42px 36px; box-shadow:0 10px 30px rgba(0,0,0,0.06);'>" +

                        "<h1 style=\"margin:0 0 12px 0; font-family:Georgia, 'Times New Roman', serif; font-size:34px; font-weight:700; color:#2f261f;\">" +
                        "AfkArt · Reinitialisation</h1>" +

                        "<p style='margin:0 0 24px 0; font-family:Arial, sans-serif; font-size:15px; color:#4d4036; line-height:1.6;'>" +
                        "Vous avez demandé la réinitialisation de votre mot de passe." +
                        "</p>" +

                        "<div style='background:#f5f1ec; border-radius:18px; padding:30px 20px; text-align:center; margin-bottom:26px;'>" +
                        "<span style='display:inline-block; background:#c97848; color:white; text-decoration:none; font-family:Arial,sans-serif; font-size:16px; font-weight:700; padding:14px 26px; border-radius:28px;'>" +
                        "Retournez dans l'application AfkArt pour continuer" +
                        "</span>" +
                        "</div>" +

                        "<p style='margin:0; font-family:Arial, sans-serif; font-size:15px; color:#4d4036;'>" +
                        "Votre demande expire dans 10 minutes." +
                        "</p>" +

                        "<p style='margin-top:18px; font-family:Arial, sans-serif; font-size:13px; color:#7a6a5f;'>" +
                        "Ouvrez l'application puis cliquez sur <b>J'ai reçu l'email</b> pour choisir un nouveau mot de passe." +
                        "</p>" +

                        "</div></div></div>";

        message.setContent(html, "text/html; charset=UTF-8");
        Transport.send(message);
    }

    public void sendForgotPasswordEmail(String toEmail, String token) throws Exception {
        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "AfkArt"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Réinitialisation du mot de passe");

        String resetLink = "http://localhost:8080/reset-password?token=" + token;

        String html =
                "<div style='margin:0; padding:0; background:#f4f2ef; width:100%;'>" +
                        "<div style='max-width:900px; margin:0 auto; padding:40px 20px; background:#f7f3ee;'>" +
                        "<div style='max-width:600px; margin:0 auto; background:#ffffff; border-radius:24px; padding:42px 36px; box-shadow:0 10px 30px rgba(0,0,0,0.06);'>" +
                        "<h1 style=\"margin:0 0 12px 0; font-family:Georgia, 'Times New Roman', serif; font-size:34px; font-weight:700; color:#2f261f;\">AfkArt · Reinitialisation</h1>" +
                        "<p style='margin:0 0 24px 0; font-family:Arial, sans-serif; font-size:15px; color:#4d4036; line-height:1.6;'>" +
                        "Vous avez demandé la réinitialisation de votre mot de passe.</p>" +
                        "<div style='background:#f5f1ec; border-radius:18px; padding:30px 20px; text-align:center; margin-bottom:26px;'>" +
                        "<a href='" + resetLink + "' style='display:inline-block; background:#c97848; color:white; text-decoration:none; font-family:Arial,sans-serif; font-size:16px; font-weight:700; padding:14px 26px; border-radius:28px;'>" +
                        "Reinitialiser mon mot de passe</a>" +
                        "</div>" +
                        "<p style='margin:0; font-family:Arial, sans-serif; font-size:15px; color:#4d4036;'>Ce lien expire dans 10 minutes.</p>" +
                        "<p style='margin-top:18px; font-family:Arial, sans-serif; font-size:13px; color:#7a6a5f; word-break:break-all;'>Lien : " + resetLink + "</p>" +
                        "</div></div></div>";

        message.setContent(html, "text/html; charset=UTF-8");
        Transport.send(message);
    }
}