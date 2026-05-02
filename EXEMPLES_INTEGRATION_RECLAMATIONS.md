# Exemples d'Intégration Pratiques

## 1. Intégration dans le Dashboard Client

### Exemple: Ajouter le bouton dans ClientDashboard.fxml

```xml
<!-- Ajouter ces lignes dans le fichier ClientDashboard.fxml -->
<Button text="📋 Mes Réclamations" 
       onAction="#handleMesReclamations"
       styleClass="btn-primary"
       style="-fx-padding: 15 30; -fx-font-size: 14;"/>
```

### Exemple: Ajouter la méthode dans ClientDashboardController.java

```java
package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.example.utils.SceneNavigator;

public class ClientDashboardController {
    // ... code existant ...

    @FXML
    void handleMesReclamations(ActionEvent event) {
        try {
            SceneNavigator.navigate(event, "/fxml/ReclamationsClient.fxml", 
                    "Mes Réclamations", 1000, 700);
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la page des réclamations");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

## 2. Intégration dans le Dashboard Admin

### Exemple: Ajouter le bouton dans ArtisanDashboard.fxml

```xml
<!-- Ajouter pour l'admin -->
<Button text="⚙️ Gestion des Réclamations" 
       onAction="#handleGestionReclamations"
       styleClass="btn-admin"
       style="-fx-padding: 15 30; -fx-font-size: 14;"/>
```

### Exemple: Ajouter la méthode dans ArtisanDashboardController.java

```java
package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.example.models.Role;
import org.example.models.User;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

public class ArtisanDashboardController {
    // ... code existant ...

    @FXML
    void handleGestionReclamations(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        
        // Vérifier que c'est un admin
        if (user != null && user.getRole() == Role.ADMIN) {
            try {
                SceneNavigator.navigate(event, "/fxml/ReclamationsAdmin.fxml", 
                        "Gestion des Réclamations", 1200, 800);
            } catch (Exception e) {
                showError("Erreur", "Impossible d'ouvrir la gestion des réclamations");
            }
        } else {
            showError("Erreur", "Accès refusé - Administrateur requis");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

## 3. Utilisation du Service dans une Autre Classe

### Exemple: Ajouter une réclamation programmatiquement

```java
import org.example.models.Reclamation;
import org.example.services.ServiceReclamation;
import java.sql.SQLException;

public class ReclamationHelper {
    
    public static void creerReclamationDiagnostique(int clientId, String titre, String description) {
        ServiceReclamation service = new ServiceReclamation();
        
        try {
            Reclamation rec = new Reclamation();
            rec.setTitre(titre);
            rec.setDescription(description);
            rec.setClientId(clientId);
            
            service.ajouterReclamation(rec);
            System.out.println("✓ Réclamation créée avec succès");
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Erreur de validation: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("✗ Erreur SQL: " + e.getMessage());
        }
    }
    
    public static void afficherStatistiques() {
        ServiceReclamation service = new ServiceReclamation();
        
        try {
            int enAttente = service.compterReclamationsParStatut("en_attente");
            int enCours = service.compterReclamationsParStatut("en_cours");
            int resolu = service.compterReclamationsParStatut("resolu");
            int rejete = service.compterReclamationsParStatut("rejete");
            int sansReponse = service.compterReclamationsSansReponse();
            
            System.out.println("=== Statistiques des Réclamations ===");
            System.out.println("En attente: " + enAttente);
            System.out.println("En cours: " + enCours);
            System.out.println("Résolues: " + resolu);
            System.out.println("Rejetées: " + rejete);
            System.out.println("Sans réponse: " + sansReponse);
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}
```

## 4. Ajouter des Notifications (Optionnel)

### Exemple: Notification quand une réponse est ajoutée

```java
package org.example.services;

import org.example.models.User;

public class NotificationService {
    
    public static void notifierReclamationModifiee(User client, String newStatut) {
        String message = "Votre réclamation a été mise à jour. Nouveau statut: " + 
                        newStatut.replace("_", " ");
        
        // Implémentation simple (pour email ou autre)
        System.out.println("[NOTIFICATION] " + client.getEmail() + ": " + message);
        
        // Optionnel: envoyer un email
        // EmailService.send(client.getEmail(), "Mise à jour de réclamation", message);
    }
    
    public static void notifierNouvelleReponse(User client, String adminNom) {
        String message = "L'administrateur " + adminNom + " a répondu à votre réclamation";
        System.out.println("[NOTIFICATION] " + client.getEmail() + ": " + message);
        
        // Optionnel: envoyer un email
        // EmailService.send(client.getEmail(), "Nouvelle réponse", message);
    }
}
```

## 5. Tester le Service Directement

### Exemple: Test complet du système

```java
import org.example.models.Reclamation;
import org.example.models.ReponseReclamation;
import org.example.services.ServiceReclamation;
import java.sql.SQLException;
import java.util.List;

public class TestReclamationComplet {
    
    public static void main(String[] args) {
        ServiceReclamation service = new ServiceReclamation();
        
        try {
            System.out.println("=== Test Complet du Système ===\n");
            
            // 1. Créer une réclamation
            System.out.println("1. Création d'une réclamation...");
            Reclamation rec = new Reclamation();
            rec.setTitre("Produit endommagé lors de la livraison");
            rec.setDescription("J'ai reçu un vase cassé. C'était un cadeau important. Très déçu.");
            rec.setImageUrl("https://example.com/broken-vase.jpg");
            rec.setClientId(1);
            
            service.ajouterReclamation(rec);
            System.out.println("✓ Réclamation créée\n");
            
            // 2. Charger les réclamations du client
            System.out.println("2. Chargement des réclamations du client...");
            List<Reclamation> reclamations = service.afficherReclamationsClient(1);
            System.out.println("✓ " + reclamations.size() + " réclamation(s) trouvée(s)\n");
            
            // 3. Afficher les détails
            if (!reclamations.isEmpty()) {
                Reclamation derniere = reclamations.get(0);
                System.out.println("3. Détails de la dernière réclamation:");
                System.out.println("   Titre: " + derniere.getTitre());
                System.out.println("   Description: " + derniere.getDescription());
                System.out.println("   Statut: " + derniere.getStatut());
                System.out.println("   Date: " + derniere.getFormattedDate() + "\n");
                
                // 4. Changer le statut
                System.out.println("4. Changement du statut à 'en_cours'...");
                service.modifierStatut(derniere.getId(), "en_cours");
                System.out.println("✓ Statut mis à jour\n");
                
                // 5. Ajouter une réponse
                System.out.println("5. Ajout d'une réponse...");
                ReponseReclamation rep = new ReponseReclamation();
                rep.setContenu("Merci de nous avoir signalé ce problème. Nous allons vous envoyer un remboursement immédiatement. Veuillez nous excuser.");
                rep.setReclamationId(derniere.getId());
                rep.setAdminId(1); // Admin ID
                
                service.ajouterReponse(rep);
                System.out.println("✓ Réponse ajoutée\n");
                
                // 6. Charger les réponses
                System.out.println("6. Chargement des réponses...");
                List<ReponseReclamation> reponses = service.obtenirReponses(derniere.getId());
                System.out.println("✓ " + reponses.size() + " réponse(s) trouvée(s)\n");
                
                for (ReponseReclamation r : reponses) {
                    System.out.println("   Réponse: " + r.getContenu().substring(0, 50) + "...");
                    System.out.println("   Date: " + r.getFormattedDate());
                }
            }
            
            System.out.println("\n✓ Test réussi!");
            
        } catch (SQLException e) {
            System.err.println("✗ Erreur SQL: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Erreur de validation: " + e.getMessage());
        }
    }
}
```

## 6. Personnaliser les Styles

### Exemple: Modifier reclamations-style.css

```css
/* Changer les couleurs des badges */
.status-en-attente {
    -fx-background-color: #FFE5B4;  /* Peach au lieu de jaune */
    -fx-text-fill: #D2691E;          /* Marron */
}

/* Changer la couleur du bouton principal */
.btn-new {
    -fx-background-color: #8B4513;   /* Saddle brown */
    -fx-text-fill: white;
}

.btn-new:hover {
    -fx-background-color: #654321;   /* Plus foncé */
}

/* Ajouter des ombres */
.reclamation-card:hover {
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 25, 0, 0, 15);
}
```

## 7. Ajouter une Barre de Statut avec Statistiques

### Exemple: StatusBar controller

```java
package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.services.ServiceReclamation;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class ReclamationStatusBarController {
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblResolu;
    @FXML private Label lblSansReponse;
    
    private ServiceReclamation service;
    
    @FXML
    void initialize() {
        service = new ServiceReclamation();
        actualiserStatistiques();
        
        // Actualiser toutes les 30 secondes
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                actualiserStatistiques();
            }
        }, 30000, 30000);
    }
    
    private void actualiserStatistiques() {
        try {
            int enAttente = service.compterReclamationsParStatut("en_attente");
            int enCours = service.compterReclamationsParStatut("en_cours");
            int resolu = service.compterReclamationsParStatut("resolu");
            int sansReponse = service.compterReclamationsSansReponse();
            
            lblEnAttente.setText("En attente: " + enAttente);
            lblEnCours.setText("En cours: " + enCours);
            lblResolu.setText("Résolues: " + resolu);
            lblSansReponse.setText("Sans réponse: " + sansReponse);
        } catch (SQLException e) {
            lblEnAttente.setText("Erreur de chargement");
        }
    }
}
```

## 8. Importer dans MainAPP (Optionnel)

```java
package org.example;

import org.example.app.TestReclamation;

public class MainAPP {
    public static void main(String[] args) {
        // Tester le système de réclamations au démarrage
        System.out.println("Démarrage de AfkArt...\n");
        
        // Optionnel: lancer les tests
        // TestReclamation.main(new String[]{});
        
        // Puis lancer l'application
        BlogView.main(args);
    }
}
```

## 9. Gestion des Erreurs Avancées

```java
public class ReclamationErrorHandler {
    
    public static String getErrorMessage(Exception e) {
        if (e instanceof IllegalArgumentException) {
            // Erreur de validation
            return "Validation échouée: " + e.getMessage();
        } else if (e instanceof SQLException) {
            // Erreur base de données
            if (e.getMessage().contains("Duplicate")) {
                return "Cet élément existe déjà";
            }
            return "Erreur de base de données";
        }
        return "Une erreur inattendue s'est produite";
    }
    
    public static void logError(String context, Exception e) {
        System.err.println("[ERREUR] " + context);
        System.err.println("Type: " + e.getClass().getSimpleName());
        System.err.println("Message: " + e.getMessage());
        e.printStackTrace();
    }
}
```

---

**Ces exemples vous permettront d'intégrer rapidement le système dans votre application! 🚀**

