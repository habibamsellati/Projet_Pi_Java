package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.example.models.Evenement;
import org.example.models.Reservation;
import org.example.services.ServiceEvenement;
import org.example.services.ServiceReservation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReservationsController {

    @FXML private TableView<Reservation> tvReservations;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, String> colEventName, colUser, colStatut;
    @FXML private TableColumn<Reservation, LocalDateTime> colDate;
    @FXML private TableColumn<Reservation, Integer> colPlaces;
    @FXML private TableColumn<Reservation, String> colTotal;
    @FXML private TableColumn<Reservation, Void> colActions;
    @FXML private TextField tfSearch;

    private ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private ServiceReservation service = new ServiceReservation();
    private Map<Integer, Evenement> evenementMap = new HashMap<>();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        tvReservations.setFixedCellSize(60.0);
        tvReservations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // -- ID --
        colId.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<Reservation, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText("RESV-" + item); setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;"); }
            }
        });

        // -- Nom Événement --
        colEventName.setCellValueFactory(cellData -> {
            int eid = cellData.getValue().getEvenementId();
            Evenement ev = evenementMap.get(eid);
            return new SimpleStringProperty(ev != null ? ev.getNom() : "ID: " + eid);
        });
        colEventName.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;"); }
            }
        });

        // -- Client --
        colUser.setCellValueFactory(cellData -> {
            Integer uid = cellData.getValue().getUserId();
            return new SimpleStringProperty(uid != null ? "Client #" + uid : "Anonyme");
        });
        colUser.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #6b5b4f;"); }
            }
        });

        // -- Date --
        colDate.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getDateReservation()));
        colDate.setCellFactory(col -> new TableCell<Reservation, LocalDateTime>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else {
                    setText(item.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px;");
                }
            }
        });

        // -- Places --
        colPlaces.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getNbPlaces()));
        colPlaces.setCellFactory(col -> new TableCell<Reservation, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item + " places"); setStyle("-fx-text-fill: #6b5b4f; -fx-font-weight: bold;"); }
            }
        });

        // -- Total --
        colTotal.setCellValueFactory(cellData -> {
            Reservation r = cellData.getValue();
            Evenement ev = evenementMap.get(r.getEvenementId());
            double prix = (ev != null && ev.getPrix() != null) ? ev.getPrix().doubleValue() : 0.0;
            return new SimpleStringProperty(String.format("%.2f DT", r.getNbPlaces() * prix));
        });
        colTotal.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #c4956a; -fx-font-size: 13px;"); }
            }
        });

        // -- Statut --
        colStatut.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.toUpperCase());
                badge.getStyleClass().add("badge");
                if ("confirme".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-confirmed");
                else if ("annule".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-cancelled");
                else badge.getStyleClass().add("badge-pending");
                setText(null);
                setGraphic(badge);
            }
        });

        // -- Actions --
        colActions.setCellFactory(col -> new TableCell<Reservation, Void>() {
            private final Button btnCheck  = new Button("✔  Valider");
            private final Button btnCancel = new Button("✘  Annuler");
            private final HBox container   = new HBox(8, btnCheck, btnCancel);
            {
                btnCheck.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; -fx-background-radius: 15; -fx-border-color: #bbf7d0; -fx-border-radius: 15; -fx-font-size: 10px; -fx-padding: 5 12; -fx-cursor: hand;");
                btnCancel.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #e53e3e; -fx-background-radius: 15; -fx-border-color: #fecaca; -fx-border-radius: 15; -fx-font-size: 10px; -fx-padding: 5 12; -fx-cursor: hand;");
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                btnCheck.setOnAction(e -> handleUpdateStatut(getTableView().getItems().get(getIndex()), "confirme"));
                btnCancel.setOnAction(e -> handleUpdateStatut(getTableView().getItems().get(getIndex()), "annule"));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(empty ? null : container);
            }
        });
    }

    @FXML
    void loadData() {
        try {
            // Load events first for mapping
            ServiceEvenement se = new ServiceEvenement();
            List<Evenement> evs = se.listerTousParDateDebutAsc();
            evenementMap.clear();
            for (Evenement e : evs) evenementMap.put(e.getId(), e);

            // Load reservations
            List<Reservation> list = service.listerTous();
            reservations.setAll(list);
            
            final FilteredList<Reservation> filteredData = new FilteredList<Reservation>(reservations);
            tfSearch.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    final String filter = (newValue == null) ? "" : newValue.toLowerCase();
                    filteredData.setPredicate(new java.util.function.Predicate<Reservation>() {
                        @Override
                        public boolean test(final Reservation res) {
                            if (filter.isEmpty()) return true;
                            
                            Evenement ev = evenementMap.get(res.getEvenementId());
                            if (ev != null && ev.getNom().toLowerCase().contains(filter)) return true;
                            if (String.valueOf(res.getId()).contains(filter)) return true;
                            return false;
                        }
                    });
                }
            });
            tvReservations.setItems(filteredData);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleUpdateStatut(Reservation r, String newStatut) {
        try {
            service.mettreAJourStatut(r.getId(), newStatut);
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour: " + e.getMessage()).show();
        }
    }
}
