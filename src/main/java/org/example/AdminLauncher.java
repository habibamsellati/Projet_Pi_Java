package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.utils.SessionStore;
import org.example.services.ServiceUser;
import org.example.models.User;

public class AdminLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Garantir que les comptes nécessaires existent avant de lancer
        org.example.utils.SystemInitializer.initializeData();

        ServiceUser su = new ServiceUser();
        // Connexion forcée en ADMIN pour la démo
        User admin = su.trouverParEmail("admin@test.com");
        if (admin != null) {
            admin.setRole(User.ROLE_ADMIN);
            SessionStore.login(admin);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminLayout.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("AFK'ART - Administration Centrale");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
