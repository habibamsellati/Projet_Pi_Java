package org.example.services;

import org.example.models.Commentaire;
import org.example.models.Role;
import org.example.models.User;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BadWordService {

    public enum ModerationStrategy {
        BLOQUER,
        CENSURER,
        SIGNALER;

        public static ModerationStrategy fromString(String value) {
            if (value == null || value.isBlank()) {
                return BLOQUER;
            }
            try {
                return ModerationStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BLOQUER;
            }
        }
    }

    public static class ModerationResult {
        private final boolean allowed;
        private final String finalText;
        private final List<String> detectedWords;
        private final boolean flagged;

        public ModerationResult(boolean allowed, String finalText, List<String> detectedWords, boolean flagged) {
            this.allowed = allowed;
            this.finalText = finalText;
            this.detectedWords = detectedWords;
            this.flagged = flagged;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getFinalText() {
            return finalText;
        }

        public List<String> getDetectedWords() {
            return detectedWords;
        }

        public boolean isFlagged() {
            return flagged;
        }
    }

    private final BadWordRepository repository;
    private ModerationStrategy strategy;

    public BadWordService() {
        this(new BadWordRepository(), ModerationStrategy.BLOQUER);
    }

    public BadWordService(BadWordRepository repository, ModerationStrategy strategy) {
        this.repository = repository;
        this.strategy = strategy == null ? ModerationStrategy.BLOQUER : strategy;
    }

    public ModerationStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ModerationStrategy strategy) {
        this.strategy = strategy == null ? ModerationStrategy.BLOQUER : strategy;
    }

    public List<String> listerBadWords() throws SQLException {
        return repository.findAllWords();
    }

    public void ajouterBadWord(String word, User currentUser) throws SQLException {
        requireAdmin(currentUser);
        String cleaned = cleanWord(word);
        boolean inserted = repository.addWord(cleaned, "fr", "MOYEN", currentUser);
        if (!inserted) {
            throw new IllegalArgumentException("Mot interdit deja existant.");
        }
    }

    public void supprimerBadWord(String word, User currentUser) throws SQLException {
        requireAdmin(currentUser);
        String cleaned = cleanWord(word);
        boolean deleted = repository.deleteWord(cleaned);
        if (!deleted) {
            throw new IllegalArgumentException("Mot interdit introuvable.");
        }
    }

    public ModerationResult verifierCommentaire(String texte) throws SQLException {
        String safeText = texte == null ? "" : texte;
        List<String> badWords = listerBadWords();
        List<String> detected = detectForbiddenWords(safeText, badWords);

        if (detected.isEmpty()) {
            return new ModerationResult(true, safeText, detected, false);
        }

        return switch (strategy) {
            case CENSURER -> new ModerationResult(true, censurerTexte(safeText, detected), detected, false);
            case SIGNALER -> new ModerationResult(true, safeText, detected, true);
            case BLOQUER -> new ModerationResult(false, safeText, detected, false);
        };
    }

    public void enregistrerSignalement(Commentaire commentaire, User currentUser, List<String> detectedWords) throws SQLException {
        if (commentaire == null || commentaire.getId() <= 0 || currentUser == null || detectedWords == null || detectedWords.isEmpty()) {
            return;
        }
        String csv = String.join(",", detectedWords);
        repository.saveFlag(commentaire.getId(), currentUser.getId(), csv);
    }

    private void requireAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Action reservee aux administrateurs.");
        }
    }

    private String cleanWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Mot interdit invalide.");
        }
        String cleaned = word.trim().toLowerCase(Locale.ROOT);
        if (cleaned.length() < 2) {
            throw new IllegalArgumentException("Le mot interdit est trop court.");
        }
        return cleaned;
    }

    private List<String> detectForbiddenWords(String text, List<String> forbiddenWords) {
        if (text == null || text.isBlank() || forbiddenWords == null || forbiddenWords.isEmpty()) {
            return List.of();
        }

        String normalizedText = normalize(text);
        Set<String> detected = new LinkedHashSet<>();

        for (String word : forbiddenWords) {
            if (word == null || word.isBlank()) {
                continue;
            }
            String normalizedWord = normalize(word);
            String pattern = "(?<!\\p{L})" + Pattern.quote(normalizedWord) + "(?!\\p{L})";
            if (Pattern.compile(pattern).matcher(normalizedText).find()) {
                detected.add(word);
            }
        }

        return new ArrayList<>(detected);
    }

    private String censurerTexte(String text, List<String> detectedWords) {
        String sanitized = text;
        for (String word : detectedWords) {
            if (word == null || word.isBlank()) {
                continue;
            }
            String stars = "*".repeat(Math.max(3, word.length()));
            Pattern p = Pattern.compile("(?i)(?<!\\p{L})" + Pattern.quote(word) + "(?!\\p{L})");
            Matcher m = p.matcher(sanitized);
            sanitized = m.replaceAll(stars);
        }
        return sanitized;
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{M}+", "");
        return noAccents.toLowerCase(Locale.ROOT);
    }
}

