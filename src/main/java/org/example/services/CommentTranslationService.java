package org.example.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service de traduction pour les commentaires.
 * Utilise MyMemory (gratuit, sans cle) avec timeout court.
 */
public class CommentTranslationService {

    private static final Pattern TRANSLATED_TEXT_PATTERN =
            Pattern.compile("\"translatedText\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private static final Pattern GOOGLE_FIRST_TRANSLATION_PATTERN =
            Pattern.compile("^\\s*\\[\\[\\[\"((?:\\\\.|[^\"])*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String translate(String text, String targetLang) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) {
            return "";
        }

        String lang = normalizeLang(targetLang);
        String translated = translateWithMyMemory(text, lang);
        if (isUsableTranslation(translated, text)) {
            return translated;
        }

        translated = translateWithGoogle(text, lang);
        if (isUsableTranslation(translated, text)) {
            return translated;
        }

        throw new IOException("Reponse de traduction invalide");
    }

    private String translateWithMyMemory(String text, String targetLang) throws IOException, InterruptedException {
        // MyMemory exige une source explicite (auto provoque 'INVALID SOURCE LANGUAGE').
        String sourceLang = "fr";
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=" + sourceLang + "%7C" + targetLang;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode());
        }

        return extractTranslatedText(response.body());
    }

    private String translateWithGoogle(String text, String targetLang) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                + targetLang + "&dt=t&q=" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode());
        }

        return extractFromGoogleResponse(response.body());
    }

    private String normalizeLang(String lang) {
        if (lang == null) return "fr";
        String l = lang.trim().toLowerCase();
        return switch (l) {
            case "fr", "en", "ar" -> l;
            default -> "fr";
        };
    }

    private String extractTranslatedText(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Matcher m = TRANSLATED_TEXT_PATTERN.matcher(json);
        if (!m.find()) {
            return null;
        }

        return unescapeJson(m.group(1));
    }

    private String extractFromGoogleResponse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Matcher m = GOOGLE_FIRST_TRANSLATION_PATTERN.matcher(json);
        if (!m.find()) {
            return null;
        }
        return unescapeJson(m.group(1));
    }

    private boolean isUsableTranslation(String translated, String sourceText) {
        if (translated == null || translated.isBlank()) {
            return false;
        }

        String t = translated.toLowerCase();
        if (t.contains("invalid source language") || t.contains("error")) {
            return false;
        }

        return true;
    }

    private String unescapeJson(String s) {
        if (s == null) return null;

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                out.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            out.append("\\u");
                        }
                    }
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

