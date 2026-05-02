package org.example.services;

import org.example.models.Evenement;
import java.util.*;

public class SmartFillService {

    /**
     * Prediction logic ported from Symfony/Python fallback.
     */
    public double predictFillRate(Evenement event) {
        int placesPrises = getPlacesPrises(event);
        if (event.getCapacite() > 0 && placesPrises >= event.getCapacite()) {
            return 100.0;
        }
        
        // Calculate the sub-scores to average them out
        Map<String, Integer> analysis = calculateStrategicAnalysis(event, 0); // 0 is dummy prediction here
        double average = (analysis.get("price_score") + analysis.get("content_score") + 
                          analysis.get("reach_score") + analysis.get("timing_score")) / 4.0;
                          
        return Math.min(100.0, average);
    }
    
    private int getPlacesPrises(Evenement event) {
        try {
            org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
            return sr.sommePlacesReserveesActives(event.getId());
        } catch (Exception e) {
            return 0;
        }
    }

    public Map<String, Integer> calculateStrategicAnalysis(Evenement event, double dummyPrediction) {
        Map<String, Integer> analysis = new HashMap<>();
        
        int placesPrises = getPlacesPrises(event);
        double realFillRate = (event.getCapacite() > 0) ? ((double) placesPrises / event.getCapacite()) * 100.0 : 0.0;

        if (realFillRate >= 100.0) {
            analysis.put("price_score", 100);
            analysis.put("content_score", 100);
            analysis.put("reach_score", 100);
            analysis.put("timing_score", 100);
            return analysis;
        }

        // 1. Price Score (Attractivité Prix)
        double price = (event.getPrix() != null) ? event.getPrix().doubleValue() : 0.0;
        int priceScore;
        if (price == 0) priceScore = 100;
        else priceScore = (int) Math.max(10, Math.min(100, 100 - (price * 0.8)));

        // 2. Content Score (Qualité Contenu)
        int descLen = (event.getDescription() != null) ? event.getDescription().length() : 0;
        int contentScore = (int) Math.min(80, (descLen / 300.0) * 80);
        if (event.getImage() != null && !event.getImage().isEmpty()) {
            contentScore += 20; // Bonus for having an image
        }
        contentScore = Math.min(100, contentScore);

        // 3. Market Opportunity (Opportunité Marché)
        String type = event.getTypeArt() != null ? event.getTypeArt().toLowerCase() : "";
        int marketScore = 75; // Baseline
        if (type.contains("céramique") || type.contains("ceramique")) marketScore = 95;
        else if (type.contains("peinture")) marketScore = 88;
        else if (type.contains("bijoux")) marketScore = 90;
        else if (type.contains("sculpture")) marketScore = 80;

        // 4. Visibility/Reach Score (Portée Visibilité)
        // Based on current real fill rate + a base factor so it's not 0 for new events
        int reachScore = (int) Math.min(100, 30 + (realFillRate * 1.2));

        analysis.put("price_score", priceScore);
        analysis.put("content_score", contentScore);
        analysis.put("timing_score", marketScore); // Used as Market Opportunity
        analysis.put("reach_score", reachScore);
        
        return analysis;
    }

    public List<Map<String, String>> getStrategicChecklist(Evenement event, Map<String, Integer> analysis) {
        List<Map<String, String>> checklist = new ArrayList<>();
        
        if (analysis.get("price_score") == 100 && analysis.get("content_score") == 100 && analysis.get("reach_score") == 100) {
            checklist.add(createTask("Événement COMPLET 🏆", "Succès Total", "Félicitations ! L'événement a atteint sa capacité maximale. La stratégie était parfaite."));
            return checklist;
        }

        // Consistent logic: if content score is < 70, suggest generation
        if (analysis.get("content_score") < 70) {
            Map<String, String> t = createTask("Amélioration du contenu requise", "Critique", "Votre description manque de détails pour rassurer les clients.");
            t.put("actionCode", "GENERATE_DESC");
            t.put("actionLabel", "✨ Générer avec l'IA");
            checklist.add(t);
        } else {
            checklist.add(createTask("Contenu Riche", "Succès", "Votre description est excellente et donne envie de participer."));
        }
        
        // Consistent logic: if price score is < 65, suggest optimization
        if (analysis.get("price_score") < 65) {
            Map<String, String> t = createTask("Prix potentiellement élevé", "Elevé", "Votre tarif freine peut-être les réservations. Un ajustement pourrait relancer les ventes.");
            t.put("actionCode", "OPTIMIZE_PRICE");
            t.put("actionLabel", "📉 Ajuster le prix (-20%)");
            checklist.add(t);
        } else {
            checklist.add(createTask("Prix attractif", "Succès", "Votre politique tarifaire est parfaitement alignée avec le marché."));
        }

        // Consistent logic: if reach is < 50
        if (analysis.get("reach_score") < 50) {
            Map<String, String> t = createTask("Déficit de visibilité", "Moyen", "Le nombre de réservations peine à décoller. Relancez la promotion.");
            t.put("actionCode", "PROMOTE");
            t.put("actionLabel", "📢 Promouvoir");
            checklist.add(t);
        }

        return checklist;
    }

    public Map<String, Double> calculatePriceElasticity(double prediction) {
        Map<String, Double> elasticity = new HashMap<>();
        elasticity.put("decrease_20", Math.min(100.0, prediction * 1.25));
        elasticity.put("current", prediction);
        elasticity.put("increase_10", Math.max(0.0, prediction * 0.90));
        return elasticity;
    }

    public Map<String, String> getInsights(double prediction) {
        Map<String, String> insight = new HashMap<>();
        if (prediction >= 100.0) {
            insight.put("status", "success");
            insight.put("message", "Sold Out ! 🏆");
            insight.put("action", "Votre événement affiche complet.");
        } else if (prediction > 75) {
            insight.put("status", "success");
            insight.put("message", "Succès Garanti ! 🔥");
            insight.put("action", "Forte demande prévue. La stratégie est solide.");
        } else if (prediction > 50) {
            insight.put("status", "warning");
            insight.put("message", "Potentiel Moyen 📈");
            insight.put("action", "Il reste de la marge de progression. Appliquez le plan d'action.");
        } else {
            insight.put("status", "danger");
            insight.put("message", "Attention : Risque élevé ⚠️");
            insight.put("action", "Appliquez d'urgence les correctifs du plan d'action cognitif.");
        }
        return insight;
    }

    private Map<String, String> createTask(String task, String impact, String detail) {
        Map<String, String> map = new HashMap<>();
        map.put("task", task);
        map.put("impact", impact);
        map.put("detail", detail);
        return map;
    }

    public int calculateSuccessScore(Evenement event, double dummyPrediction) {
        // The success score is now EXACTLY the mathematical average of the 4 sub-scores.
        Map<String, Integer> analysis = calculateStrategicAnalysis(event, 0);
        double average = (analysis.get("price_score") + analysis.get("content_score") + 
                          analysis.get("reach_score") + analysis.get("timing_score")) / 4.0;
        return (int) Math.min(100, average);
    }
}
