package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import org.example.models.Commande;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceCommande;
import org.example.utils.SessionManager;

import java.util.function.UnaryOperator;

public class ModifierCommandeController {
    @FXML private TextField tfNumero;
    @FXML private TextField tfTotal;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfClientId;
    @FXML private TextArea taAdresseLivraison;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<String> cbModePaiement;

    private final ServiceCommande service = new ServiceCommande();
    private Commande commande;
    private Runnable onSaved;

    @FXML
    void initialize() {
        cbStatut.getItems().setAll("en_attente", "confirmee", "livree", "annulee");
        cbModePaiement.getItems().setAll("carte", "espèces", "virement");
        configurerChampTotal();
        configurerChampClientId();
    }

    private void configurerChampTotal() {
        UnaryOperator<TextFormatter.Change> filtre = change -> {
            String texte = change.getControlNewText();
            if (texte.isEmpty() || texte.matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        };
        tfTotal.setTextFormatter(new TextFormatter<>(filtre));
    }

    private void configurerChampClientId() {
        UnaryOperator<TextFormatter.Change> filtre = change -> {
            String texte = change.getControlNewText();
            if (texte.isEmpty() || texte.matches("\\d*")) {
                return change;
            }
            return null;
        };
        tfClientId.setTextFormatter(new TextFormatter<>(filtre));
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
        tfNumero.setText(commande.getNumero());
        tfTotal.setText(commande.getTotal() == null ? "" : String.valueOf(commande.getTotal()));
        tfTelephone.setText(commande.getTelephone() == null ? "" : commande.getTelephone());
        tfClientId.setText(commande.getClientId() == null ? "" : String.valueOf(commande.getClientId()));
        taAdresseLivraison.setText(commande.getAdresseLivraison());
        cbStatut.setValue(commande.getStatut());
        cbModePaiement.setValue(commande.getModePaiement());
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void handleModifier() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null || user.getRole() != Role.ADMIN) {
                throw new IllegalArgumentException("Accès refusé : seul un admin peut modifier une commande.");
            }

            String ancienStatut = commande.getStatut();
            String nouveauStatut = cbStatut.getValue();
            if (!isTransitionAllowed(ancienStatut, nouveauStatut)) {
                throw new IllegalArgumentException("Transition de statut non autorisée : " + ancienStatut + " -> " + nouveauStatut);
            }

            commande.setTotal(tfTotal.getText() == null || tfTotal.getText().isBlank() ? null : Double.parseDouble(tfTotal.getText().trim()));
            commande.setTelephone(videVersNull(tfTelephone.getText()));
            commande.setClientId(tfClientId.getText() == null || tfClientId.getText().isBlank() ? null : Integer.parseInt(tfClientId.getText().trim()));
            commande.setAdresseLivraison(taAdresseLivraison.getText());
            commande.setStatut(nouveauStatut);
            commande.setModePaiement(cbModePaiement.getValue());

            service.modifier(commande);
            new Alert(Alert.AlertType.INFORMATION, "Commande modifiée avec succès !").showAndWait();

            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleAnnuler() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNumero.getScene().getWindow();
        stage.close();
    }

    private String videVersNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private boolean isTransitionAllowed(String oldStatus, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            return false;
        }
        if (oldStatus == null || oldStatus.isBlank()) {
            return "en_attente".equals(newStatus);
        }
        if (oldStatus.equals(newStatus)) {
            return true;
        }
        if ("livree".equals(oldStatus)) {
            return false;
        }
        if ("annulee".equals(newStatus)) {
            return true;
        }
        if ("en_attente".equals(oldStatus) && "confirmee".equals(newStatus)) {
            return true;
        }
        return "confirmee".equals(oldStatus) && "livree".equals(newStatus);
    }
}

