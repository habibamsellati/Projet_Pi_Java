package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.models.User;

public class RoleSpaceController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private Label card1TitleLabel;

    @FXML
    private Label card1SubtitleLabel;

    @FXML
    private Label card2TitleLabel;

    @FXML
    private Label card2SubtitleLabel;

    @FXML
    private Label card3TitleLabel;

    @FXML
    private Label card3SubtitleLabel;

    private User currentUser;

    public void configure(User user,
                          String pageTitle,
                          String pageSubtitle,
                          String card1Title,
                          String card1Subtitle,
                          String card2Title,
                          String card2Subtitle,
                          String card3Title,
                          String card3Subtitle) {

        this.currentUser = user;

        String fullName = "";
        if (user != null) {
            String prenom = user.getPrenom() != null ? user.getPrenom() : "";
            String nom = user.getNom() != null ? user.getNom() : "";
            fullName = (prenom + " " + nom).trim();
        }

        if (fullName.isEmpty() && user != null && user.getEmail() != null) {
            fullName = user.getEmail();
        }

        userNameLabel.setText(fullName);
        userRoleLabel.setText("(" + user.getRole().toUpperCase() + ")");

        pageTitleLabel.setText(pageTitle);
        pageSubtitleLabel.setText(pageSubtitle);

        card1TitleLabel.setText(card1Title);
        card1SubtitleLabel.setText(card1Subtitle);

        card2TitleLabel.setText(card2Title);
        card2SubtitleLabel.setText(card2Subtitle);

        card3TitleLabel.setText(card3Title);
        card3SubtitleLabel.setText(card3Subtitle);
    }

    @FXML
    public void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/profile.fxml"));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}