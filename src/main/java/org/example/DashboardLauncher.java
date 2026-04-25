package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.utils.SessionStore;
import org.example.services.ServiceUser;
import org.example.models.User;

public class DashboardLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Préparation d'une session Admin pour la démo
        ServiceUser su = new ServiceUser();
        User admin = su.trouverParEmail("artisan@test.com"); // Ou un compte défini comme ADMIN
        if (admin != null) {
            SessionStore.login(admin);
        }

        // Chargement direct du Dashboard Premium
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminLayout.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("AFK'ART - Dashboard Premium (Mode Démo)");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
