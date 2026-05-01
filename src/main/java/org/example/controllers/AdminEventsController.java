package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.example.models.Evenement;
import org.example.services.ServiceEvenement;

public class AdminEventsController {

    @FXML
    private Label lblTotalEvents, lblTotalCapacity, lblUpcoming;
    @FXML
    private ProgressBar pbActive, pbCapacity, pbUpcoming;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> comboSort, comboOrder;
    @FXML
    private VBox vboxEvents;

    private ServiceEvenement serviceEvenement = new ServiceEvenement();
    private ObservableList<Evenement> allEvents = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        comboSort.getItems().addAll("Date", "Nom", "Capacité", "Prix");
        comboSort.setValue("Date");
        comboOrder.getItems().addAll("ASC", "DESC");
        comboOrder.setValue("ASC");

        loadData();

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    public void loadData() {
        try {
            allEvents.setAll(serviceEvenement.listerTousParDateDebutAsc());
            loadStats();
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        try {
            int total = serviceEvenement.compterTotal();
            int capacity = serviceEvenement.sommeCapaciteTotale();

            lblTotalEvents.setText(String.valueOf(total));
            lblTotalCapacity.setText(String.valueOf(capacity));
            lblUpcoming.setText(String.valueOf(total));

            pbActive.setProgress(1.0);
            pbCapacity.setProgress(Math.min(1.0, (double) capacity / 1000.0));
            pbUpcoming.setProgress(0.5);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        vboxEvents.getChildren().clear();

        String query = tfSearch.getText().toLowerCase();
        String sortBy = comboSort.getValue();
        boolean asc = "ASC".equals(comboOrder.getValue());

        List<Evenement> filtered = allEvents.stream()
                .filter(e -> {
                    if (query.isEmpty())
                        return true;
                    return e.getNom().toLowerCase().contains(query) ||
                            e.getLieu().toLowerCase().contains(query);
                })
                .sorted((e1, e2) -> {
                    int res = 0;
                    switch (sortBy != null ? sortBy : "Date") {
                        case "Date":
                            res = e1.getDateDebut().compareTo(e2.getDateDebut());
                            break;
                        case "Nom":
                            res = e1.getNom().compareTo(e2.getNom());
                            break;
                        case "Capacité":
                            res = Integer.compare(e1.getCapacite(), e2.getCapacite());
                            break;
                        case "Prix":
                            res = Double.compare(e1.getPrix().doubleValue(), e2.getPrix().doubleValue());
                            break;
                    }
                    return asc ? res : -res;
                })
                .collect(Collectors.toList());

        for (Evenement e : filtered) {
            addEventRow(e);
        }
    }

    private void addEventRow(Evenement e) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-padding: 15 25; -fx-background-color: white; -fx-border-color: transparent transparent #ede5de transparent;");

        Label lblId = new Label("#" + e.getId());
        lblId.setPrefWidth(60);
        lblId.setMinWidth(Region.USE_PREF_SIZE);
        lblId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;");

        Label lblNom = new Label(e.getNom());
        lblNom.setPrefWidth(280);
        lblNom.setMinWidth(Region.USE_PREF_SIZE);
        lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;");

        Label lblDate = new Label(e.getDateDebut().format(formatter));
        lblDate.setPrefWidth(180);
        lblDate.setMinWidth(Region.USE_PREF_SIZE);
        lblDate.setStyle("-fx-text-fill: #6b5b4f;");

        Label lblLieu = new Label(e.getLieu());
        lblLieu.setPrefWidth(150);
        lblLieu.setMinWidth(Region.USE_PREF_SIZE);
        lblLieu.setStyle("-fx-text-fill: #9e8e82;");

        Label lblPrixLabel = new Label(String.format("%.2f DT", e.getPrix()));
        lblPrixLabel.setPrefWidth(100);
        lblPrixLabel.setMinWidth(Region.USE_PREF_SIZE);
        lblPrixLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c4956a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setPrefWidth(150);
        actions.setMinWidth(Region.USE_PREF_SIZE);

        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b5e4a; -fx-cursor: hand;");
        btnEdit.setOnAction(ev -> handleEditEvent(e));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #c62828; -fx-cursor: hand;");
        btnDelete.setOnAction(ev -> handleDeleteEvent(e));

        actions.getChildren().addAll(btnEdit, btnDelete);

        row.getChildren().addAll(lblId, lblNom, lblDate, lblLieu, lblPrixLabel, spacer, actions);
        vboxEvents.getChildren().add(row);
    }

    @FXML
    private void handleNewEvent(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AjouterEvenement.fxml"));
            vboxEvents.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleEditEvent(Evenement ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierEvenement.fxml"));
            Parent root = loader.load();
            vboxEvents.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleDeleteEvent(Evenement ev) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'événement '" + ev.getNom() + "' ?",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    serviceEvenement.supprimer(ev.getId());
                    loadData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
