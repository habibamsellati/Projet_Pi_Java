package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class ModulePlaceholderController {

    @FXML
    private Label moduleTitleLabel;

    @FXML
    private Label bigModuleLabel;

    @FXML
    private Label userInfoLabel;

    private User currentUser;

    public void setContext(User user, String moduleTitle) {
        this.currentUser = user;
        moduleTitleLabel.setText(moduleTitle);
        bigModuleLabel.setText(moduleTitle);

        if (user != null) {
            userInfoLabel.setText(user.getPrenom() + " " + user.getNom() + " - " + user.getRole());
        }
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) moduleTitleLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}