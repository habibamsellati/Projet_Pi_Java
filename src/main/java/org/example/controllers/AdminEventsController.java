package org.example.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.example.models.Evenement;
import org.example.services.ServiceEvenement;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class AdminEventsController {

    @FXML private TableView<Evenement> tvEvenements;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String> colNom, colLieu, colStatut;
    @FXML private TableColumn<Evenement, LocalDateTime> colDate;
    @FXML private TableColumn<Evenement, Integer> colCapacite;
    @FXML private TableColumn<Evenement, Double> colPrix;
    @FXML private TableColumn<Evenement, Void> colActions;
    @FXML private TextField tfSearch;

    private ObservableList<Evenement> evenements = FXCollections.observableArrayList();
    private ServiceEvenement service = new ServiceEvenement();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        tvEvenements.setFixedCellSize(60.0);
        tvEvenements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // -- ID --
        colId.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<Evenement, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else {
                    setText("EVT-" + item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;");
                }
            }
        });

        // -- Nom --
        colNom.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getNom()));
        colNom.setCellFactory(col -> new TableCell<Evenement, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #2d2d2d;"); }
            }
        });

        // -- Date --
        colDate.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getDateDebut()));
        colDate.setCellFactory(col -> new TableCell<Evenement, LocalDateTime>() {
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

        // -- Lieu --
        colLieu.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getLieu()));
        colLieu.setCellFactory(col -> new TableCell<Evenement, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #6b5b4f;"); }
            }
        });

        // -- Capacité --
        colCapacite.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(p.getValue().getCapacite()));
        colCapacite.setCellFactory(col -> new TableCell<Evenement, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else { setText(item + " pers."); setStyle("-fx-text-fill: #6b5b4f;"); }
            }
        });

        // -- Prix --
        colPrix.setCellValueFactory(p -> {
            double pr = p.getValue().getPrix() != null ? p.getValue().getPrix().doubleValue() : 0.0;
            return new javafx.beans.property.SimpleObjectProperty<>(pr);
        });
        colPrix.setCellFactory(col -> new TableCell<Evenement, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setText(null); }
                else {
                    setText(String.format("%.2f DT", item));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #c4956a; -fx-font-size: 13px;");
                }
            }
        });

        // -- Statut --
        colStatut.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<Evenement, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.toUpperCase());
                badge.getStyleClass().add("badge");
                if ("publie".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-confirmed");
                else if ("brouillon".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-pending");
                else badge.getStyleClass().add("badge-cancelled");
                setText(null);
                setGraphic(badge);
            }
        });

        // -- Actions --
        colActions.setCellFactory(col -> new TableCell<Evenement, Void>() {
            private final Button btnEdit   = new Button("✎  Modifier");
            private final Button btnDelete = new Button("✘  Suppr.");
            private final HBox container   = new HBox(8, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-size: 10px; -fx-padding: 5 12; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #e53e3e; -fx-background-radius: 15; -fx-border-color: #fecaca; -fx-border-radius: 15; -fx-font-size: 10px; -fx-padding: 5 12; -fx-cursor: hand;");
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                btnEdit.setOnAction(e -> handleEditEvent(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteEvent(getTableView().getItems().get(getIndex())));
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
            List<Evenement> list = service.listerTousParDateDebutDesc();
            evenements.setAll(list);
            
            final FilteredList<Evenement> filteredData = new FilteredList<Evenement>(evenements);
            tfSearch.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    final String filter = (newValue == null) ? "" : newValue.toLowerCase();
                    filteredData.setPredicate(new java.util.function.Predicate<Evenement>() {
                        @Override
                        public boolean test(Evenement ev) {
                            if (filter.isEmpty()) return true;
                            if (ev.getNom().toLowerCase().contains(filter)) return true;
                            if (ev.getLieu().toLowerCase().contains(filter)) return true;
                            return false;
                        }
                    });
                }
            });
            tvEvenements.setItems(filteredData);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleNewEvent(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterEvenement.fxml"));
            Parent root = loader.load();
            tvEvenements.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleEditEvent(Evenement ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterEvenement.fxml"));
            Parent root = loader.load();
            AjouterEvenementController controller = loader.getController();
            controller.setEvenementToEdit(ev);
            tvEvenements.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDeleteEvent(Evenement ev) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'événement '" + ev.getNom() + "' ?", ButtonType.YES, ButtonType.NO);
        final Evenement toDelete = ev;
        alert.showAndWait().ifPresent(new java.util.function.Consumer<ButtonType>() {
            @Override
            public void accept(ButtonType response) {
                if (response == ButtonType.YES) {
                    try {
                        service.supprimer(toDelete.getId());
                        loadData();
                    } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        });
    }
}
