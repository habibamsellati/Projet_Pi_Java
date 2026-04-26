package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.User;

import java.net.URL;

public class AdminDashboardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;

        if (user != null) {
            String fullName = (safe(user.getNom()) + " " + safe(user.getPrenom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(user.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText(safe(user.getRole()).toUpperCase());
        }
    }

    @FXML
    public void goToDashboard() {
        // Déjà sur dashboard
    }

    @FXML
    public void goToUsers() {
        try {
            URL url = getClass().getResource("/view/admin-users.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/admin-users.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminUsersController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture page utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    public void openProduitsModule() {
        openProtectedModule("produits", "Produits recyclables");
    }

    @FXML
    public void openPropositionsModule() {
        openProtectedModule("propositions", "Propositions");
    }

    @FXML
    public void openLivraisonModule() {
        openProtectedModule("livraison", "Livraison");
    }

    @FXML
    public void openReclamationsModule() {
        openProtectedModule("reclamations", "Réclamations");
    }

    @FXML
    public void openArticlesModule() {
        openProtectedModule("articles", "Articles");
    }

    @FXML
    public void openEvenementsModule() {
        openProtectedModule("evenements", "Evenements");
    }

    @FXML
    public void openReservationsModule() {
        openProtectedModule("reservations", "Réservations");
    }

    @FXML
    public void openSuiviModule() {
        openProtectedModule("suivi", "Suivi livraison");
    }

    private void openProtectedModule(String moduleKey, String moduleTitle) {
        try {
            FXMLLoader dialogLoader = new FXMLLoader(getClass().getResource("/view/module-login-dialog.fxml"));
            Parent dialogRoot = dialogLoader.load();

            ModuleLoginDialogController dialogController = dialogLoader.getController();
            dialogController.setModuleKey(moduleKey);

            Stage dialogStage = new Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(userNameLabel.getScene().getWindow());
            dialogStage.setTitle("Authentification module");
            dialogStage.setResizable(false);
            dialogStage.setScene(new Scene(dialogRoot));

            dialogController.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (dialogController.isAuthenticated()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/feature-module.fxml"));
                Parent root = loader.load();

                FeatureModuleController controller = loader.getController();

                switch (moduleKey) {
                    case "produits":
                        controller.configure(
                                currentUser,
                                "Produits recyclables",
                                "Gestion du catalogue recyclé",
                                "Ce module permet de gérer les produits recyclables, suivre les ajouts, les stocks et les catégories.",
                                "📦", "128", "Produits",
                                "🗂", "12", "Catégories",
                                "✅", "96", "Disponibles"
                        );
                        break;

                    case "propositions":
                        controller.configure(
                                currentUser,
                                "Propositions",
                                "Gestion des propositions des utilisateurs",
                                "Ce module centralise les propositions soumises, leur validation et leur suivi administratif.",
                                "💡", "54", "Propositions",
                                "⏳", "17", "En attente",
                                "✅", "31", "Validées"
                        );
                        break;

                    case "livraison":
                        controller.configure(
                                currentUser,
                                "Livraison",
                                "Suivi du service livraison",
                                "Ce module permet de consulter les livraisons en cours, leur état et les affectations.",
                                "🚚", "43", "Livraisons",
                                "📍", "11", "En route",
                                "✅", "29", "Livrées"
                        );
                        break;

                    case "reclamations":
                        controller.configure(
                                currentUser,
                                "Réclamations",
                                "Gestion des réclamations clients",
                                "Ce module permet d’enregistrer, suivre et traiter les réclamations soumises.",
                                "⚠", "32", "Réclamations",
                                "📬", "8", "Nouvelles",
                                "✔", "19", "Résolues"
                        );
                        break;

                    case "articles":
                        controller.configure(
                                currentUser,
                                "Articles",
                                "Gestion des contenus éditoriaux",
                                "Ce module permet de publier, modifier et organiser les articles et publications.",
                                "📰", "21", "Articles",
                                "✍", "6", "Brouillons",
                                "🌍", "15", "Publiés"
                        );
                        break;

                    case "evenements":
                        controller.configure(
                                currentUser,
                                "Evenements",
                                "Organisation des événements",
                                "Ce module gère les événements, leur planification et leur visibilité sur la plateforme.",
                                "🎉", "14", "Evenements",
                                "📅", "5", "À venir",
                                "👥", "240", "Participants"
                        );
                        break;

                    case "reservations":
                        controller.configure(
                                currentUser,
                                "Réservations",
                                "Gestion des réservations",
                                "Ce module permet de consulter et gérer les réservations des utilisateurs.",
                                "📘", "64", "Réservations",
                                "⌛", "18", "En attente",
                                "✅", "39", "Confirmées"
                        );
                        break;

                    case "suivi":
                        controller.configure(
                                currentUser,
                                "Suivi livraison",
                                "Suivi détaillé des livraisons",
                                "Ce module fournit une vue détaillée sur le suivi, les étapes et l’historique des livraisons.",
                                "📍", "26", "Suivis",
                                "🕒", "7", "Retards",
                                "✔", "19", "Terminés"
                        );
                        break;

                    default:
                        controller.configure(
                                currentUser,
                                moduleTitle,
                                "Module sécurisé",
                                "Module accessible après authentification.",
                                "📁", "0", "Element 1",
                                "📊", "0", "Element 2",
                                "✅", "0", "Element 3"
                        );
                }

                Stage stage = (Stage) userNameLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture module : " + e.getMessage());
        }
    }

    @FXML
    public void goToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/front-home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture front : " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        goToFront();
    }

    @FXML
    public void handleLogout() {
        try {
            URL url = getClass().getResource("/view/login.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/login.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur déconnexion : " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenProfile() {
        try {
            URL url = getClass().getResource("/view/profile.fxml");
            if (url == null) {
                showError("Fichier introuvable : /view/profile.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture profil : " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
}