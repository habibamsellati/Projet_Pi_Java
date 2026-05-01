package org.example.services;

/**
 * Service SMS — simulation console (pas d'API SMS réelle configurée).
 * Pour activer un vrai SMS, intégrer Twilio ou InfoBip ici.
 */
public class SmsService {

    private static final String PREFIX_TN = "+216";

    /**
     * Envoie un SMS au numéro donné avec le message donné.
     * Actuellement : affiche dans la console (mode preview).
     */
    public boolean envoyerSms(String telephone, String message) {
        if (telephone == null || telephone.isBlank()) {
            System.err.println("[SMS] Numéro de téléphone manquant — SMS non envoyé.");
            return false;
        }
        String numero = normaliserNumero(telephone);
        System.out.println("\n========== SMS AFKART ==========");
        System.out.println("TO  : " + numero);
        System.out.println("MSG : " + message);
        System.out.println("================================\n");
        return true;
    }

    // ── Messages prédéfinis ───────────────────────────────────────────────────

    public boolean smsPropositionAcceptee(String telephone, String nomProduit, double prix) {
        String msg = "AfkArt : Votre proposition pour " + nomProduit +
                     " a été ACCEPTÉE. Prix convenu : " + String.format("%.2f TND", prix) +
                     ". L'artisan vous contactera prochainement.";
        return envoyerSms(telephone, msg);
    }

    public boolean smsPropositionRefusee(String telephone, String nomProduit) {
        String msg = "AfkArt : Votre proposition pour " + nomProduit +
                     " a été REFUSÉE. Vous pouvez soumettre une nouvelle proposition sur afkart.tn";
        return envoyerSms(telephone, msg);
    }

    public boolean smsPropositionTerminee(String telephone, String nomProduit) {
        String msg = "AfkArt : Bonne nouvelle ! Votre œuvre réalisée à partir de " + nomProduit +
                     " est TERMINÉE. Contactez l'artisan pour la récupération.";
        return envoyerSms(telephone, msg);
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    private String normaliserNumero(String tel) {
        String n = tel.trim().replaceAll("[\\s\\-().]", "");
        if (n.startsWith("+216")) return n;
        if (n.startsWith("00216")) return "+" + n.substring(2);
        if (n.length() == 8) return PREFIX_TN + n;
        return n;
    }
}
