package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.models.Role;
import org.example.services.AvatarService;
import org.example.services.UserManagementService;
import org.example.utils.SceneNavigator;

public class SignupController {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfMotDePasse;
    @FXML private ComboBox<String> cbSexe;
    @FXML private ComboBox<String> cbRole;
    @FXML private ImageView imgAvatarPreview;
    @FXML private Label lblMessage;

    private final UserManagementService userService = new UserManagementService();
    private final AvatarService avatarService = new AvatarService();
    private String avatarUrl;

    @FXML
    void initialize() {
        cbSexe.setItems(FXCollections.observableArrayList("Homme", "Femme", "Non spécifié"));
        cbSexe.getSelectionModel().select("Non spécifié");
        cbRole.setItems(FXCollections.observableArrayList("CLIENT", "ARTISANT", "LIVREUR"));
        cbRole.getSelectionModel().select("CLIENT");

        tfNom.textProperty().addListener((obs, oldV, newV) -> refreshAvatarPreview());
        tfPrenom.textProperty().addListener((obs, oldV, newV) -> refreshAvatarPreview());
        tfEmail.textProperty().addListener((obs, oldV, newV) -> refreshAvatarPreview());
        cbSexe.valueProperty().addListener((obs, oldV, newV) -> refreshAvatarPreview());

        refreshAvatarPreview();
    }

    @FXML
    void handleInscription(ActionEvent event) {
        try {
            Role role = Role.fromDatabaseValue(cbRole.getValue());
            if (role == Role.UNKNOWN) role = Role.CLIENT;

            userService.inscrireUtilisateur(
                    tfNom.getText(),
                    tfPrenom.getText(),
                    tfEmail.getText(),
                    pfMotDePasse.getText(),
                    role,
                    cbSexe.getValue(),
                    avatarUrl
            );

            // Générer et envoyer le code de vérification
            String code = genererCode();
            envoyerCodeVerification(tfPrenom.getText(), tfNom.getText(), tfEmail.getText(), code);

            // Demander à l'utilisateur de saisir le code
            demanderCodeVerification(event, tfEmail.getText(), code);

        } catch (Exception e) {
            lblMessage.setText(e.getMessage());
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private String genererCode() {
        int code = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(code);
    }

    private void envoyerCodeVerification(String prenom, String nom, String email, String code) {
        try {
            org.example.services.EmailService emailService = new org.example.services.EmailService();
            emailService.envoyerCodeVerification(email, prenom + " " + nom, code);
            System.out.println("[Signup] Code envoyé à " + email + " : " + code);
        } catch (Exception e) {
            System.err.println("[Signup] Email non envoyé : " + e.getMessage());
        }
    }

    private void demanderCodeVerification(ActionEvent event, String email, String codeAttendu) {
        // Popup de saisie du code
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Vérification de l'email");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().setStyle(
            "-fx-background-color: white; -fx-font-family: 'Segoe UI';"
        );

        // Contenu personnalisé
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-padding: 20;");

        javafx.scene.control.Label titre = new javafx.scene.control.Label("Vérifiez votre email");
        titre.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#5D4037;");

        javafx.scene.control.Label info = new javafx.scene.control.Label(
            "Un code à 6 chiffres a été envoyé à :\n" + email);
        info.setStyle("-fx-font-size:13;-fx-text-fill:#7a6a5a;-fx-text-alignment:center;");
        info.setWrapText(true);
        info.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.TextField tfCode = new javafx.scene.control.TextField();
        tfCode.setPromptText("Entrez le code à 6 chiffres");
        tfCode.setStyle(
            "-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#2f2430;" +
            "-fx-background-radius:10;-fx-border-radius:10;" +
            "-fx-border-color:#dcd5ce;-fx-padding:12;-fx-alignment:CENTER;" +
            "-fx-max-width:200;"
        );
        tfCode.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Label lblErreur = new javafx.scene.control.Label("");
        lblErreur.setStyle("-fx-text-fill:#d93025;-fx-font-size:12;");

        content.getChildren().addAll(titre, info, tfCode, lblErreur);
        dialog.getDialogPane().setContent(content);
        dialog.getEditor().setVisible(false);
        dialog.getEditor().setManaged(false);

        // Boutons
        dialog.getDialogPane().getButtonTypes().setAll(
            javafx.scene.control.ButtonType.OK,
            javafx.scene.control.ButtonType.CANCEL
        );

        // Styliser le bouton OK
        javafx.scene.Node btnOk = dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        btnOk.setStyle(
            "-fx-background-color:#c8763a;-fx-text-fill:white;" +
            "-fx-background-radius:20;-fx-padding:8 24;-fx-font-weight:bold;"
        );

        // Validation à chaque clic OK
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String saisi = tfCode.getText().trim();
            if (!saisi.equals(codeAttendu)) {
                lblErreur.setText("Code incorrect. Réessayez.");
                tfCode.clear();
                tfCode.requestFocus();
                e.consume(); // empêche la fermeture
            }
        });

        dialog.showAndWait().ifPresent(ignored -> {
            String saisi = tfCode.getText().trim();
            if (saisi.equals(codeAttendu)) {
                new Alert(Alert.AlertType.INFORMATION,
                    "Email vérifié avec succès !\nVous pouvez maintenant vous connecter.")
                    .showAndWait();
                try {
                    SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
                } catch (Exception ex) {
                    lblMessage.setText(ex.getMessage());
                }
            }
        });
    }

    @FXML
    void handleRefreshAvatar() {
        refreshAvatarPreview();
    }

    @FXML
    void handleRetourLogin(ActionEvent event) {
        try {
            SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) {
            lblMessage.setText(e.getMessage());
        }
    }

    private void refreshAvatarPreview() {
        avatarUrl = avatarService.generateAvatarUrl(
                cbSexe == null ? null : cbSexe.getValue(),
                tfNom == null ? null : tfNom.getText(),
                tfPrenom == null ? null : tfPrenom.getText(),
                tfEmail == null ? null : tfEmail.getText()
        );

        if (imgAvatarPreview == null || avatarUrl == null || avatarUrl.isBlank()) return;

        // Chargement en arrière-plan pour ne pas bloquer l'UI
        String urlToLoad = avatarUrl;
        javafx.concurrent.Task<javafx.scene.image.Image> task = new javafx.concurrent.Task<>() {
            @Override
            protected javafx.scene.image.Image call() {
                return new javafx.scene.image.Image(urlToLoad, 80, 80, true, true, false);
            }
        };
        task.setOnSucceeded(e -> {
            javafx.application.Platform.runLater(() -> {
                javafx.scene.image.Image img = task.getValue();
                if (img != null && !img.isError()) {
                    imgAvatarPreview.setImage(img);
                }
            });
        });
        new Thread(task, "avatar-loader").start();
    }
}

