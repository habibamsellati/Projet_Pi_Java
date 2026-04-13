package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.example.models.Commande;
import org.example.services.ServiceCommande;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminCommandesController {

    @FXML private Label lblStatTotal, lblStatConfirmed, lblStatPending, lblStatRevenue;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private TableView<Commande> tvCommandes;
    @FXML private TableColumn<Commande, String> colNumero, colDate, colStatut;
    @FXML private TableColumn<Commande, Integer> colClient;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, String> colAdresse, colActions;

    private ObservableList<Commande> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
        setupFilters();
    }

    private void setupTable() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresseLivraison"));

        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Commande c = getTableRow().getItem();
                    if (c.getDateCommande() != null) {
                        setText(c.getDateCommande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    } else {
                        setText("-");
                    }
                }
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Commande c = getTableRow().getItem();
                    Label badge = new Label(c.getStatut());
                    badge.getStyleClass().add("badge");
                    
                    if (ServiceCommande.STATUT_CONFIRMEE.equals(c.getStatut())) {
                        badge.getStyleClass().add("badge-confirmed");
                    } else {
                        badge.getStyleClass().add("badge-pending");
                    }
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Commande, String> call(TableColumn<Commande, String> param) {
                return new TableCell<>() {
                    private final Button btnV = new Button("✓");
                    private final Button btnX = new Button("✕");
                    private final HBox container = new HBox(8, btnV, btnX);

                    {
                        btnV.getStyleClass().add("btn-action-v");
                        btnX.getStyleClass().add("btn-action-x");
                        
                        btnV.setOnAction(e -> handleUpdateStatus(getTableRow().getItem(), ServiceCommande.STATUT_CONFIRMEE));
                        btnX.setOnAction(e -> handleUpdateStatus(getTableRow().getItem(), ServiceCommande.STATUT_ANNULEE));
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) setGraphic(null);
                        else setGraphic(container);
                    }
                };
            }
        });
    }

    private void loadData() {
        try {
            ServiceCommande sc = new ServiceCommande();
            List<Commande> list = sc.afficherToutesLesCommandes();
            masterData.setAll(list);
            tvCommandes.setItems(masterData);
            
            updateStats(sc);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStats(ServiceCommande sc) throws SQLException {
        lblStatTotal.setText(String.valueOf(sc.compterTotal()));
        lblStatConfirmed.setText(String.valueOf(sc.compterParStatut(ServiceCommande.STATUT_CONFIRMEE)));
        lblStatPending.setText(String.valueOf(sc.compterParStatut(ServiceCommande.STATUT_EN_ATTENTE)));
        lblStatRevenue.setText(String.format("%.2f DT", sc.getChiffreAffairesTotal()));
    }

    private void setupFilters() {
        cbFilterStatut.setItems(FXCollections.observableArrayList("Tous", ServiceCommande.STATUT_EN_ATTENTE, ServiceCommande.STATUT_CONFIRMEE, ServiceCommande.STATUT_ANNULEE));
        cbFilterStatut.setValue("Tous");

        FilteredList<Commande> filteredData = new FilteredList<>(masterData, p -> true);
        
        tfSearch.textProperty().addListener((obs, oldV, newV) -> updateFilter(filteredData));
        cbFilterStatut.valueProperty().addListener((obs, oldV, newV) -> updateFilter(filteredData));
        
        tvCommandes.setItems(filteredData);
    }

    private void updateFilter(FilteredList<Commande> filteredData) {
        String searchText = tfSearch.getText().toLowerCase();
        String statusFilter = cbFilterStatut.getValue();

        filteredData.setPredicate(c -> {
            boolean matchSearch = searchText.isEmpty() || 
                                  (c.getNumero() != null && c.getNumero().toLowerCase().contains(searchText)) ||
                                  String.valueOf(c.getUserId()).contains(searchText);
            
            boolean matchStatus = statusFilter.equals("Tous") || statusFilter.equals(c.getStatut());
            
            return matchSearch && matchStatus;
        });
    }

    private void handleUpdateStatus(Commande c, String newStatus) {
        if (c == null) return;
        try {
            ServiceCommande sc = new ServiceCommande();
            sc.mettreAJourStatut(c.getId(), newStatus);
            loadData(); // Refresh everything
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour : " + e.getMessage()).show();
        }
    }
}
