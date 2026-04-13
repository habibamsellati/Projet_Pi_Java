package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class TestFXML extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelleReservation.fxml"));
            loader.load();
            System.out.println("TEST_SUCCESS_NO_EXCEPTION");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("TEST_ERROR_TRACE:");
            e.printStackTrace();
            if (e.getCause() != null) {
                System.err.println("CAUSE:");
                e.getCause().printStackTrace();
                if (e.getCause().getCause() != null) {
                    System.err.println("ROOT CAUSE:");
                    e.getCause().getCause().printStackTrace();
                }
            }
            System.exit(1);
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
