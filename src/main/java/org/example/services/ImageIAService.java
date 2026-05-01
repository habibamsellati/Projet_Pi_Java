package org.example.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Génération d'image IA via Pollinations.ai (gratuit, sans clé API).
 * URL : https://image.pollinations.ai/prompt/{description}?width=400&height=300&nologo=true
 */
public class ImageIAService {

    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";
    private static final int WIDTH  = 400;
    private static final int HEIGHT = 300;
    private static final int TIMEOUT_SECONDS = 20;

    private final HttpClient httpClient;

    public ImageIAService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Génère une URL d'image IA à partir d'une description.
     * L'URL est directement utilisable dans un ImageView JavaFX.
     *
     * @param description  texte décrivant le produit ou la proposition
     * @param seed         graine pour reproductibilité (utiliser l'ID ou un random)
     * @return URL de l'image générée
     */
    public String genererUrlImage(String description, int seed) {
        if (description == null || description.isBlank()) {
            return null;
        }

        // Construire un prompt optimisé
        String prompt = construirePrompt(description);
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);

        return BASE_URL + encoded
                + "?width=" + WIDTH
                + "&height=" + HEIGHT
                + "&nologo=true"
                + "&seed=" + seed;
    }

    /**
     * Génère l'URL et vérifie que l'image est accessible (HEAD request).
     * Retourne null si l'image n'est pas disponible.
     */
    public String genererUrlImageVerifiee(String description, int seed) {
        String url = genererUrlImage(description, seed);
        if (url == null) return null;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                System.out.println("[ImageIA] Image générée : " + url);
                return url;
            }
        } catch (Exception e) {
            System.err.println("[ImageIA] Erreur génération image : " + e.getMessage());
        }
        return null;
    }

    /**
     * Construit un prompt optimisé pour Pollinations à partir de la description.
     */
    private String construirePrompt(String description) {
        String low = description.toLowerCase();

        // Détecter le type de contenu pour adapter le style
        String style = "realistic photo clean ";

        // Matériaux recyclables
        if (low.contains("plastique") || low.contains("bouteille"))
            style += "plastic bottles recycling ";
        else if (low.contains("carton") || low.contains("boite"))
            style += "cardboard boxes recycling ";
        else if (low.contains("verre"))
            style += "glass bottles recycling ";
        else if (low.contains("bois") || low.contains("chaise") || low.contains("meuble"))
            style += "wood planks timber recycling ";
        else if (low.contains("metal") || low.contains("fer") || low.contains("aluminium"))
            style += "metal scrap recycling ";
        else if (low.contains("tissu") || low.contains("textile") || low.contains("vetement"))
            style += "fabric textile recycling ";
        else if (low.contains("papier") || low.contains("journal"))
            style += "paper newspaper recycling ";

        // État
        if (low.contains("bon") || low.contains("bonne") || low.contains("excellent"))
            style += "good condition ";
        else if (low.contains("use") || low.contains("casse") || low.contains("vieux"))
            style += "used worn ";

        // Ajouter la description originale
        style += description.trim();

        return style;
    }
}
