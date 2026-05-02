package org.example.services;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vrai flux OAuth 2.0 Google.
 *
 * Étapes :
 * 1. Ouvre accounts.google.com dans le navigateur par défaut
 * 2. L'utilisateur sélectionne son compte Google
 * 3. Google redirige vers http://localhost:8765/callback?code=...
 * 4. On échange le code contre un access_token
 * 5. On récupère l'email depuis l'API Google
 *
 * Configuration dans src/main/resources/google-oauth.properties :
 *   google.client.id=VOTRE_CLIENT_ID
 *   google.client.secret=VOTRE_CLIENT_SECRET
 */
public class GoogleOAuthService {

    private static final String REDIRECT_URI  = "http://localhost:8765/callback";
    private static final int    CALLBACK_PORT = 8765;
    private static final int    TIMEOUT_SEC   = 120;

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthService() {
        Properties props = loadProperties();
        this.clientId     = props.getProperty("google.client.id",     "");
        this.clientSecret = props.getProperty("google.client.secret", "");
    }

    /**
     * Retourne null si configuré, sinon un message d'erreur.
     */
    public String getConfigurationError() {
        if (clientId.isBlank() || clientId.contains("VOTRE"))
            return "google.client.id non configuré";
        if (clientSecret.isBlank() || clientSecret.contains("VOTRE"))
            return "google.client.secret non configuré";
        return null;
    }

    /**
     * Lance le flux OAuth complet et retourne l'email Google de l'utilisateur.
     * Bloque jusqu'à réception du callback (max 2 minutes).
     */
    public String obtenirEmailGoogle() throws Exception {
        if (getConfigurationError() != null)
            throw new IllegalStateException("Google OAuth non configuré : " + getConfigurationError());

        // 1. Démarrer le serveur local pour recevoir le callback
        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer server = demarrerServeurCallback(codeFuture);

        try {
            // 2. Construire l'URL Google OAuth et ouvrir le navigateur
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&access_type=offline"
                + "&prompt=select_account";  // Force l'affichage du sélecteur de compte

            if (!Desktop.isDesktopSupported())
                throw new IllegalStateException("Ouverture du navigateur non supportée.");

            Desktop.getDesktop().browse(URI.create(authUrl));
            System.out.println("[GoogleOAuth] Navigateur ouvert : " + authUrl);

            // 3. Attendre le code d'autorisation (timeout 2 min)
            String code = codeFuture.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (code == null || code.isBlank())
                throw new IllegalStateException("Aucun code reçu de Google.");

            // 4. Échanger le code contre un access_token
            String accessToken = echangerCodeContreToken(code);

            // 5. Récupérer l'email depuis l'API Google
            return obtenirEmailDepuisToken(accessToken);

        } finally {
            server.stop(0);
        }
    }

    // ── Serveur callback local ────────────────────────────────────────────────

    private HttpServer demarrerServeurCallback(CompletableFuture<String> codeFuture) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code  = extraireParam(query, "code");
            String error = extraireParam(query, "error");

            String html;
            if (code != null && !code.isBlank()) {
                html = "<html><body style='font-family:Arial;text-align:center;padding:60px;'>"
                     + "<h2 style='color:#2d7a58;'>✅ Connexion réussie !</h2>"
                     + "<p>Vous pouvez fermer cet onglet et retourner à l'application AfkArt.</p>"
                     + "</body></html>";
                codeFuture.complete(code);
            } else {
                html = "<html><body style='font-family:Arial;text-align:center;padding:60px;'>"
                     + "<h2 style='color:#d93025;'>❌ Connexion annulée</h2>"
                     + "<p>" + (error != null ? error : "Erreur inconnue") + "</p>"
                     + "</body></html>";
                codeFuture.completeExceptionally(new Exception("Google OAuth annulé : " + error));
            }

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        server.start();
        System.out.println("[GoogleOAuth] Serveur callback démarré sur port " + CALLBACK_PORT);
        return server;
    }

    // ── Échange code → token ──────────────────────────────────────────────────

    private String echangerCodeContreToken(String code) throws Exception {
        String body = "code=" + URLDecoder.decode(code, StandardCharsets.UTF_8)
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&redirect_uri=" + REDIRECT_URI
                + "&grant_type=authorization_code";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new Exception("Erreur token Google : " + response.body());

        String accessToken = extraireJson(response.body(), "access_token");
        if (accessToken == null)
            throw new Exception("access_token manquant dans la réponse Google.");
        return accessToken;
    }

    // ── Récupérer email ───────────────────────────────────────────────────────

    private String obtenirEmailDepuisToken(String accessToken) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new Exception("Erreur userinfo Google : " + response.body());

        String email = extraireJson(response.body(), "email");
        if (email == null)
            throw new Exception("Email manquant dans la réponse Google.");

        System.out.println("[GoogleOAuth] Email récupéré : " + email);
        return email;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String extraireParam(String query, String param) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param))
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private String extraireJson(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("google-oauth.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("[GoogleOAuth] google-oauth.properties introuvable");
        }
        return props;
    }
}
