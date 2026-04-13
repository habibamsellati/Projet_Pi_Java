package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.models.Evenement;
import org.example.models.Reservation;
import java.io.File;
import java.time.format.DateTimeFormatter;

public class TicketController {

    @FXML public Label lblEvenementTitre, lblDateHeure, lblLieu, lblNbPlaces, lblPrix;
    @FXML public Label lblActionRequise, lblPaye, lblExpiration;
    @FXML public javafx.scene.layout.VBox boxPaiement;
    @FXML public javafx.scene.control.Button btnFinaliser;
    @FXML public ImageView ivEvent;

    private javafx.animation.Timeline timeline;

    public void setReservationData(Reservation r, Evenement e) {
        if (lblEvenementTitre != null) lblEvenementTitre.setText(e.getNom().toUpperCase());
        
        if (lblDateHeure != null && r.getDateReservation() != null) {
            lblDateHeure.setText(r.getDateReservation().format(DateTimeFormatter.ofPattern("dd MMMM yyyy | HH:mm")));
        }
        
        if (lblLieu != null) lblLieu.setText(e.getLieu() != null ? e.getLieu() : "bejaa");
        
        if (lblNbPlaces != null) lblNbPlaces.setText(r.getNbPlaces() + (r.getNbPlaces() > 1 ? " Places" : " Place"));
        
        if (lblPrix != null) {
            double total = r.getNbPlaces() * (e.getPrix() != null ? e.getPrix().doubleValue() : 0.0);
            lblPrix.setText(String.format("%.2f TND", total));
        }

        if (ivEvent != null && e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                File file = new File(e.getImage());
                if (file.exists()) {
                    ivEvent.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception ex) {}
        }

        // --- Logique d'affichage selon le statut ---
        boolean enAttente = org.example.services.ServiceReservation.STATUT_EN_ATTENTE.equals(r.getStatut());
        
        if (lblActionRequise != null) {
            lblActionRequise.setText("⏳ EN ATTENTE DE PAIEMENT");
            lblActionRequise.setVisible(enAttente);
            lblActionRequise.setManaged(enAttente);
        }
        if (boxPaiement != null) {
            boxPaiement.setVisible(enAttente);
            boxPaiement.setManaged(enAttente);
        }
        if (lblPaye != null) {
            lblPaye.setText("✓ RÉSERVATION CONFIRMÉE");
            lblPaye.setVisible(!enAttente);
            lblPaye.setManaged(!enAttente);
        }

        if (enAttente && r.getCreatedAt() != null) {
            startCountdown(r.getCreatedAt());
        }
    }

    private void startCountdown(java.time.LocalDateTime createdAt) {
        if (timeline != null) timeline.stop();
        
        java.time.LocalDateTime expirationTime = createdAt.plusHours(3);
        
        timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                java.time.Duration diff = java.time.Duration.between(java.time.LocalDateTime.now(), expirationTime);
                
                if (diff.isNegative() || diff.isZero()) {
                    if (lblExpiration != null) lblExpiration.setText("Billet Expiré");
                    if (btnFinaliser != null) btnFinaliser.setDisable(true);
                    timeline.stop();
                } else {
                    long h = diff.toHours();
                    long m = diff.toMinutesPart();
                    long s = diff.toSecondsPart();
                    if (lblExpiration != null) {
                        lblExpiration.setText(String.format("Expire dans %02d:%02d:%02d", h, m, s));
                    }
                }
            }
        }));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
}
