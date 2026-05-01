package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.models.Evenement;
import org.example.models.Reservation;
import org.example.services.ServiceReservation;

import java.io.File;
import java.io.IOException;

public class PaiementReservationController {

    @FXML
    private ImageView ivEventPoster;
    @FXML
    private Label lblEventTitre, lblEventLieu, lblEventTag, lblDateEvt;
    @FXML
    private Label lblPrixUnitaire, lblTotal;
    @FXML
    private Button lblTotalBtn;
    @FXML
    private TextField tfCartName, tfCartNum, tfCartExp, tfCartCvc;

    private Reservation currentReservation;
    private Evenement currentEvenement;

    public void setReservationData(Reservation r, Evenement e) {
        this.currentReservation = r;
        this.currentEvenement = e;

        if (e != null) {
            lblEventTitre.setText(e.getNom());
            lblEventLieu.setText("📍 " + (e.getLieu() != null ? e.getLieu() : ""));
            lblEventTag.setText(e.getTypeArt() != null ? e.getTypeArt().toUpperCase() : "ART");
            lblDateEvt.setText("📅 " + (e.getDateDebut() != null ? e.getDateDebut().toLocalDate().toString() : ""));

            double prixUnitaire = e.getPrix() != null ? e.getPrix().doubleValue() : 0.0;
            double total = prixUnitaire * r.getNbPlaces();

            lblPrixUnitaire.setText(String.format("%.2f TND", prixUnitaire));
            lblTotal.setText(String.format("%.2f TND", total));
            lblTotalBtn.setText(String.format("Payer %.2f TND", total));

            if (e.getImage() != null && !e.getImage().isEmpty()) {
                try {
                    File file = new File(e.getImage());
                    if (file.exists()) {
                        ivEventPoster.setImage(new Image(file.toURI().toString()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @FXML
    void handleContinuerPaiement(ActionEvent event) {
        String holder = tfCartName.getText().trim();
        String number = tfCartNum.getText().replace(" ", "").trim();
        String exp = tfCartExp.getText().trim();
        String cvc = tfCartCvc.getText().trim();

        if (holder.isEmpty() || number.isEmpty() || exp.isEmpty() || cvc.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs de la carte bancaire.").show();
            return;
        }

        if (holder.length() < 3) {
            new Alert(Alert.AlertType.ERROR, "Le nom du titulaire doit contenir au moins 3 caractères.").show();
            return;
        }

        if (!number.matches("\\d{16}")) {
            new Alert(Alert.AlertType.ERROR, "Le numéro de carte doit contenir exactement 16 chiffres.").show();
            return;
        }

        if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            new Alert(Alert.AlertType.ERROR,
                    "La date d'expiration doit être au format MM/AA avec un mois valide (01-12).").show();
            return;
        }

        if (!cvc.matches("\\d{3}")) {
            new Alert(Alert.AlertType.ERROR, "Le code CVC doit contenir exactement 3 chiffres.").show();
            return;
        }

        try {
            // Créer la réservation définitive
            ServiceReservation service = new ServiceReservation();
            // Assurez-vous que le statut est bien "confirme"
            currentReservation.setStatut("confirme");
            service.ajouter(currentReservation);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès du Paiement");
            success.setHeaderText(null);
            success.setContentText("Paiement validé avec succès ! Votre réservation est confirmée.");
            success.showAndWait();

            // Afficher le ticket après le paiement (Similaire à ce qui est fait dans
            // l'historique)
            showTicket(currentReservation, currentEvenement);

            // Retourner à l'accueil
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModuleEvenementReservation.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors du paiement: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    private void showTicket(Reservation r, Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.setReservationData(r, e);

            Stage stage = new Stage();
            stage.setTitle("Billet Artisanal - " + e.getNom());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelleReservation.fxml"));
            Parent root = loader.load();
            NouvelleReservationController c = loader.getController();
            c.preselectEvenement(currentEvenement.getId());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
