package tn.esprit.controllers;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.services.UserService;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
    private ComboBox<String> sortComboBox;

    @FXML
    private ListView<User> usersListView;

    @FXML
    private VBox tableHeaderContainer;

    private final UserService userService = new UserService();
    private User currentUser;
    private List<User> currentUsers = new ArrayList<>();

    private static final double COL_PRENOM = 120;
    private static final double COL_NOM = 120;
    private static final double COL_EMAIL = 250;
    private static final double COL_ROLE = 110;
    private static final double COL_STATUT = 110;
    private static final double COL_DATE = 120;
    private static final double COL_ACTIONS = 180;

    @FXML
    public void initialize() {
        buildHeader();

        sortComboBox.setItems(FXCollections.observableArrayList(
                "Prénom A-Z",
                "Nom A-Z",
                "Email A-Z",
                "Rôle A-Z",
                "Statut A-Z",
                "Date récente"
        ));

        usersListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createUserRow(user));
                }
            }
        });

        loadUsers();
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

    private void buildHeader() {
        tableHeaderContainer.getChildren().clear();

        GridPane header = new GridPane();
        header.getStyleClass().add("users-header-grid");
        header.setHgap(10);
        header.setPadding(new Insets(14, 18, 14, 18));

        ColumnConstraints c1 = new ColumnConstraints(COL_PRENOM);
        ColumnConstraints c2 = new ColumnConstraints(COL_NOM);
        ColumnConstraints c3 = new ColumnConstraints(COL_EMAIL);
        ColumnConstraints c4 = new ColumnConstraints(COL_ROLE);
        ColumnConstraints c5 = new ColumnConstraints(COL_STATUT);
        ColumnConstraints c6 = new ColumnConstraints(COL_DATE);
        ColumnConstraints c7 = new ColumnConstraints(COL_ACTIONS);

        c1.setHalignment(HPos.LEFT);
        c2.setHalignment(HPos.LEFT);
        c3.setHalignment(HPos.LEFT);
        c4.setHalignment(HPos.CENTER);
        c5.setHalignment(HPos.CENTER);
        c6.setHalignment(HPos.CENTER);
        c7.setHalignment(HPos.CENTER);

        header.getColumnConstraints().addAll(c1, c2, c3, c4, c5, c6, c7);

        addHeaderCell(header, "Prénom", 0, false);
        addHeaderCell(header, "Nom", 1, false);
        addHeaderCell(header, "Email", 2, false);
        addHeaderCell(header, "Rôle", 3, true);
        addHeaderCell(header, "Statut", 4, true);
        addHeaderCell(header, "Créé le", 5, true);
        addHeaderCell(header, "Actions", 6, true);

        tableHeaderContainer.getChildren().add(header);
    }

    private void loadUsers() {
        currentUsers = new ArrayList<>(userService.getAllUsers());
        refreshList(currentUsers);
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<User> filtered = userService.getAllUsers();

        if (!keyword.isEmpty()) {
            filtered = filtered.stream()
                    .filter(u ->
                            safe(u.getNom()).toLowerCase().contains(keyword) ||
                                    safe(u.getPrenom()).toLowerCase().contains(keyword) ||
                                    safe(u.getEmail()).toLowerCase().contains(keyword) ||
                                    safe(u.getRole()).toLowerCase().contains(keyword) ||
                                    safe(u.getStatut()).toLowerCase().contains(keyword)
                    )
                    .collect(Collectors.toList());
        }

        currentUsers = new ArrayList<>(filtered);
        refreshList(currentUsers);
    }

    @FXML
    public void handleSort() {
        refreshList(currentUsers);
    }

    @FXML
    public void handleExportPdf() {
        try {
            List<User> users = new ArrayList<>(usersListView.getItems());

            if (users.isEmpty()) {
                showInfo("Export PDF", "Aucun utilisateur à exporter.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fileChooser.setInitialFileName("rapport_utilisateurs.pdf");

            Stage stage = (Stage) usersListView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file == null) {
                return;
            }

            // ===== Statistiques =====
            int totalUsers = users.size();

            long totalAdmins = users.stream()
                    .filter(u -> safe(u.getRole()).equalsIgnoreCase("ADMIN"))
                    .count();

            long totalClients = users.stream()
                    .filter(u -> safe(u.getRole()).equalsIgnoreCase("CLIENT"))
                    .count();

            long totalArtisans = users.stream()
                    .filter(u -> safe(u.getRole()).equalsIgnoreCase("ARTISAN"))
                    .count();

            long totalLivreurs = users.stream()
                    .filter(u -> safe(u.getRole()).equalsIgnoreCase("LIVREUR"))
                    .count();

            long totalHommes = users.stream()
                    .filter(u -> safe(u.getSexe()).equalsIgnoreCase("Homme"))
                    .count();

            long totalFemmes = users.stream()
                    .filter(u -> safe(u.getSexe()).equalsIgnoreCase("Femme"))
                    .count();



            // ===== PDF =====
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 25, 25, 25, 25);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Polices
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font subTitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.ITALIC);
            com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

            // Titre
            Paragraph title = new Paragraph("Rapport des utilisateurs", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);

            SimpleDateFormat exportDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Paragraph exportDate = new Paragraph("Date d'export : " + exportDateFormat.format(new java.util.Date()), subTitleFont);
            exportDate.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(exportDate);

            document.add(new Paragraph(" "));

            // ===== Bloc statistiques =====
            Paragraph statsTitle = new Paragraph("Statistiques générales", sectionFont);
            statsTitle.setSpacingAfter(10f);
            document.add(statsTitle);

            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(55);
            statsTable.setSpacingAfter(20f);

            addStatsCell(statsTable, "Total utilisateurs", String.valueOf(totalUsers));
            addStatsCell(statsTable, "Admins", String.valueOf(totalAdmins));
            addStatsCell(statsTable, "Clients", String.valueOf(totalClients));
            addStatsCell(statsTable, "Artisans", String.valueOf(totalArtisans));
            addStatsCell(statsTable, "Livreurs", String.valueOf(totalLivreurs));
            addStatsCell(statsTable, "Hommes", String.valueOf(totalHommes));
            addStatsCell(statsTable, "Femmes", String.valueOf(totalFemmes));


            document.add(statsTable);

            // ===== Tableau utilisateurs =====
            Paragraph listTitle = new Paragraph("Liste détaillée des utilisateurs", sectionFont);
            listTitle.setSpacingAfter(10f);
            document.add(listTitle);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.2f, 2.2f, 4f, 2f, 2f, 2f, 2f});

            addHeaderCell(table, "Prénom", headerFont);
            addHeaderCell(table, "Nom", headerFont);
            addHeaderCell(table, "Email", headerFont);
            addHeaderCell(table, "Rôle", headerFont);
            addHeaderCell(table, "Sexe", headerFont);
            addHeaderCell(table, "Statut", headerFont);
            addHeaderCell(table, "Date", headerFont);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            boolean alternate = false;
            for (User user : users) {
                java.awt.Color bg = alternate ? new java.awt.Color(245, 245, 245) : java.awt.Color.WHITE;

                addBodyCell(table, safe(user.getPrenom()), normalFont, bg);
                addBodyCell(table, safe(user.getNom()), normalFont, bg);
                addBodyCell(table, safe(user.getEmail()), normalFont, bg);
                addBodyCell(table, safe(user.getRole()), normalFont, bg);
                addBodyCell(table, safe(user.getSexe()), normalFont, bg);
                addBodyCell(table, safe(user.getStatut()), normalFont, bg);
                addBodyCell(table, user.getDateCreation() != null ? sdf.format(user.getDateCreation()) : "-", normalFont, bg);

                alternate = !alternate;
            }

            document.add(table);

            document.close();

            showInfo("Export PDF", "PDF exporté avec succès.");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur export PDF : " + e.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new java.awt.Color(103, 173, 173));
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, java.awt.Color bgColor) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addStatsCell(PdfPTable table, String label, String value) {
        com.lowagie.text.Font labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL);

        com.lowagie.text.pdf.PdfPCell cell1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(label, labelFont));
        cell1.setPadding(7f);
        cell1.setBackgroundColor(new java.awt.Color(240, 248, 248));

        com.lowagie.text.pdf.PdfPCell cell2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(value, valueFont));
        cell2.setPadding(7f);

        table.addCell(cell1);
        table.addCell(cell2);
    }

    private void refreshList(List<User> users) {
        List<User> sorted = new ArrayList<>(users);

        String selectedSort = sortComboBox != null ? sortComboBox.getValue() : null;

        if (selectedSort != null) {
            switch (selectedSort) {
                case "Prénom A-Z":
                    sorted.sort(Comparator.comparing(u -> safe(u.getPrenom()).toLowerCase()));
                    break;
                case "Nom A-Z":
                    sorted.sort(Comparator.comparing(u -> safe(u.getNom()).toLowerCase()));
                    break;
                case "Email A-Z":
                    sorted.sort(Comparator.comparing(u -> safe(u.getEmail()).toLowerCase()));
                    break;
                case "Rôle A-Z":
                    sorted.sort(Comparator.comparing(u -> safe(u.getRole()).toLowerCase()));
                    break;
                case "Statut A-Z":
                    sorted.sort(Comparator.comparing(u -> safe(u.getStatut()).toLowerCase()));
                    break;
                case "Date récente":
                    sorted.sort((u1, u2) -> {
                        if (u1.getDateCreation() == null && u2.getDateCreation() == null) return 0;
                        if (u1.getDateCreation() == null) return 1;
                        if (u2.getDateCreation() == null) return -1;
                        return u2.getDateCreation().compareTo(u1.getDateCreation());
                    });
                    break;
            }
        }

        usersListView.setItems(FXCollections.observableArrayList(sorted));
    }

    private GridPane createUserRow(User user) {
        GridPane row = new GridPane();
        row.getStyleClass().add("user-row-grid");
        row.setHgap(10);
        row.setPadding(new Insets(14, 18, 14, 18));

        ColumnConstraints c1 = new ColumnConstraints(COL_PRENOM);
        ColumnConstraints c2 = new ColumnConstraints(COL_NOM);
        ColumnConstraints c3 = new ColumnConstraints(COL_EMAIL);
        ColumnConstraints c4 = new ColumnConstraints(COL_ROLE);
        ColumnConstraints c5 = new ColumnConstraints(COL_STATUT);
        ColumnConstraints c6 = new ColumnConstraints(COL_DATE);
        ColumnConstraints c7 = new ColumnConstraints(COL_ACTIONS);

        c1.setHalignment(HPos.LEFT);
        c2.setHalignment(HPos.LEFT);
        c3.setHalignment(HPos.LEFT);
        c4.setHalignment(HPos.CENTER);
        c5.setHalignment(HPos.CENTER);
        c6.setHalignment(HPos.CENTER);
        c7.setHalignment(HPos.CENTER);

        row.getColumnConstraints().addAll(c1, c2, c3, c4, c5, c6, c7);

        addTextCell(row, safe(user.getPrenom()), 0, false);
        addTextCell(row, safe(user.getNom()), 1, false);
        addTextCell(row, safe(user.getEmail()), 2, false);

        Label roleLabel = new Label(safe(user.getRole()).toUpperCase());
        roleLabel.getStyleClass().addAll("badge-label", getRoleBadgeClass(user.getRole()));
        HBox roleBox = new HBox(roleLabel);
        roleBox.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(roleBox, 3);
        row.getChildren().add(roleBox);

        Label statutLabel = new Label(safe(user.getStatut()));
        statutLabel.getStyleClass().addAll("badge-label", getStatutBadgeClass(user.getStatut()));
        HBox statutBox = new HBox(statutLabel);
        statutBox.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(statutBox, 4);
        row.getChildren().add(statutBox);

        String dateText = "-";
        if (user.getDateCreation() != null) {
            dateText = new SimpleDateFormat("dd/MM/yyyy").format(user.getDateCreation());
        }
        addTextCell(row, dateText, 5, true);

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("table-edit-btn");
        editBtn.setOnAction(e -> openEditPage(user));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("table-delete-btn");
        deleteBtn.setOnAction(e -> handleDeleteUser(user));

        HBox actionsBox = new HBox(10, editBtn, deleteBtn);
        actionsBox.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(actionsBox, 6);
        row.getChildren().add(actionsBox);

        return row;
    }

    private void addHeaderCell(GridPane grid, String text, int col, boolean centered) {
        Label label = new Label(text);
        label.getStyleClass().addAll("users-header-cell", headerColumnClass(col));

        HBox box = new HBox(label);
        box.setAlignment(centered ? Pos.CENTER : Pos.CENTER_LEFT);

        GridPane.setColumnIndex(box, col);
        grid.getChildren().add(box);
    }

    private void addTextCell(GridPane grid, String text, int col, boolean centered) {
        Label label = new Label(text);
        label.getStyleClass().add("user-text");

        HBox box = new HBox(label);
        box.setAlignment(centered ? Pos.CENTER : Pos.CENTER_LEFT);

        GridPane.setColumnIndex(box, col);
        grid.getChildren().add(box);
    }

    private String headerColumnClass(int col) {
        switch (col) {
            case 0: return "users-col-prenom";
            case 1: return "users-col-nom";
            case 2: return "users-col-email";
            case 3: return "users-col-role";
            case 4: return "users-col-statut";
            case 5: return "users-col-date";
            default: return "users-col-actions";
        }
    }

    private String getRoleBadgeClass(String role) {
        String r = safe(role).toUpperCase();
        switch (r) {
            case "ADMIN": return "badge-admin";
            case "CLIENT": return "badge-client";
            case "ARTISAN": return "badge-artisan";
            case "LIVREUR": return "badge-livreur";
            default: return "badge-default";
        }
    }

    private String getStatutBadgeClass(String statut) {
        String s = safe(statut).toLowerCase();
        switch (s) {
            case "actif": return "badge-active";
            case "inactif": return "badge-inactive";
            default: return "badge-default";
        }
    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Voulez-vous vraiment supprimer cet utilisateur ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            userService.deleteUser(user.getId());
            loadUsers();
        }
    }

    private void openEditPage(User userToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-user-edit.fxml"));
            Parent root = loader.load();

            AdminUserEditController controller = loader.getController();
            controller.setContext(currentUser, userToEdit);

            Stage stage = (Stage) usersListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture page modification : " + e.getMessage());
        }
    }

    @FXML
    public void goToAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-user-add.fxml"));
            Parent root = loader.load();

            AdminUserAddController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Stage stage = (Stage) usersListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture ajout utilisateur : " + e.getMessage());
        }
    }

    @FXML
    public void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin-dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) usersListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur ouverture dashboard : " + e.getMessage());
        }
    }

    @FXML
    public void goToUsers() {
    }

    @FXML
    public void goToFront() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/front-home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usersListView.getScene().getWindow();
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}