package org.example.services;

import org.example.models.Reclamation;
import org.example.models.ReponseReclamation;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resume intelligent local (heuristique) pour accelerer le traitement admin.
 */
public class ReclamationSummaryService {

    public enum Urgence {
        BASSE,
        MOYENNE,
        ELEVEE,
        CRITIQUE
    }

    public enum Categorie {
        LIVRAISON,
        PAIEMENT,
        QUALITE,
        COMPTE,
        SERVICE_CLIENT,
        AUTRE
    }

    public record SummaryInsights(
            String shortSummary,
            Urgence urgence,
            Categorie categorie,
            List<String> pointsCles,
            String prochaineAction,
            String delaiTraitement
    ) {}

    private static final Set<String> KEYWORDS = Set.of(
            "probleme", "erreur", "livraison", "remboursement", "retard", "defaut",
            "panne", "urgent", "annulation", "commande", "paiement", "casse",
            "manquant", "qualite", "sav"
    );

    private static final Map<Categorie, List<String>> CATEGORY_KEYWORDS = Map.of(
            Categorie.LIVRAISON, List.of("livraison", "retard", "colis", "transport", "manquant"),
            Categorie.PAIEMENT, List.of("paiement", "remboursement", "carte", "facture", "transaction"),
            Categorie.QUALITE, List.of("defaut", "casse", "qualite", "abime", "endommag"),
            Categorie.COMPTE, List.of("compte", "connexion", "mot de passe", "profil", "bloque"),
            Categorie.SERVICE_CLIENT, List.of("support", "sav", "reponse", "service", "assistance")
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\b");

    public String generateAISummary(String description) {
        String text = description == null ? "" : description.trim();
        if (text.isEmpty()) {
            return "Aucune description fournie.";
        }

        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return fallbackFirstSentences(text);
        }

        List<ScoredSentence> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = scoreSentence(sentence, i);
            scored.add(new ScoredSentence(i, sentence, score));
        }

        int keep = Math.min(3, Math.max(1, sentences.size() >= 4 ? 2 : 1));
        List<ScoredSentence> top = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredSentence::score).reversed())
                .limit(keep)
                .sorted(Comparator.comparingInt(ScoredSentence::index))
                .toList();

        StringBuilder summary = new StringBuilder();
        for (ScoredSentence s : top) {
            if (!summary.isEmpty()) {
                summary.append(" ");
            }
            summary.append(s.sentence());
        }

        String finalSummary = summary.toString().trim();
        if (finalSummary.isEmpty() || isTooCloseToOriginal(text, finalSummary)) {
            return fallbackFirstSentences(text);
        }
        return finalSummary;
    }

    public String buildAdminDigest(Reclamation reclamation, List<ReponseReclamation> reponses) {
        SummaryInsights insights = analyze(reclamation, reponses);
        String statut = reclamation == null ? "--" : safe(reclamation.getStatut());
        String date = reclamation == null ? "--" : safe(reclamation.getFormattedDate());
        String user = "Inconnu";
        if (reclamation != null && reclamation.getClient() != null) {
            user = safe(reclamation.getClient().getNomComplet()) + " (" + safe(reclamation.getClient().getEmail()) + ")";
        }

        StringBuilder out = new StringBuilder();
        out.append("Resume automatique\n");
        out.append(insights.shortSummary()).append("\n\n");

        out.append("Infos generales\n");
        out.append("- Statut: ").append(statut).append("\n");
        out.append("- Date: ").append(date).append("\n");
        out.append("- Utilisateur: ").append(user).append("\n");
        out.append("- Temps de traitement: ").append(insights.delaiTraitement()).append("\n");
        out.append("- Urgence: ").append(insights.urgence()).append("\n");
        out.append("- Categorie probable: ").append(insights.categorie()).append("\n\n");

        out.append("Points cles detectes\n");
        if (insights.pointsCles().isEmpty()) {
            out.append("- Aucun point cle specifique detecte.\n");
        } else {
            for (String point : insights.pointsCles()) {
                out.append("- ").append(point).append("\n");
            }
        }
        out.append("\n");

        out.append("Prochaine action recommandee\n");
        out.append(insights.prochaineAction()).append("\n\n");

        out.append("Description complete\n");
        out.append(reclamation == null ? "" : safe(reclamation.getDescription())).append("\n\n");

        out.append("Historique des reponses admin\n");
        if (reponses == null || reponses.isEmpty()) {
            out.append("Aucune reponse pour le moment.\n");
        } else {
            for (ReponseReclamation r : reponses) {
                String admin = r.getAdmin() == null ? "Admin" : safe(r.getAdmin().getNomComplet());
                out.append("- ").append(admin)
                        .append(" (" + safe(r.getFormattedDate()) + ")")
                        .append(" : ")
                        .append(safe(r.getContenu()))
                        .append("\n");
            }
        }
        return out.toString();
    }

    public SummaryInsights analyze(Reclamation reclamation, List<ReponseReclamation> reponses) {
        String description = reclamation == null ? "" : safe(reclamation.getDescription());
        String shortSummary = generateAISummary(description);
        Categorie categorie = detectCategory(description);
        Urgence urgence = detectUrgence(description, reclamation, reponses);
        List<String> points = extractKeyPoints(description, reclamation, reponses);
        String delai = computeProcessingDelay(reclamation, reponses);
        String action = buildRecommendedAction(categorie, urgence, reponses == null ? 0 : reponses.size());

        return new SummaryInsights(shortSummary, urgence, categorie, points, action, delai);
    }

    public SummaryInsights analyzeReclamationOnly(Reclamation reclamation) {
        String description = reclamation == null ? "" : safe(reclamation.getDescription());
        String shortSummary = generateAISummary(description);
        Categorie categorie = detectCategory(description);
        Urgence urgence = detectUrgence(description, reclamation, null);
        List<String> points = extractKeyPoints(description, reclamation, null);
        String delai = "N/A";
        String action = buildRecommendedAction(categorie, urgence, 0);
        return new SummaryInsights(shortSummary, urgence, categorie, points, action, delai);
    }

    public String buildReclamationOnlyDigest(Reclamation reclamation) {
        SummaryInsights insights = analyzeReclamationOnly(reclamation);
        String statut = reclamation == null ? "--" : safe(reclamation.getStatut());
        String date = reclamation == null ? "--" : safe(reclamation.getFormattedDate());
        String user = "Inconnu";
        if (reclamation != null && reclamation.getClient() != null) {
            user = safe(reclamation.getClient().getNomComplet()) + " (" + safe(reclamation.getClient().getEmail()) + ")";
        }

        StringBuilder out = new StringBuilder();
        out.append("Resume automatique (base sur la description de reclamation uniquement)\n");
        out.append(insights.shortSummary()).append("\n\n");

        out.append("Infos generales\n");
        out.append("- Statut: ").append(statut).append("\n");
        out.append("- Date: ").append(date).append("\n");
        out.append("- Utilisateur: ").append(user).append("\n");
        out.append("- Urgence: ").append(insights.urgence()).append("\n");
        out.append("- Categorie probable: ").append(insights.categorie()).append("\n\n");

        out.append("Points cles detectes\n");
        if (insights.pointsCles().isEmpty()) {
            out.append("- Aucun point cle specifique detecte.\n");
        } else {
            for (String point : insights.pointsCles()) {
                out.append("- ").append(point).append("\n");
            }
        }
        out.append("\n");

        out.append("Prochaine action recommandee\n");
        out.append(insights.prochaineAction()).append("\n\n");

        out.append("Description complete\n");
        out.append(reclamation == null ? "" : safe(reclamation.getDescription())).append("\n");
        return out.toString();
    }

    private String computeProcessingDelay(Reclamation reclamation, List<ReponseReclamation> reponses) {
        if (reclamation == null || reclamation.getDateCreation() == null || reponses == null || reponses.isEmpty()) {
            return "En attente de reponse";
        }

        Timestamp firstResponse = reponses.stream()
                .map(ReponseReclamation::getDateReponse)
                .filter(d -> d != null)
                .sorted()
                .findFirst()
                .orElse(null);

        if (firstResponse == null) {
            return "En attente de reponse";
        }

        Duration duration = Duration.between(reclamation.getDateCreation().toInstant(), firstResponse.toInstant());
        long totalHours = Math.max(0, duration.toHours());
        long days = totalHours / 24;
        long hours = totalHours % 24;

        if (days > 0) {
            return days + "j " + hours + "h";
        }
        return hours + "h";
    }

    private Categorie detectCategory(String description) {
        String low = normalizeText(description);
        Categorie best = Categorie.AUTRE;
        int max = 0;

        for (Map.Entry<Categorie, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (low.contains(keyword)) {
                    score++;
                }
            }
            if (score > max) {
                max = score;
                best = entry.getKey();
            }
        }

        return max == 0 ? Categorie.AUTRE : best;
    }

    private Urgence detectUrgence(String description, Reclamation reclamation, List<ReponseReclamation> reponses) {
        String low = normalizeText(description);
        int score = 0;

        if (low.contains("urgent") || low.contains("bloque") || low.contains("impossible")) {
            score += 3;
        }
        if (low.contains("remboursement") || low.contains("paiement")) {
            score += 2;
        }
        if (low.contains("retard") || low.contains("defaut") || low.contains("casse")) {
            score += 1;
        }
        if (description != null && description.contains("!")) {
            score += 1;
        }

        if (reclamation != null && "en_attente".equalsIgnoreCase(reclamation.getStatut())) {
            score += 1;
        }

        if (reclamation != null && reclamation.getDateCreation() != null && (reponses == null || reponses.isEmpty())) {
            Duration waiting = Duration.between(reclamation.getDateCreation().toInstant(), Timestamp.from(java.time.Instant.now()).toInstant());
            if (waiting.toHours() >= 72) {
                score += 3;
            } else if (waiting.toHours() >= 24) {
                score += 2;
            }
        }

        if (score >= 7) return Urgence.CRITIQUE;
        if (score >= 5) return Urgence.ELEVEE;
        if (score >= 3) return Urgence.MOYENNE;
        return Urgence.BASSE;
    }

    private List<String> extractKeyPoints(String description, Reclamation reclamation, List<ReponseReclamation> reponses) {
        List<String> points = new ArrayList<>();
        if (description == null || description.isBlank()) {
            points.add("Description absente");
        } else {
            Matcher matcher = NUMBER_PATTERN.matcher(description);
            List<String> numbers = new ArrayList<>();
            while (matcher.find()) {
                numbers.add(matcher.group());
                if (numbers.size() >= 3) break;
            }
            if (!numbers.isEmpty()) {
                points.add("Valeurs numeriques mentionnees: " + String.join(", ", numbers));
            }

            String low = normalizeText(description);
            for (String kw : KEYWORDS) {
                if (low.contains(kw)) {
                    points.add("Mot-clé detecté: " + kw);
                    if (points.size() >= 4) break;
                }
            }
        }

        if (reclamation != null) {
            points.add("Statut actuel: " + safe(reclamation.getStatut()));
        }
        points.add((reponses == null || reponses.isEmpty())
                ? "Aucune reponse admin pour le moment"
                : ("Nombre de reponses admin: " + reponses.size()));

        return points.stream().distinct().limit(6).toList();
    }

    private String buildRecommendedAction(Categorie categorie, Urgence urgence, int nbReponses) {
        String base = switch (categorie) {
            case LIVRAISON -> "Verifier le suivi transport et proposer une date de resolution claire.";
            case PAIEMENT -> "Verifier la transaction, puis confirmer remboursement ou correction facture.";
            case QUALITE -> "Demander photo/preuve et proposer remplacement, reparation ou remboursement.";
            case COMPTE -> "Verifier l'etat du compte et fournir une procedure de recuperation simple.";
            case SERVICE_CLIENT -> "Apporter une reponse precise et planifier un suivi sous 24h.";
            case AUTRE -> "Qualifier le besoin exact puis repondre avec un plan d'action concret.";
        };

        if (urgence == Urgence.CRITIQUE || urgence == Urgence.ELEVEE) {
            return base + " Priorite haute: traiter immediatement et notifier le client en premier.";
        }
        if (nbReponses == 0) {
            return base + " Commencer par une premiere reponse empathique avec delai annonce.";
        }
        return base;
    }

    private List<String> splitSentences(String text) {
        String[] raw = text.split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s == null ? "" : s.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        return sentences;
    }

    private double scoreSentence(String sentence, int index) {
        String low = sentence.toLowerCase(Locale.ROOT);
        double score = 0.0;

        if (index == 0) score += 2.0;
        else if (index == 1) score += 1.0;

        for (String kw : KEYWORDS) {
            if (low.contains(kw)) {
                score += 2.0;
            }
        }

        if (low.matches(".*\\d+.*")) {
            score += 1.2;
        }

        int len = sentence.length();
        if (len >= 40 && len <= 220) {
            score += 0.8;
        }

        return score;
    }

    private boolean isTooCloseToOriginal(String original, String summary) {
        String o = normalizeText(original);
        String s = normalizeText(summary);
        if (o.equals(s)) {
            return true;
        }
        if (s.length() > (int) (o.length() * 0.85)) {
            return true;
        }

        Set<String> oWords = new HashSet<>(List.of(o.split("\\s+")));
        Set<String> sWords = new HashSet<>(List.of(s.split("\\s+")));
        oWords.remove("");
        sWords.remove("");
        if (oWords.isEmpty() || sWords.isEmpty()) {
            return false;
        }
        long overlap = sWords.stream().filter(oWords::contains).count();
        double ratio = overlap / (double) sWords.size();
        return ratio > 0.85;
    }

    private String fallbackFirstSentences(String text) {
        List<String> sentences = splitSentences(text);
        if (sentences.size() == 1) {
            return sentences.get(0);
        }
        return sentences.get(0) + " " + sentences.get(1);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredSentence(int index, String sentence, double score) {}
}

