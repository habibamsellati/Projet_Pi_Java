package org.example.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.models.Role;
import org.example.models.User;

public final class SceneNavigator {
    private SceneNavigator() {
    }

    public static void navigate(ActionEvent event, String fxmlPath, String title, double width, double height) throws Exception {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        navigate(stage, fxmlPath, title, width, height);
    }

    public static void navigate(Stage stage, String fxmlPath, String title, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        Parent root = loader.load();
        stage.setScene(new Scene(root, width, height));
        stage.setTitle(title);
        stage.show();
    }

    public static void openDashboardFor(User user, Stage stage) throws Exception {
        if (user == null) {
            navigate(stage, "/fxml/Login.fxml", "Connexion", 520, 420);
            return;
        }

        switch (user.getRole()) {
            case ADMIN      -> navigate(stage, "/fxml/AdminDashboard.fxml",   "afk'art – Backoffice Admin", 1200, 780);
            case ARTISANT   -> navigate(stage, "/fxml/MainView.fxml",         "Artefact",                   1200, 750);
            case CLIENT     -> navigate(stage, "/fxml/MainView.fxml",         "Artefact",                   1200, 750);
            case LIVREUR    -> navigate(stage, "/fxml/MainView.fxml",         "Artefact – Livreur",         1200, 750);
            case RESPONSABLE-> navigate(stage, "/fxml/AdminDashboard.fxml",   "afk'art – Responsable",      1200, 780);
            default         -> navigate(stage, "/fxml/Login.fxml",            "Connexion",                  520,  420);
        }
    }
}

