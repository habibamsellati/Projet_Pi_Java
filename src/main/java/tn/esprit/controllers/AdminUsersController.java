package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, String> prenomColumn;

    @FXML
    private TableColumn<User, String> nomColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> statutColumn;

    @FXML
    private TableColumn<User, String> dateColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        prenomColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getPrenom())));
        nomColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getNom())));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getEmail())));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getRole())));
        statutColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatut())));

        dateColumn.setCellValueFactory(data -> {
            if (data.getValue().getDateCreation() != null) {
                return new SimpleStringProperty(
                        new SimpleDateFormat("dd/MM/yyyy HH:mm").format(data.getValue().getDateCreation())
                );
            }
            return new SimpleStringProperty("-");
        });

        addActionsColumn();
    }

    public void setUser(User user) {
        this.currentUser = user;

        if (user != null) {
            String fullName = (safe(user.getNom()) + " " + safe(user.getPrenom())).trim();
            if (fullName.isEmpty()) {
                fullName = safe(user.getEmail());
            }

            userNameLabel.setText(fullName);
            userRoleLabel.setText(safe(user.getRole()).toUpperCase());
        }

        loadUsers();
    }

    private void loadUsers() {
        List<User> users = userService.getAllUsers();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<User> users = userService.getAllUsers();

        if (!keyword.isEmpty()) {
            users = users.stream().filter(u ->
                    safe(u.getNom()).toLowerCase().contains(keyword) ||
                            safe(u.getPrenom()).toLowerCase().contains(keyword) ||
                            safe(u.getEmail()).toLowerCase().contains(keyword) ||
                            safe(u.getRole()).toLowerCase().contains(keyword) ||
                            safe(u.getStatut()).toLowerCase().contains(keyword)
            ).collect(Collectors.toList());
        }

        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    private void addActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox box = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("table-edit-btn");
                deleteBtn.getStyleClass().add("table-delete-btn");

                editBtn.setOnAction(event -> {
                    User selectedUser = getTableView().getItems().get(getIndex());
                    openEditPage(selectedUser);
                });

                deleteBtn.setOnAction(event -> {
                    User selectedUser = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Supprimer l'utilisateur");
                    confirm.setContentText("Voulez-vous vraiment supprimer cet utilisateur ?");

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        userService.deleteUser(selectedUser.getId());
                        loadUsers();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void openEditPage(User userToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-user-edit.fxml"));
            Parent root = loader.load();

            AdminUserEditController controller = loader.getController();
            controller.setContext(currentUser, userToEdit);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture page modification : " + e.getMessage());
        }
    }

    @FXML
    public void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture dashboard : " + e.getMessage());
        }
    }

    @FXML
    public void goToUsers() {
        // déjà sur la page
    }

    @FXML
    public void goToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/front-home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture front : " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        goToDashboard();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void goToAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-user-add.fxml"));
            Parent root = loader.load();

            AdminUserAddController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture ajout utilisateur : " + e.getMessage());
        }
    }
}