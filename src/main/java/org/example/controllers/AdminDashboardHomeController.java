package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import java.sql.SQLException;

public class AdminDashboardHomeController {

    @FXML private Label lblTotalEvents, lblTotalReservations, lblOccupationRate, lblTotalRevenue;
    @FXML private ProgressBar pbEvents, pbReservations, pbOccupation, pbRevenue;
    @FXML private VBox vboxRecentActivity, vboxTopEvents;

    private org.example.services.ServiceEvenement serviceEvenement = new org.example.services.ServiceEvenement();
    private org.example.services.ServiceReservation serviceReservation = new org.example.services.ServiceReservation();

    @FXML
    public void initialize() {
        loadStats();
        loadRecentActivity();
        loadTopEvents();
    }

    private void loadStats() {
        try {
            int totalEvents = serviceEvenement.compterTotal();
            int totalReservations = serviceReservation.compterTotal();
            int totalCapacity = serviceEvenement.sommeCapaciteTotale();
            int reservedPlaces = serviceReservation.sommePlacesReserveesTotales();
            double revenue = serviceReservation.calculerRevenuTotal();
            
            lblTotalEvents.setText(String.valueOf(totalEvents));
            lblTotalReservations.setText(String.valueOf(totalReservations));
            
            double occRate = totalCapacity > 0 ? (double) reservedPlaces / totalCapacity : 0.0;
            lblOccupationRate.setText(String.format("%.1f%%", occRate * 100));
            lblTotalRevenue.setText(String.format("%.2f DT", revenue));
            
            pbEvents.setProgress(Math.min(1.0, totalEvents / 50.0));
            pbReservations.setProgress(Math.min(1.0, totalReservations / 200.0));
            pbOccupation.setProgress(occRate);
            pbRevenue.setProgress(Math.min(1.0, revenue / 5000.0));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRecentActivity() {
        try {
            java.util.List<org.example.models.Reservation> recent = serviceReservation.listerTous();
            // Take the first 8 for the dashboard
            int count = 0;
            for (org.example.models.Reservation r : recent) {
                if (count >= 8) break;
                addActivityRow(r);
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addActivityRow(org.example.models.Reservation r) throws java.io.IOException, SQLException {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AdminDashboardRow.fxml"));
        javafx.scene.layout.HBox row = loader.load();

        Label lblClientName = (Label) row.lookup("#lblClientName");
        Label lblClientEmail = (Label) row.lookup("#lblClientEmail");
        Label lblEventTitle = (Label) row.lookup("#lblEventTitle");
        Label lblDate = (Label) row.lookup("#lblDate");
        Label lblPlaces = (Label) row.lookup("#lblPlaces");
        Label lblTotal = (Label) row.lookup("#lblTotal");
        Label lblStatus = (Label) row.lookup("#lblStatus");
        javafx.scene.layout.StackPane badgeContainer = (javafx.scene.layout.StackPane) row.lookup("#badgeContainer");

        org.example.models.Evenement evt = serviceEvenement.trouverParId(r.getEvenementId());
        
        lblClientName.setText(r.getUserId() != null ? "Client #" + r.getUserId() : "Anonyme");
        lblClientEmail.setText("client" + r.getId() + "@example.tn");
        lblEventTitle.setText(evt != null ? evt.getNom() : "Évènement inconnu");
        lblDate.setText(r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "Date inconnue");
        lblPlaces.setText(String.valueOf(r.getNbPlaces()));
        
        double price = evt != null && evt.getPrix() != null ? evt.getPrix().doubleValue() : 0.0;
        lblTotal.setText(String.format("%.2f DT", r.getNbPlaces() * price));
        
        String status = r.getStatut().toLowerCase();
        lblStatus.setText(status.toUpperCase());
        
        if (status.contains("confirme")) badgeContainer.getStyleClass().add("badge-confirmed");
        else if (status.contains("attente")) badgeContainer.getStyleClass().add("badge-pending");
        else badgeContainer.getStyleClass().add("badge-cancelled");

        vboxRecentActivity.getChildren().add(row);
    }

    private void loadTopEvents() {
        try {
            java.util.List<org.example.models.Evenement> all = serviceEvenement.listerTousParDateDebutAsc();
            // Top 3 events based on capacity/relevance
            int count = 0;
            for (org.example.models.Evenement e : all) {
                if (count >= 4) break;
                addTopEventCard(e);
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTopEventCard(org.example.models.Evenement e) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(6);
        card.setStyle("-fx-background-color: #faf7f5; -fx-padding: 14; -fx-background-radius: 12; -fx-border-color: #ede5de; -fx-border-radius: 12; -fx-border-width: 1;");

        Label title = new Label(e.getNom().toUpperCase());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d; -fx-font-size: 13px;");

        Label meta = new Label(e.getLieu() + "  •  " + e.getCapacite() + " places");
        meta.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px;");

        ProgressBar pb = new ProgressBar(0.65);
        pb.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(title, meta, pb);
        vboxTopEvents.getChildren().add(card);
    }
}
