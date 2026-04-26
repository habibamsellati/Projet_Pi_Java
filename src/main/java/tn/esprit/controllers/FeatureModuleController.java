package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class FeatureModuleController {

    @FXML
    private Label moduleTitleLabel;

    @FXML
    private Label moduleSubtitleLabel;

    @FXML
    private Label userInfoLabel;

    @FXML
    private Label roleInfoLabel;

    @FXML
    private Label bigModuleLabel;

    @FXML
    private Label moduleDescriptionLabel;

    @FXML
    private Label icon1Label;

    @FXML
    private Label icon2Label;

    @FXML
    private Label icon3Label;

    @FXML
    private Label stat1ValueLabel;

    @FXML
    private Label stat1TitleLabel;

    @FXML
    private Label stat2ValueLabel;

    @FXML
    private Label stat2TitleLabel;

    @FXML
    private Label stat3ValueLabel;

    @FXML
    private Label stat3TitleLabel;

    private User currentUser;

    public void configure(
            User user,
            String title,
            String subtitle,
            String description,
            String icon1, String stat1Value, String stat1Title,
            String icon2, String stat2Value, String stat2Title,
            String icon3, String stat3Value, String stat3Title
    ) {
        this.currentUser = user;

        moduleTitleLabel.setText(title);
        moduleSubtitleLabel.setText(subtitle);
        bigModuleLabel.setText(title);
        moduleDescriptionLabel.setText(description);

        icon1Label.setText(icon1);
        stat1ValueLabel.setText(stat1Value);
        stat1TitleLabel.setText(stat1Title);

        icon2Label.setText(icon2);
        stat2ValueLabel.setText(stat2Value);
        stat2TitleLabel.setText(stat2Title);

        icon3Label.setText(icon3);
        stat3ValueLabel.setText(stat3Value);
        stat3TitleLabel.setText(stat3Title);

        if (user != null) {
            String fullName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(user.getEmail());
            }

            userInfoLabel.setText(fullName);
            roleInfoLabel.setText(safe(user.getRole()).toUpperCase());
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}