package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import java.sql.SQLException;

public class AdminDashboardHomeController {

    @FXML
    private Label lblTotalEvents, lblTotalReservations, lblOccupationRate, lblTotalRevenue;
    @FXML
    private ProgressBar pbEvents, pbReservations, pbOccupation, pbRevenue;
    @FXML
    private VBox vboxRecentActivity, vboxTopEvents;
    @FXML
    private javafx.scene.chart.LineChart<String, Number> lineChart;
    @FXML
    private javafx.scene.chart.PieChart pieChart;

    private org.example.services.ServiceEvenement serviceEvenement = new org.example.services.ServiceEvenement();
    private org.example.services.ServiceReservation serviceReservation = new org.example.services.ServiceReservation();
    private org.example.services.ServiceUser serviceUser = new org.example.services.ServiceUser();

    @FXML
    public void initialize() {
        loadStats();
        loadRecentActivity();
        loadTopEvents();
        loadCharts();
    }

    private void loadCharts() {
        try {
            int clients = serviceUser.compterParRole("CLIENT");
            int artisans = serviceUser.compterParRole("ARTISANT");
            int admins = serviceUser.compterParRole("ADMIN");

            pieChart.getData().setAll(
                    new javafx.scene.chart.PieChart.Data("Clients", clients),
                    new javafx.scene.chart.PieChart.Data("Artisans", artisans),
                    new javafx.scene.chart.PieChart.Data("Admins", admins));
        } catch (Exception e) {
            pieChart.getData().setAll(
                    new javafx.scene.chart.PieChart.Data("Clients (Sim)", 700),
                    new javafx.scene.chart.PieChart.Data("Artisans (Sim)", 200),
                    new javafx.scene.chart.PieChart.Data("Admins (Sim)", 50));
        }

        javafx.scene.chart.XYChart.Series<String, Number> seriesUsers = new javafx.scene.chart.XYChart.Series<>();
        seriesUsers.setName("Inscriptions");
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Lun", 20));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Mar", 45));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Mer", 28));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Jeu", 55));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Ven", 42));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Sam", 12));
        seriesUsers.getData().add(new javafx.scene.chart.XYChart.Data<>("Dim", 8));

        javafx.scene.chart.XYChart.Series<String, Number> seriesReservations = new javafx.scene.chart.XYChart.Series<>();
        seriesReservations.setName("Réservations");
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Lun", 5));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Mar", 12));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Mer", 35));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Jeu", 60));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Ven", 95));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Sam", 120));
        seriesReservations.getData().add(new javafx.scene.chart.XYChart.Data<>("Dim", 140));

        lineChart.getData().setAll(seriesUsers, seriesReservations);
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
            int count = 0;
            for (org.example.models.Reservation r : recent) {
                if (count >= 5)
                    break;
                addActivityRow(r);
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addActivityRow(org.example.models.Reservation r) throws java.io.IOException, SQLException {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10; -fx-background-color: #faf7f5; -fx-background-radius: 8;");

        org.example.models.Evenement evt = serviceEvenement.trouverParId(r.getEvenementId());

        Label lblName = new Label(evt != null ? evt.getNom() : "Événement #" + r.getEvenementId());
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;");
        lblName.setPrefWidth(200);

        Label lblDate = new Label(r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "---");
        lblDate.setStyle("-fx-text-fill: #9e8e82;");
        lblDate.setPrefWidth(120);

        Label lblStatus = new Label(r.getStatut().toUpperCase());
        lblStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #8b5e4a;");

        row.getChildren().addAll(lblName, lblDate, lblStatus);
        vboxRecentActivity.getChildren().add(row);
    }

    private void loadTopEvents() {
        try {
            java.util.List<org.example.models.Evenement> all = serviceEvenement.listerTousParDateDebutAsc();
            int count = 0;
            for (org.example.models.Evenement e : all) {
                if (count >= 4)
                    break;
                addTopEventCard(e);
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTopEventCard(org.example.models.Evenement e) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(5);
        card.setStyle(
                "-fx-background-color: #white; -fx-padding: 12; -fx-background-radius: 10; -fx-border-color: #ede5de; -fx-border-radius: 10;");

        Label title = new Label(e.getNom().toUpperCase());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d; -fx-font-size: 12px;");

        Label meta = new Label(e.getLieu() + " • " + e.getCapacite() + " places");
        meta.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 10px;");

        card.getChildren().addAll(title, meta);
        vboxTopEvents.getChildren().add(card);
    }
}
