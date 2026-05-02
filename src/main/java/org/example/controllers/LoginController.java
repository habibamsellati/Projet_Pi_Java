package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.AuthService;
import org.example.services.GoogleOAuthService;
import org.example.utils.SceneNavigator;

import java.util.concurrent.ThreadLocalRandom;

public class LoginController {

    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfMotDePasse;
    @FXML private TextField     tfCaptcha;
    @FXML private ImageView     ivCaptcha;
    @FXML private Label         lblInfo;
    @FXML private Button        btnGoogle;

    private final AuthService       authService   = new AuthService();
    private final GoogleOAuthService googleOAuth  = new GoogleOAuthService();
    private String captchaCode = "";

    @FXML
    public void initialize() {
        genererCaptcha();
    }

    // ── CAPTCHA ───────────────────────────────────────────────────────────────

    private void genererCaptcha() {
        captchaCode = randomCaptchaCode(6);
        if (ivCaptcha == null) return;

        final int width = 170, height = 55;
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#FFF8F1"));
        gc.fillRoundRect(0, 0, width, height, 12, 12);
        gc.setStroke(Color.web("#DCCFC2"));
        gc.strokeRoundRect(0.5, 0.5, width - 1, height - 1, 12, 12);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 9; i++) {
            gc.setStroke(Color.color(
                rng.nextDouble(0.45, 0.75),
                rng.nextDouble(0.35, 0.65),
                rng.nextDouble(0.25, 0.55), 0.33));
            gc.strokeLine(rng.nextInt(width), rng.nextInt(height),
                          rng.nextInt(width), rng.nextInt(height));
        }

        gc.setFont(Font.font("Consolas", 24));
        for (int i = 0; i < captchaCode.length(); i++) {
            gc.save();
            gc.translate(17 + i * 24, 36 + rng.nextInt(-3, 4));
            gc.rotate(rng.nextInt(-20, 21));
            gc.setFill(Color.web(i % 2 == 0 ? "#8D6E63" : "#C8763A"));
            gc.fillText(String.valueOf(captchaCode.charAt(i)), 0, 0);
            gc.restore();
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        ivCaptcha.setImage(canvas.snapshot(params, null));
    }

    @FXML
    void handleRefreshCaptcha() {
        if (tfCaptcha != null) tfCaptcha.clear();
        genererCaptcha();
    }

    // ── Connexion normale ─────────────────────────────────────────────────────

    @FXML
    void handleConnexion(ActionEvent event) {
        lblInfo.setText("");

        String saisieCaptcha = tfCaptcha != null ? tfCaptcha.getText().trim() : "";
        if (saisieCaptcha.isEmpty()) {
            lblInfo.setText("Veuillez saisir le code CAPTCHA.");
            return;
        }
        if (!captchaCode.equalsIgnoreCase(saisieCaptcha)) {
            lblInfo.setText("Code CAPTCHA incorrect. Veuillez réessayer.");
            handleRefreshCaptcha();
            return;
        }

        try {
            User user = authService.login(tfEmail.getText(), pfMotDePasse.getText());
            ouvrirSession(user);
        } catch (Exception e) {
            lblInfo.setText(e.getMessage());
            handleRefreshCaptcha();
        }
    }

    // ── Connexion Google ──────────────────────────────────────────────────────

    @FXML
    void handleGoogleLogin() {
        lblInfo.setText("");

        // Vérifier si Google OAuth est configuré
        String configError = googleOAuth.getConfigurationError();
        if (configError != null) {
            // Fallback : popup saisie email si pas configuré
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Connexion avec Google");
            dialog.setHeaderText("Google OAuth non configuré.\nEntrez votre email directement :");
            dialog.setContentText("Email :");
            dialog.getEditor().setPromptText("exemple@gmail.com");
            dialog.showAndWait().ifPresent(email -> {
                if (email == null || email.isBlank()) return;
                try {
                    User user = authService.loginAvecGoogle(email.trim());
                    if (user == null) { lblInfo.setText("Aucun compte trouvé pour : " + email.trim()); return; }
                    ouvrirSession(user);
                } catch (Exception e) { lblInfo.setText("Erreur : " + e.getMessage()); }
            });
            return;
        }

        // Vrai flux OAuth — ouvre le navigateur avec la page Google
        btnGoogle.setDisable(true);
        btnGoogle.setText("Connexion en cours...");

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override protected String call() throws Exception {
                return googleOAuth.obtenirEmailGoogle();
            }
        };

        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            String email = task.getValue();
            btnGoogle.setDisable(false);
            btnGoogle.setText("Google");
            try {
                User user = authService.loginAvecGoogle(email);
                if (user == null) {
                    lblInfo.setText("Compte Google non trouvé pour : " + email +
                        "\nVeuillez vous inscrire d'abord.");
                    return;
                }
                ouvrirSession(user);
            } catch (Exception ex) {
                lblInfo.setText("Erreur : " + ex.getMessage());
            }
        }));

        task.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
            btnGoogle.setDisable(false);
            btnGoogle.setText("Google");
            lblInfo.setText("Connexion Google annulée ou expirée.");
        }));

        new Thread(task, "google-oauth-thread").start();
    }

    // ── Autres actions ────────────────────────────────────────────────────────

    @FXML
    void handleForgotPassword() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Mot de passe oublié");
        a.setHeaderText(null);
        a.setContentText("Contactez l'administrateur à : issrabenghrib04@gmail.com");
        a.showAndWait();
    }

    @FXML
    void handleAllerInscription(ActionEvent event) {
        try {
            SceneNavigator.navigate(event, "/fxml/Signup.fxml", "Inscription", 650, 560);
        } catch (Exception e) {
            lblInfo.setText("Page d'inscription non disponible.");
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void ouvrirSession(User user) throws Exception {
        org.example.utils.SessionManager.login(user);
        SessionManager.utilisateurConnecte = new SessionManager.Utilisateur(
            user.getId(), user.getNom(), user.getPrenom(),
            user.getEmail(), user.getRole() == null ? "" : user.getRole().name()
        );
        Stage stage = (Stage) tfEmail.getScene().getWindow();
        SceneNavigator.openDashboardFor(user, stage);
    }

    private String randomCaptchaCode(int length) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        return sb.toString();
    }
}
