package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.models.Evenement;
import org.example.models.Reservation;
import java.io.File;
import java.time.format.DateTimeFormatter;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

public class TicketController {

    @FXML
    public Label lblEvenementTitre, lblDateHeure, lblLieu, lblNbPlaces, lblPrix;
    @FXML
    public Label lblActionRequise, lblPaye, lblExpiration;
    @FXML
    public javafx.scene.layout.VBox boxPaiement;
    @FXML
    public javafx.scene.layout.HBox ticketCard;
    @FXML
    public javafx.scene.control.Button btnFinaliser, btnTelecharger, btnAnnuler;
    @FXML
    public javafx.scene.control.TextField txtCardHolder, txtCardNumber, txtCardExp, txtCardCVC;
    @FXML
    public ImageView ivEvent, ivQRCode;
    @FXML
    public Label lblID;
    @FXML
    public javafx.scene.layout.HBox boxPagination;
    @FXML
    public Label lblPagination;
    @FXML
    public javafx.scene.control.Button btnPrevTicket, btnNextTicket;

    private Reservation currentReservation;
    private Evenement currentEvenement;
    private javafx.animation.Timeline timeline;
    private int currentSubTicketIndex = 1; // 1-based

    public void setReservationData(Reservation r, Evenement e) {
        this.currentReservation = r;
        this.currentEvenement = e;
        if (lblEvenementTitre != null)
            lblEvenementTitre.setText(e.getNom().toUpperCase());
        if (lblID != null)
            lblID.setText(String.format("#AFK-%04d", r.getId()));

        if (lblDateHeure != null && r.getDateReservation() != null) {
            lblDateHeure.setText(r.getDateReservation().format(DateTimeFormatter.ofPattern("dd MMMM yyyy | HH:mm")));
        }

        if (lblLieu != null)
            lblLieu.setText(e.getLieu() != null ? e.getLieu() : "Bejaa");

        if (lblNbPlaces != null)
            lblNbPlaces.setText("1 Place"); // Chaque sous-billet = 1 place

        // Pagination si plusieurs places
        currentSubTicketIndex = 1;
        if (boxPagination != null) {
            boolean multi = r.getNbPlaces() > 1;
            boxPagination.setVisible(multi);
            boxPagination.setManaged(multi);
            if (multi && lblPagination != null)
                lblPagination.setText("Billet 1 / " + r.getNbPlaces());
        }

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
            } catch (Exception ex) {
            }
        }

        updateStatusUI();
        if (org.example.services.ServiceReservation.STATUT_EN_ATTENTE.equals(r.getStatut())
                && r.getCreatedAt() != null) {
            startCountdown(r.getCreatedAt());
        }
    }

    private void updateStatusUI() {
        boolean enAttente = org.example.services.ServiceReservation.STATUT_EN_ATTENTE
                .equals(currentReservation.getStatut());
        boolean annule = org.example.services.ServiceReservation.STATUT_ANNULE
                .equals(currentReservation.getStatut());

        if (lblActionRequise != null) {
            if (annule) {
                lblActionRequise.setText("❌ BILLET EXPIRÉ/ANNULÉ");
                lblActionRequise.setStyle(
                        "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 10px; -fx-font-weight: bold;");
                lblActionRequise.setVisible(true);
                lblActionRequise.setManaged(true);
            } else {
                lblActionRequise.setText("⏳ EN ATTENTE DE PAIEMENT");
                lblActionRequise.setStyle(
                        "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 10px; -fx-font-weight: bold;");
                lblActionRequise.setVisible(enAttente);
                lblActionRequise.setManaged(enAttente);
            }
        }
        if (boxPaiement != null) {
            boxPaiement.setVisible(enAttente);
            boxPaiement.setManaged(enAttente);
        }
        if (lblPaye != null) {
            lblPaye.setText("✓ RÉSERVATION CONFIRMÉE");
            lblPaye.setVisible(!enAttente && !annule);
            lblPaye.setManaged(!enAttente && !annule);
        }
        if (ivQRCode != null) {
            ivQRCode.setVisible(!enAttente && !annule);
            ivQRCode.setManaged(!enAttente && !annule);
            if (!enAttente && !annule) {
                // QR Code unique par sous-billet (siège)
                try {
                    String qrData = "AFKART-TICKET-" + currentReservation.getId()
                            + "-SEAT-" + currentSubTicketIndex
                            + "-USER-" + currentReservation.getUserId();
                    String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + qrData;
                    ivQRCode.setImage(new javafx.scene.image.Image(qrUrl, true));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (btnTelecharger != null) {
            btnTelecharger.setVisible(!enAttente && !annule);
            btnTelecharger.setManaged(!enAttente && !annule);
        }
    }

    @FXML
    private void handlePrevTicket() {
        if (currentReservation == null || currentSubTicketIndex <= 1)
            return;
        currentSubTicketIndex--;
        refreshSubTicket();
    }

    @FXML
    private void handleNextTicket() {
        if (currentReservation == null || currentSubTicketIndex >= currentReservation.getNbPlaces())
            return;
        currentSubTicketIndex++;
        refreshSubTicket();
    }

    private void refreshSubTicket() {
        int total = currentReservation.getNbPlaces();
        if (lblPagination != null)
            lblPagination.setText("Billet " + currentSubTicketIndex + " / " + total);
        if (lblID != null)
            lblID.setText(String.format("#AFK-%04d-S%d", currentReservation.getId(), currentSubTicketIndex));
        // Regenerer le QR code unique
        if (ivQRCode != null && ivQRCode.isVisible()) {
            try {
                String qrData = "AFKART-TICKET-" + currentReservation.getId()
                        + "-SEAT-" + currentSubTicketIndex
                        + "-USER-" + currentReservation.getUserId();
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + qrData;
                ivQRCode.setImage(new javafx.scene.image.Image(qrUrl, true));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // Mettre à jour les boutons prev/next
        if (btnPrevTicket != null)
            btnPrevTicket.setDisable(currentSubTicketIndex <= 1);
        if (btnNextTicket != null)
            btnNextTicket.setDisable(currentSubTicketIndex >= total);
    }

    @FXML
    private void handleFinaliserPaiement() {
        // Simulation de validation (comme dans le web)
        String holder = txtCardHolder != null ? txtCardHolder.getText() : "";
        String number = txtCardNumber != null ? txtCardNumber.getText().replace(" ", "") : "";
        String exp = txtCardExp != null ? txtCardExp.getText() : "";
        String cvc = txtCardCVC != null ? txtCardCVC.getText() : "";

        if (holder.isEmpty() || number.isEmpty() || exp.isEmpty() || cvc.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Veuillez remplir tous les champs de la carte bancaire.").show();
            return;
        }

        if (holder.length() < 3) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Le nom du titulaire doit contenir au moins 3 caractères.").show();
            return;
        }

        if (!number.matches("\\d{16}")) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Le numéro de carte doit contenir exactement 16 chiffres.").show();
            return;
        }

        if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "La date d'expiration doit être au format MM/AA avec un mois valide (01-12).").show();
            return;
        }

        if (!cvc.matches("\\d{3}")) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Le code CVC doit contenir exactement 3 chiffres.").show();
            return;
        }

        try {
            org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
            sr.mettreAJourStatut(currentReservation.getId(), org.example.services.ServiceReservation.STATUT_CONFIRME);
            currentReservation.setStatut(org.example.services.ServiceReservation.STATUT_CONFIRME);

            if (timeline != null)
                timeline.stop();
            updateStatusUI();

            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Paiement réussi ! Votre billet est maintenant disponible.").show();
        } catch (Exception e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Erreur lors du paiement : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleTelechargerBillet() {
        try {
            // Masquer le bouton de téléchargement pour qu'il n'apparaisse pas sur le PDF
            if (btnTelecharger != null)
                btnTelecharger.setVisible(false);

            // Création d'un instantané haute qualité du ticket visuel
            WritableImage fxImage = ticketCard.snapshot(new SnapshotParameters(), null);

            // Réafficher le bouton
            if (btnTelecharger != null)
                btnTelecharger.setVisible(true);

            java.awt.image.BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(awtImage, "png", baos);

            // Chemin d'exportation
            String userHome = System.getProperty("user.home");
            String fileName = userHome + "/Desktop/billet_afkart_" + currentReservation.getId() + ".pdf";

            // Génération du PDF avec iText
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(fileName));
            document.open();

            com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(baos.toByteArray());
            // Ajustement de l'image pour qu'elle tienne dans la largeur de la page A4
            // (moins les marges)
            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                    - document.rightMargin()) / pdfImage.getWidth()) * 100;
            pdfImage.scalePercent(scaler);
            pdfImage.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);

            document.add(pdfImage);
            document.close();

            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Billet PDF téléchargé avec succès sur votre Bureau !").show();
        } catch (Exception e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Erreur de téléchargement PDF : " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    private void startCountdown(java.time.LocalDateTime createdAt) {
        if (timeline != null)
            timeline.stop();

        java.time.LocalDateTime expirationTime = createdAt.plusHours(3);

        timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
                new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        java.time.Duration diff = java.time.Duration.between(java.time.LocalDateTime.now(),
                                expirationTime);

                        if (diff.isNegative() || diff.isZero()) {
                            if (lblExpiration != null) {
                                lblExpiration.setText("Billet Expiré");
                                lblExpiration.setStyle(
                                        "-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
                            }
                            if (btnFinaliser != null)
                                btnFinaliser.setDisable(true);
                            if (btnAnnuler != null)
                                btnAnnuler.setDisable(true);
                            if (lblActionRequise != null) {
                                lblActionRequise.setText("❌ BILLET EXPIRÉ");
                                lblActionRequise.setStyle(
                                        "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 10px; -fx-font-weight: bold;");
                            }

                            // Mise à jour base de données en temps réel (si expiré sous les yeux de
                            // l'utilisateur)
                            try {
                                org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
                                sr.mettreAJourStatut(currentReservation.getId(),
                                        org.example.services.ServiceReservation.STATUT_ANNULE);
                                currentReservation.setStatut(org.example.services.ServiceReservation.STATUT_ANNULE);
                            } catch (Exception ignored) {
                            }

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

    @FXML
    private void handleAnnulerReservation() {
        try {
            org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
            sr.mettreAJourStatut(currentReservation.getId(), org.example.services.ServiceReservation.STATUT_ANNULE);
            currentReservation.setStatut(org.example.services.ServiceReservation.STATUT_ANNULE);

            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "La réservation a été annulée avec succès.").showAndWait();

            // Fermer la fenêtre du ticket
            if (ticketCard != null && ticketCard.getScene() != null) {
                ((javafx.stage.Stage) ticketCard.getScene().getWindow()).close();
            }
        } catch (Exception e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Erreur lors de l'annulation : " + e.getMessage()).show();
        }
    }
}
