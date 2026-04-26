package org.example.tools;

import org.example.services.BadWordService;

public class BadWordsSmokeTest {

    public static void main(String[] args) {
        String sample = args.length == 0 ? "Ce produit est nul et idiot" : String.join(" ", args);

        try {
            BadWordService service = new BadWordService();
            BadWordService.ModerationResult result = service.verifierCommentaire(sample);

            System.out.println("Texte: " + sample);
            System.out.println("Valide: " + result.isAllowed());
            System.out.println("Mots detectes: " + result.getDetectedWords());
            System.out.println("Texte final: " + result.getFinalText());
            System.out.println("Signale: " + result.isFlagged());
        } catch (Exception e) {
            System.err.println("Smoke test Bad Words en echec: " + e.getMessage());
        }
    }
}

