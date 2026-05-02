package org.example.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Génère des avatars portraits photoréalistes via Pollinations.ai.
 * Le portrait change selon le sexe, le nom et l'email.
 */
public class AvatarService {

    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";

    public String generateAvatarUrl(String sexe, String nom, String prenom, String email) {
        // Seed unique et stable basé sur email + nom
        int seed = buildSeed(email, nom, prenom);

        String prompt = buildPrompt(sexe, prenom, nom, seed);
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);

        return BASE_URL + encoded
                + "?width=400&height=400&nologo=true&seed=" + seed;
    }

    private String buildPrompt(String sexe, String prenom, String nom, int seed) {
        boolean homme = sexe != null && sexe.trim().toLowerCase().contains("homme");
        boolean femme = sexe != null && sexe.trim().toLowerCase().contains("femme");

        // Variation de style selon le seed
        int style = Math.abs(seed) % 4;

        if (homme) {
            return switch (style) {
                case 0 -> "professional portrait photo of a young north african man, " +
                          "short dark hair, brown eyes, light beard, " +
                          "wearing a brown turtleneck sweater, warm studio background, " +
                          "photorealistic, 8k, sharp focus, professional headshot";
                case 1 -> "portrait photo of a handsome mediterranean man, " +
                          "buzz cut dark hair, full beard, wearing a dark blazer, " +
                          "warm orange background, cinematic lighting, photorealistic, 8k";
                case 2 -> "professional headshot of a young arab man, " +
                          "styled dark brown hair, light stubble, wearing a rust orange jacket, " +
                          "warm caramel background, studio lighting, photorealistic, 8k";
                default -> "portrait of a young tunisian man, dark hair, " +
                           "wearing casual smart clothes, warm neutral background, " +
                           "professional photo, photorealistic, 8k";
            };
        } else if (femme) {
            return switch (style) {
                case 0 -> "professional portrait photo of a young north african woman, " +
                          "long dark hair, warm brown eyes, natural makeup, " +
                          "wearing elegant beige blouse, warm studio background, " +
                          "photorealistic, 8k, sharp focus";
                case 1 -> "portrait photo of a beautiful mediterranean woman, " +
                          "dark wavy hair, subtle makeup, wearing a terracotta dress, " +
                          "warm golden background, cinematic lighting, photorealistic, 8k";
                case 2 -> "professional headshot of a young tunisian woman, " +
                          "dark hair, natural look, wearing a rust orange top, " +
                          "warm studio background, sharp focus, photorealistic, 8k";
                default -> "portrait of a young arab woman, dark hair, " +
                           "elegant style, warm neutral background, " +
                           "professional photo, photorealistic, 8k";
            };
        } else {
            // Non spécifié — avatar neutre artistique
            return "professional portrait photo, warm neutral background, " +
                   "natural lighting, elegant style, photorealistic, 8k, " +
                   "seed " + seed;
        }
    }

    /**
     * Seed stable basé sur email + nom + prénom.
     * Le même utilisateur aura toujours le même avatar.
     */
    private int buildSeed(String email, String nom, String prenom) {
        String key = nvl(email) + nvl(nom) + nvl(prenom);
        if (key.isBlank()) return 42000;
        int hash = 0;
        for (char c : key.toCharArray()) {
            hash = hash * 31 + c;
        }
        return Math.abs(hash % 99999) + 1000; // entre 1000 et 100999
    }

    private String nvl(String v) {
        return v == null ? "" : v.trim();
    }
}
