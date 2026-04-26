package org.example.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service de génération de messages personnalisés via l'API Hugging Face.
 * Utilise le modèle Mistral-7B pour créer des messages uniques et contextualisés.
 * Fallback automatique si l'API est indisponible.
 */
public class PersonalizedMessageService {

    public record MessageGenerationResult(String message, boolean aiGenerated, String source) {}

    private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";
    private static final String HF_API_KEY = System.getenv("HUGGING_FACE_API_KEY");
    private static final int TIMEOUT_SECONDS = 10;

    private static final List<String> FALLBACK_TEMPLATES = List.of(
            "Bonjour {nom}, nous avons bien reçu votre commande n°{numero}. Nous préparons avec soin vos {nb} article(s) pour un total de {total} DT. Merci pour votre confiance !",
            "Cher(e) {nom}, votre commande n°{numero} est confirmée. Nos artisans prennent en charge vos {nb} article(s) pour un montant de {total} DT. Merci de votre achat et à très bientôt !",
            "Merci {nom} ! Votre commande n°{numero} ({total} DT) a été enregistrée avec succès. Notre équipe prépare dès maintenant vos {nb} article(s) avec attention."
    );

    private final HttpClient httpClient;
    private int fallbackIndex = 0;

    public PersonalizedMessageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Génère un message personnalisé pour une commande.
     * 
     * @param clientNom nom du client
     * @param articles liste des articles commandés
     * @param total montant total de la commande
     * @param adresse adresse de livraison
     * @return message personnalisé généré ou fallback
     */
    public MessageGenerationResult generateOrderConfirmationMessage(String customerName,
                                                                    List<String> articles,
                                                                    double total,
                                                                    String orderNumber) {
        String safeName = sanitizeName(customerName);
        List<String> safeArticles = articles == null ? List.of() : articles.stream()
                .filter(a -> a != null && !a.isBlank())
                .toList();
        try {
            if (HF_API_KEY == null || HF_API_KEY.isBlank()) {
                System.err.println("WARN: HUGGING_FACE_API_KEY non configurée, utilisation du fallback");
                return fallbackResult(safeName, safeArticles, total, orderNumber, "fallback:no-api-key");
            }

            String prompt = construirePrompt(safeName, safeArticles, total, orderNumber);
            String messageIA = appelHuggingFace(prompt);

            if (messageIA != null && !messageIA.isBlank()) {
                String cleaned = nettoyerReponseIA(messageIA, prompt);
                if (cleaned != null && !cleaned.isBlank()) {
                    return new MessageGenerationResult(cleaned, true, "huggingface");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel Hugging Face: " + e.getMessage());
        }

        return fallbackResult(safeName, safeArticles, total, orderNumber, "fallback:error");
    }

    public String genererMessagePersonnalise(String clientNom, List<String> articles, double total, String orderNumber) {
        return generateOrderConfirmationMessage(clientNom, articles, total, orderNumber).message();
    }

    private String construirePrompt(String customerName, List<String> articles, double total, String orderNumber) {
        String articlesText = articles.isEmpty()
                ? "un article artisanal"
                : String.join(", ", articles);

        return String.format(Locale.ROOT,
                "Écris un message de confirmation de commande personnalisé et chaleureux en français pour %s. " +
                        "La commande numéro %s contient: %s. Le montant total est de %.2f DT. " +
                        "Le message doit être court (3-4 phrases), professionnel mais amical, remercier le client, " +
                        "mettre en avant l'artisanat et l'esprit éco-responsable, sans signature.",
                customerName,
                orderNumber == null || orderNumber.isBlank() ? "commande AfkArt" : orderNumber,
                articlesText,
                total);
    }

    private String appelHuggingFace(String prompt) throws IOException, InterruptedException {
        String payload = "{\"inputs\": \"" + escapeJson(prompt) + "\", \"parameters\": {\"max_new_tokens\": 150, \"temperature\": 0.7, \"top_p\": 0.9}}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(HUGGING_FACE_API_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Authorization", "Bearer " + HF_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return extraireTexteReponse(response.body());
    }

    private String extraireTexteReponse(String jsonResponse) {
        try {
            Pattern p = Pattern.compile("\"generated_text\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
            Matcher m = p.matcher(jsonResponse);
            if (m.find()) {
                return unescapeJson(m.group(1));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de la réponse Hugging Face: " + e.getMessage());
        }
        return null;
    }

    private String nettoyerReponseIA(String texte, String prompt) {
        String cleaned = texte.trim();

        if (prompt != null && !prompt.isBlank() && cleaned.startsWith(prompt)) {
            cleaned = cleaned.substring(prompt.length()).trim();
        }
        
        int messageIdx = cleaned.toUpperCase().indexOf("MESSAGE:");
        if (messageIdx >= 0) {
            cleaned = cleaned.substring(messageIdx + 8).trim();
        }
        
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        cleaned = cleaned.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 500) + "...";
        }
        
        return cleaned;
    }

    private MessageGenerationResult fallbackResult(String clientNom, List<String> articles, double total, String orderNumber, String source) {
        return new MessageGenerationResult(
                genererFallback(clientNom, articles, total, orderNumber),
                false,
                source
        );
    }

    private String genererFallback(String clientNom, List<String> articles, double total, String orderNumber) {
        String template = FALLBACK_TEMPLATES.get(fallbackIndex % FALLBACK_TEMPLATES.size());
        fallbackIndex++;
        
        return template
                .replace("{nom}", clientNom)
                .replace("{nb}", String.valueOf(articles == null ? 0 : articles.size()))
                .replace("{total}", String.format("%.2f", total))
                .replace("{numero}", orderNumber == null || orderNumber.isBlank() ? "--" : orderNumber);
    }

    private String sanitizeName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return "Client";
        }
        return customerName.trim();
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char n = input.charAt(++i);
                switch (n) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

