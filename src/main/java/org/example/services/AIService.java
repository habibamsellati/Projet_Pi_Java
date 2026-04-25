package org.example.services;

import java.util.Random;

public class AIService {

    public String generateDescription(String title, String type, String theme) {
        String[] templates = {
                "Plongez dans un univers où la matière rencontre l'esprit. Cet événement '%s' dédié à la discipline %s vous invite à explorer des techniques ancestrales revisitées par une vision moderne. Une expérience immersive unique centrée sur %s pour éveiller vos sens.",
                "Découvrez l'essence même du geste artistique à travers cet événement : %s. Entre tradition et innovation, nous vous proposons un voyage au cœur de la création pure (%s), où chaque détail raconte une histoire de passion et d'excellence sous le thème %s.",
                "Une rencontre exceptionnelle avec l'art : %s. Venez partager un moment privilégié au plus près de la création de type %s, pour comprendre les secrets d'un savoir-faire d'exception et repartir avec une source d'inspiration intarissable sur le thème %s."
        };

        Random r = new Random();
        String template = templates[r.nextInt(templates.length)];
        String t = (type != null && !type.isEmpty()) ? type : "artisanale";
        String th = (theme != null && !theme.isEmpty()) ? theme : "l'art et la culture";

        return String.format(template, title, t, th);
    }

    public String getChatResponse(String userMessage, boolean isArtisan) {
        String msg = userMessage.toLowerCase();

        try {
            if (msg.contains("combien") && (msg.contains("événement") || msg.contains("evenement"))) {
                org.example.services.ServiceEvenement se = new org.example.services.ServiceEvenement();
                int count = se.compterTotal();
                return "Nous avons actuellement " + count
                        + " événements disponibles sur la plateforme AfkArt ! Y a-t-il un type d'art particulier que vous recherchez ?";
            }
            if (msg.contains("combien")
                    && (msg.contains("réservation") || msg.contains("reservation") || msg.contains("place"))) {
                org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
                // Si l'utilisateur est connecté et N'EST PAS artisan, on lui dit combien lui a
                // de réservations
                if (!isArtisan && org.example.utils.SessionStore.isConnected()) {
                    int myResCount = sr.listerParUtilisateur(org.example.utils.SessionStore.getUserId()).size();
                    return "Vous avez actuellement " + myResCount
                            + " réservations dans votre historique. Souhaitez-vous les consulter ?";
                }
                return "Au global, nous avons comptabilisé " + sr.compterTotal() + " réservations sur la plateforme.";
            }
            if (msg.contains("dernier") || msg.contains("nouveau")) {
                org.example.services.ServiceEvenement se = new org.example.services.ServiceEvenement();
                java.util.List<org.example.models.Evenement> events = se.listerTousParDateDebutDesc();
                if (!events.isEmpty()) {
                    return "Notre événement le plus récent est '" + events.get(0).getNom()
                            + "'. Allez jeter un coup d'œil dans le catalogue !";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (msg.contains("bonjour") || msg.contains("salut")) {
            return "Bienvenue chez AfkArt ! En tant que votre Concierge Culturel, je suis ravi de vous assister. Souhaitez-vous découvrir nos ateliers ou avez-vous besoin d'aide pour vos réservations ?";
        }

        // --- DISCIPLINES ---
        if (msg.contains("céramique") || msg.contains("ceramique")) {
            return "La céramique est une excellente discipline pour se concentrer. Je vous invite à utiliser la barre de recherche avec le mot 'céramique' pour trouver nos ateliers spécialisés !";
        }
        if (msg.contains("peinture")) {
            return "La peinture vous passionne ? Nous avons de nombreux événements autour de l'art pictural. Filtrez le catalogue par 'peinture' pour les découvrir.";
        }
        if (msg.contains("sculpture")) {
            return "Travailler la matière brute est fascinant. Filtrez les événements par 'sculpture' pour participer aux prochaines sessions.";
        }

        // --- CONVERSATION BASIQUE ---
        if (msg.equals("non") || msg.equals("nan") || msg.equals("pas vraiment")) {
            return "D'accord, aucun problème ! Prenez votre temps pour explorer notre vitrine. Filtrez, triez et n'hésitez pas à me redemander à tout moment.";
        }
        if (msg.equals("oui") || msg.equals("ouais") || msg.equals("absolument")) {
            return "Super ! Dites-m'en un peu plus. Plutôt des travaux manuels, du design graphique, ou de la peinture ?";
        }
        if (msg.contains("merci")) {
            return "C'est un plaisir de vous aider ! L'équipe AfkArt reste à votre entière disposition.";
        }

        if (isArtisan) {
            return "En tant qu'Artisan, n'oubliez pas de garder un œil sur votre tableau de bord Premium pour suivre en direct vos inscriptions !";
        }

        // --- WIKIPEDIA INTELLIGENCE (Recherche de connaissances générales) ---
        if (msg.length() > 3) {
            // Nettoyer la question (ex: "qu'est ce que picasso", "qui est mozart")
            String search = msg.replace("qu'est ce que", "").replace("qui est", "").replace("c'est quoi", "")
                    .replace("?", "").trim();
            if (!search.isEmpty()) {
                String wikiAnswer = queryWikipedia(search);
                if (wikiAnswer != null) {
                    return "Voici ce que j'ai trouvé : " + wikiAnswer;
                }
            }
        }

        // --- FALLBACK (Si aucune autre règle ne matche) ---
        return "Je suis spécialisé dans notre plateforme AfkArt et la culture " +
                (isArtisan ? "artisanale." : "artistique.") +
                " Pour plus d'informations, précisez votre demande !";
    }

    private String queryWikipedia(String query) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            java.net.URL url = new java.net.URL(
                    "https://fr.wikipedia.org/w/api.php?action=query&prop=extracts&exchars=300&exintro&explaintext&format=json&titles="
                            + encodedQuery);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                content.append(inputLine);
            in.close();

            String json = content.toString();
            int extractIndex = json.indexOf("\"extract\":\"");
            if (extractIndex != -1) {
                int start = extractIndex + 11;
                // Trouver la bonne fin de la valeur JSON
                int end = json.indexOf("\"", start);

                // Gérer le cas où il y a des " échappés dans l'extrait
                while (end != -1 && json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }

                if (end > start) {
                    String extract = json.substring(start, end)
                            .replace("\\n", " ")
                            .replace("\\\"", "\"")
                            .replace("\\u00e9", "é")
                            .replace("\\u00e8", "è")
                            .replace("\\u00ea", "ê")
                            .replace("\\u00e0", "à")
                            .replace("\\u00e7", "ç")
                            .replace("\\u00e2", "â")
                            .replace("\\u00ee", "î")
                            .replace("\\u00f4", "ô")
                            .replace("\\u00fb", "û")
                            .replace("\\u00e1", "á");
                    if (!extract.trim().isEmpty() && extract.length() > 15) {
                        return extract + " (Source: Wikipédia)";
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public double suggestPricing(String type) {
        if (type == null)
            return 25.0;
        switch (type.toLowerCase()) {
            case "peinture":
                return 45.0;
            case "sculpture":
                return 60.0;
            case "céramique":
                return 35.0;
            case "bijoux":
                return 80.0;
            default:
                return 30.0;
        }
    }
}
