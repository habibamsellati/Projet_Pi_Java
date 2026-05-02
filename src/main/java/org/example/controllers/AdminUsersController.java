package org.example.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.models.UserAccount;
import org.example.services.UserManagementService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminUsersController {

    @FXML private TextField tfSearch;
    @FXML private TableView<UserAccount> tableUsers;
    @FXML private TableColumn<UserAccount, String> colId;
    @FXML private TableColumn<UserAccount, String> colNom;
    @FXML private TableColumn<UserAccount, String> colPrenom;
    @FXML private TableColumn<UserAccount, String> colEmail;
    @FXML private TableColumn<UserAccount, String> colRole;
    @FXML private TableColumn<UserAccount, String> colStatut;
    @FXML private TableColumn<UserAccount, String> colDateCreation;
    @FXML private Button btnDesactiver;

    private final UserManagementService userService = new UserManagementService();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNom())));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPrenom())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getEmail())));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole() == null ? "" : c.getValue().getRole().name()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getStatut())));
        colDateCreation.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCreation() == null
                        ? "-"
                        : c.getValue().getDateCreation().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FMT)
        ));

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                btnDesactiver.setDisable(newV == null));

        chargerUsers();
    }

    @FXML
    void handleSearch() {
        chargerUsers();
    }

    @FXML
    void handleRefresh() {
        tfSearch.clear();
        chargerUsers();
    }

    @FXML
    void handleDesactiver() {
        UserAccount selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            userService.desactiverUtilisateur(selected.getId());
            chargerUsers();
            new Alert(Alert.AlertType.INFORMATION, "Utilisateur desactive avec succes.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur desactivation: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    void handleCreerUtilisateur() {
        try {
            javafx.stage.Stage stage = new javafx.stage.Stage();
            org.example.utils.SceneNavigator.navigate(stage, "/fxml/Signup.fxml", "Inscription", 650, 560);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void chargerUsers() {
        try {
            List<UserAccount> users = userService.listerUtilisateurs(tfSearch.getText());
            tableUsers.setItems(FXCollections.observableArrayList(users));
            btnDesactiver.setDisable(tableUsers.getSelectionModel().getSelectedItem() == null);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement utilisateurs: " + e.getMessage()).showAndWait();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

