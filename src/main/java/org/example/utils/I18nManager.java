package org.example.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Gestionnaire d'internationalisation (i18n) pour AfkArt.
 *
 * Utilisation :
 *   I18nManager.setLocale(Locale.FRENCH);       // change la langue
 *   I18nManager.get("comment.publish");          // "Publier le commentaire"
 *   I18nManager.get("btn.like", 5);              // "❤️ J'aime (5)"
 *   I18nManager.isRTL();                         // true si arabe
 */
public final class I18nManager {

    private static final String BASE_NAME = "i18n/messages";

    /** Locale actif — FR par défaut. */
    private static Locale currentLocale = Locale.FRENCH;

    /** Bundle chargé depuis i18n/messages_<locale>.properties */
    private static ResourceBundle bundle = loadBundle(currentLocale);

    private I18nManager() {}

    // ─── API publique ────────────────────────────────────────────────────────

    /** Retourne la traduction de la clé, ou la clé elle-même si introuvable. */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Retourne la traduction avec substitution des paramètres {0}, {1}…
     * Exemple : I18nManager.get("btn.like", 5)  →  "❤️ J'aime (5)"
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /** Change la langue active et recharge le bundle. */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = loadBundle(locale);
    }

    /** Retourne le locale actif. */
    public static Locale getLocale() {
        return currentLocale;
    }

    /**
     * Retourne true si la langue actuelle se lit de droite à gauche (ex : arabe).
     * Utilisé pour définir NodeOrientation.RIGHT_TO_LEFT dans JavaFX.
     */
    public static boolean isRTL() {
        return "ar".equals(currentLocale.getLanguage());
    }

    // ─── Langues prédéfinies pour le sélecteur ───────────────────────────────

    public static final Locale FRENCH  = Locale.FRENCH;
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale ARABIC  = new Locale("ar");

    // ─── Interne ─────────────────────────────────────────────────────────────

    private static ResourceBundle loadBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE_NAME, locale);
        } catch (MissingResourceException e) {
            // Repli sur le français si la locale n'existe pas
            try {
                return ResourceBundle.getBundle(BASE_NAME, Locale.FRENCH);
            } catch (MissingResourceException ex) {
                throw new IllegalStateException("Fichier i18n/messages_fr.properties introuvable.", ex);
            }
        }
    }
}

