package org.example.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeocodingService {

    public static double[] getCoordinates(String address) {
        try {
            // 1. Encodage propre
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=1";

            // 2. Client avec Timeout (pour éviter que l'app ne freeze si le net est lent)
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // 3. Requête avec un User-Agent plus "standard"
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept", "application/json")
                    .build();

            System.out.println("Envoi de la requête à : " + url);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- DEBUG : On affiche ce que le serveur dit vraiment ---
            System.out.println("Code réponse serveur : " + response.statusCode());
            System.out.println("Corps de la réponse : " + response.body());
            // ---------------------------------------------------------

            if (response.statusCode() == 200) {
                JSONArray jsonArray = new JSONArray(response.body());

                if (jsonArray.length() > 0) {
                    JSONObject obj = jsonArray.getJSONObject(0);
                    double lat = Double.parseDouble(obj.getString("lat")); // Parfois Nominatim renvoie des Strings
                    double lon = Double.parseDouble(obj.getString("lon"));
                    return new double[]{lat, lon};
                } else {
                    System.err.println("Aucun résultat trouvé pour cette adresse sur Nominatim.");
                }
            } else {
                System.err.println("Erreur Serveur : Code " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Exception lors du géocodage : ");
            e.printStackTrace(); // Pour voir l'erreur complète dans la console
        }
        return null;
    }
}