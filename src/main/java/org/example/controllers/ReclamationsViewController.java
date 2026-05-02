package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.example.models.Role;
import org.example.models.User;
import org.example.utils.SessionManager;

import java.io.IOException;

/**
 * Contrôleur wrapper pour gérer la redirection intelligente vers:
 * - ReclamationsClient.fxml si l'utilisateur est CLIENT ou ARTISAN
 * - ReclamationsAdmin.fxml si l'utilisateur est ADMIN
 */
public class ReclamationsViewController {
    @FXML
    private BorderPane root;

    @FXML
    void initialize() {
        loadAppropriateView();
    }

    private void loadAppropriateView() {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null) {
                showError("Utilisateur non connecté", "Veuillez vous reconnecter");
                return;
            }

            String fxmlToLoad;
            if (currentUser.getRole() == Role.ADMIN) {
                fxmlToLoad = "/fxml/ReclamationsAdmin.fxml";
            } else {
                fxmlToLoad = "/fxml/ReclamationsClient.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
            Parent content = loader.load();

            if (root == null) {
                throw new IllegalStateException("La racine FXML 'root' n'a pas été injectée.");
            }

            root.setTop(null);
            root.setBottom(null);
            root.setLeft(null);
            root.setRight(null);
            root.setCenter(content);

        } catch (IOException e) {
            showError("Erreur de chargement IO", e.getMessage());
        } catch (Exception e) {
            showError("Erreur générale", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


