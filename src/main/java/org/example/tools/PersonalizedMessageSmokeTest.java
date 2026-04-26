package org.example.tools;

import org.example.services.PersonalizedMessageService;

import java.util.List;

public class PersonalizedMessageSmokeTest {

    public static void main(String[] args) {
        System.out.println("=== PersonalizedMessageService Smoke Test ===\n");

        PersonalizedMessageService service = new PersonalizedMessageService();

        // Test 1 : Commande simple
        System.out.println("Test 1 - Commande simple:");
        testCommand(service, "benghrib issra", "Vase céramique, Tapis tissé", 750.00, "CMD-TEST-001");

        // Test 2 : Commande multi-articles
        System.out.println("\nTest 2 - Commande multi-articles:");
        testCommand(service, "Amir", "Vase céramique, Tapis tissé, Sculpture en bois, Bracelet artisanal", 1250.00, "CMD-TEST-002");

        // Test 3 : Sans API key (fallback)
        System.out.println("\nTest 3 - Fallback messages:");
        testCommand(service, "Fatima", "Bracelet", 300.00, "CMD-TEST-003");
    }

    private static void testCommand(PersonalizedMessageService service, String nom, String articles, double total, String orderNumber) {
        List<String> articlesList = List.of(articles.split(", "));
        PersonalizedMessageService.MessageGenerationResult result =
                service.generateOrderConfirmationMessage(nom, articlesList, total, orderNumber);

        System.out.println("  Nom client: " + nom);
        System.out.println("  Articles: " + articles);
        System.out.println("  Total: " + total + " DT");
        System.out.println("  Référence commande: " + orderNumber);
        System.out.println("  Source: " + result.source());
        System.out.println("  AI généré: " + result.aiGenerated());
        System.out.println("  Message généré:");
        System.out.println("  ---");
        System.out.println("  " + result.message());
        System.out.println("  ---");
    }
}

