package tn.esprit.utils;

import java.util.HashMap;
import java.util.Map;

public class ModuleAccessManager {

    private static final Map<String, String[]> MODULE_CREDENTIALS = new HashMap<>();

    static {
        MODULE_CREDENTIALS.put("produits", new String[]{"produits", "produit"});
        MODULE_CREDENTIALS.put("propositions", new String[]{"propositions", "prop"});
        MODULE_CREDENTIALS.put("livraison", new String[]{"livraison", "liv"});
        MODULE_CREDENTIALS.put("reclamations", new String[]{"reclamations", "reclam"});
        MODULE_CREDENTIALS.put("articles", new String[]{"articles", "article"});
        MODULE_CREDENTIALS.put("evenements", new String[]{"evenements", "event"});
        MODULE_CREDENTIALS.put("reservations", new String[]{"reservations", "resa"});
        MODULE_CREDENTIALS.put("suivi", new String[]{"suivi", "suivi"});
    }

    public static boolean validateAccess(String moduleKey, String username, String password) {
        if (!MODULE_CREDENTIALS.containsKey(moduleKey)) {
            return false;
        }

        String[] creds = MODULE_CREDENTIALS.get(moduleKey);
        return creds[0].equals(username) && creds[1].equals(password);
    }

    public static String getDefaultUsername(String moduleKey) {
        return MODULE_CREDENTIALS.containsKey(moduleKey) ? MODULE_CREDENTIALS.get(moduleKey)[0] : "";
    }
}
