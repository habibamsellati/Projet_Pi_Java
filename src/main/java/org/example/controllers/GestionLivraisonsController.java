package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.Livraison;
import org.example.models.Role;
import org.example.models.User;
import org.example.services.ServiceLivraison;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GestionLivraisonsController {

    @FXML private Label lblTotal;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblLivrees;

    @FXML private TextField tfRecherche;
    @FXML private TableView<Livraison> tableLivraisons;
    @FXML private TableColumn<Livraison, String> colDate;
    @FXML private TableColumn<Livraison, String> colAdresse;
    @FXML private TableColumn<Livraison, String> colStatut;
    @FXML private TableColumn<Livraison, Void> colActions;

    @FXML private Button btnAjouterLivraison;
    @FXML private Button btnExporter;

    private final ServiceLivraison service = new ServiceLivraison();
    private final ObservableList<Livraison> data = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    void initialize() {
        if (!checkAdminAccess()) {
            return;
        }

        colAdresse.setCellValueFactory(cell -> {
            String adresse = cell.getValue().getAdresseLivraison();
            return new javafx.beans.property.SimpleStringProperty(adresse == null ? "-" : "📍 " + adresse);
        });
        colDate.setCellValueFactory(cell -> {
            Livraison l = cell.getValue();
            String text = l.getCreatedAt() == null
                    ? "-"
                    : l.getCreatedAt().toLocalDateTime().format(dateFormatter);
            return new javafx.beans.property.SimpleStringProperty(text);
        });
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); return; }
                javafx.scene.control.Label badge = new javafx.scene.control.Label();
                String s = statut.toLowerCase();
                if (s.contains("livree") || s.contains("livre")) {
                    badge.setText("✓ Livré");
                    badge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#065f46; -fx-background-radius:20; -fx-padding: 4 10; -fx-font-size:11; -fx-font-weight:bold;");
                } else if (s.contains("attente")) {
                    badge.setText("⏳ En attente");
                    badge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#92400e; -fx-background-radius:20; -fx-padding: 4 10; -fx-font-size:11; -fx-font-weight:bold;");
                } else if (s.contains("cours")) {
                    badge.setText("🔄 En cours");
                    badge.setStyle("-fx-background-color:#dbeafe; -fx-text-fill:#1e40af; -fx-background-radius:20; -fx-padding: 4 10; -fx-font-size:11; -fx-font-weight:bold;");
                } else {
                    badge.setText(statut);
                    badge.setStyle("-fx-background-color:#f3f4f6; -fx-text-fill:#555; -fx-background-radius:20; -fx-padding: 4 10; -fx-font-size:11;");
                }
                setGraphic(badge);
            }
        });

        setupActionsColumn();

        tableLivraisons.setItems(data);
        tfRecherche.textProperty().addListener((obs, oldVal, value) -> rechercher());
        chargerDonnees();
    }

    @FXML
    void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterLivraison.fxml"));
            Parent root = loader.load();
            AjouterLivraisonController controller = loader.getController();
            controller.setOnSaved(this::chargerDonnees);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter livraison");
            stage.setScene(new Scene(root, 1050, 720));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ajout livraison", e.getMessage());
        }
    }

    @FXML
    void handleExportPdf() {
        showInfo("Export PDF", "Export PDF prete: connectez votre service PDF pour finaliser.");
    }


    @FXML
    void handleReinitialiser() {
        tfRecherche.clear();
        tableLivraisons.getSelectionModel().clearSelection();
        chargerDonnees();
    }

    @FXML
    void handleRafraichir() {
        chargerDonnees();
    }

    private void rechercher() {
        try {
            List<Livraison> list = service.rechercher(tfRecherche.getText());
            data.setAll(list);
            refreshStats(list);
        } catch (SQLException e) {
            showError("Recherche", e.getMessage());
        }
    }

    private void chargerDonnees() {
        try {
            List<Livraison> list = service.afficherTout();
            data.setAll(list);
            refreshStats(list);
        } catch (SQLException e) {
            showError("Chargement", e.getMessage());
        }
    }

    private void refreshStats(List<Livraison> list) {
        int total = list.size();
        int enAttente = 0;
        int enCours = 0;
        int livrees = 0;

        for (Livraison l : list) {
            String s = l.getStatut();
            if ("en_attente".equals(s) || "en_preparation".equals(s)) {
                enAttente++;
            }
            if ("en_cours".equals(s)) {
                enCours++;
            }
            if ("livree".equals(s)) {
                livrees++;
            }
        }

        lblTotal.setText(String.valueOf(total));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblEnCours.setText(String.valueOf(enCours));
        lblLivrees.setText(String.valueOf(livrees));
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Corbeille");
            private final HBox box = new HBox(8, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:6; -fx-cursor:hand; -fx-font-size:11; -fx-padding: 4 10;");
                btnSupprimer.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:6; -fx-cursor:hand; -fx-font-size:11; -fx-padding: 4 10;");

                btnModifier.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void handleEdit(Livraison livraison) {
        if (livraison == null) {
            return;
        }

        List<String> statuts = new ArrayList<>(List.of("en_attente", "en_preparation", "en_cours", "livree", "retournee", "annulee"));
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(livraison.getStatut(), statuts);
        dialog.setTitle("Modifier statut");
        dialog.setHeaderText("Livraison #" + livraison.getId());
        dialog.setContentText("Nouveau statut:");
        dialog.showAndWait().ifPresent(newStatut -> {
            try {
                service.updateStatut(livraison.getId(), newStatut);
                chargerDonnees();
            } catch (SQLException e) {
                showError("Modification", e.getMessage());
            }
        });
    }

    private void handleDelete(Livraison livraison) {
        if (livraison == null) {
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la livraison #" + livraison.getId() + " ?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                try {
                    service.supprimer(livraison.getId());
                    chargerDonnees();
                } catch (SQLException e) {
                    showError("Suppression", e.getMessage());
                }
            }
        });
    }

    private boolean checkAdminAccess() {
        User user = SessionManager.getCurrentUser();
        boolean allowed = user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.RESPONSABLE);
        if (!allowed) {
            showError("Acces refuse", "La gestion des livraisons est reservee a l'administration.");
            disableAll();
        }
        return allowed;
    }

    private void disableAll() {
        btnAjouterLivraison.setDisable(true);
        btnExporter.setDisable(true);
        tableLivraisons.setDisable(true);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

