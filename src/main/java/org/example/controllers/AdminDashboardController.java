package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.models.User;
import org.example.utils.MyDatabase;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    // ── Topbar ──
    @FXML private Label lblBienvenue;
    @FXML private Label lblAvatar;
    @FXML private Label lblNomAdmin;

    // ── Zone de contenu central (pour charger les sous-vues) ──
    @FXML private ScrollPane scrollContent;
    @FXML private javafx.scene.layout.VBox mainVBox;
    @FXML private javafx.scene.layout.VBox dashboardContent;
    @FXML private Label topbarTitle;

    // ── Boutons sidebar navigation ──
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavUsers;
    @FXML private Button btnNavProduits;
    @FXML private Button btnNavPropositions;
    @FXML private Button btnNavReclamations;
    @FXML private Button btnNavArticles;
    @FXML private Button btnNavCommandes;
    @FXML private Button btnNavLivraisons;
    @FXML private Button btnNavSuiviLivraisons;
    @FXML private Button btnNavEvenements;
    @FXML private Button btnNavReservations;

    // ── Stat cards ──
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalOeuvres;
    @FXML private Label lblTotalArtisans;
    @FXML private Label lblTotalReclamations;
    @FXML private Label badgeUsers;
    @FXML private Label badgeOeuvres;
    @FXML private Label badgeArtisans;
    @FXML private Label badgeReclamations;
    // Stats commandes – labels optionnels dans le dashboard
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblChiffreAffaires;

    // ── Line chart ──
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis lineXAxis;
    @FXML private NumberAxis lineYAxis;
    @FXML private Button btn7j;
    @FXML private Button btn30j;
    @FXML private Button btn12m;

    // ── Donut ──
    @FXML private StackPane pieChartContainer;
    @FXML private Label lblTotalRoles;
    @FXML private Label lblPctClients;
    @FXML private Label lblPctArtisans;
    @FXML private Label lblPctAdmins;

    // ── Tables ──
    @FXML private VBox recentUsersContainer;
    @FXML private VBox recentActivityContainer;

    // Datasets pour le graphique d'activité
    private static final String[] LABELS_7J  = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
    private static final String[] LABELS_30J = {"Semaine 1","Semaine 2","Semaine 3","Semaine 4"};
    private static final String[] LABELS_12M = {"Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc"};

    private static final Number[] USERS_7J   = {42,58,63,71,80,55,60};
    private static final Number[] USERS_30J  = {320,410,490,560};
    private static final Number[] USERS_12M  = {200,280,310,350,420,480,530,490,560,610,700,875};

    private static final Number[] OEUVRES_7J  = {18,22,28,31,26,19,24};
    private static final Number[] OEUVRES_30J = {180,220,270,310};
    private static final Number[] OEUVRES_12M = {80,110,130,150,160,180,190,175,195,200,210,214};

    @FXML
    void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String nom = user.getNomComplet();
            lblBienvenue.setText("Bienvenue, " + nom);
            lblNomAdmin.setText(nom);
            String initiales = buildInitiales(nom);
            lblAvatar.setText(initiales);
        }

        loadStats();
        setupLineChart(LABELS_7J, USERS_7J, OEUVRES_7J);
        setupDonutChart();
        loadRecentUsers();
        loadRecentActivity();
    }

    // ══════════════ Stats ══════════════

    private void loadStats() {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn == null) {
            lblTotalUsers.setText("—");
            lblTotalOeuvres.setText("—");
            lblTotalArtisans.setText("—");
            lblTotalReclamations.setText("—");
            return;
        }
        try {
            int totalUsers       = queryCount(conn, "SELECT COUNT(*) FROM `user` WHERE deleted_at IS NULL");
            int totalOeuvres     = queryCount(conn, "SELECT COUNT(*) FROM `article`");
            int totalArtisans    = queryCount(conn, "SELECT COUNT(*) FROM `user` WHERE role='ARTISANT' AND deleted_at IS NULL");
            int totalReclamations = queryCount(conn, "SELECT COUNT(*) FROM `reclamation`");

            lblTotalUsers.setText(String.valueOf(totalUsers));
            lblTotalOeuvres.setText(String.valueOf(totalOeuvres));
            lblTotalArtisans.setText(String.valueOf(totalArtisans));
            lblTotalReclamations.setText(String.valueOf(totalReclamations));

            badgeUsers.setText("↗ +12.5%");
            badgeOeuvres.setText("↗ +8.2%");
            badgeArtisans.setText("↗ +5.7%");
            badgeReclamations.setText("↘ -3.1%");

            // Stats commandes (labels optionnels)
            if (lblTotalCommandes != null) {
                int totalCommandes = queryCount(conn, "SELECT COUNT(*) FROM `commande`");
                lblTotalCommandes.setText(String.valueOf(totalCommandes));
            }
            if (lblChiffreAffaires != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(SUM(total),0) FROM commande WHERE statut='confirmee'");
                     ResultSet rs = ps.executeQuery()) {
                    double ca = rs.next() ? rs.getDouble(1) : 0.0;
                    lblChiffreAffaires.setText(String.format("%.2f DT", ca));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement stats : " + e.getMessage());
        }
    }

    private int queryCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ══════════════ Line Chart ══════════════

    private void setupLineChart(String[] labels, Number[] usersData, Number[] oeuvresData) {
        lineChart.getData().clear();
        lineXAxis.getCategories().clear();
        lineXAxis.setAnimated(false);
        lineYAxis.setAnimated(false);
        lineYAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(lineYAxis));

        XYChart.Series<String, Number> seriesUsers = new XYChart.Series<>();
        seriesUsers.setName("Utilisateurs");
        for (int i = 0; i < labels.length; i++) {
            seriesUsers.getData().add(new XYChart.Data<>(labels[i], usersData[i]));
        }

        XYChart.Series<String, Number> seriesOeuvres = new XYChart.Series<>();
        seriesOeuvres.setName("Oeuvres");
        for (int i = 0; i < labels.length; i++) {
            seriesOeuvres.getData().add(new XYChart.Data<>(labels[i], oeuvresData[i]));
        }

        lineChart.getData().addAll(seriesUsers, seriesOeuvres);
    }

    @FXML void switchTo7j(ActionEvent e)  { activateTab(btn7j);  setupLineChart(LABELS_7J,  USERS_7J,  OEUVRES_7J);  }
    @FXML void switchTo30j(ActionEvent e) { activateTab(btn30j); setupLineChart(LABELS_30J, USERS_30J, OEUVRES_30J); }
    @FXML void switchTo12m(ActionEvent e) { activateTab(btn12m); setupLineChart(LABELS_12M, USERS_12M, OEUVRES_12M); }

    private void activateTab(Button active) {
        for (Button b : new Button[]{btn7j, btn30j, btn12m}) {
            b.getStyleClass().removeAll("tab-btn-active");
            if (!b.getStyleClass().contains("tab-btn")) b.getStyleClass().add("tab-btn");
        }
        active.getStyleClass().add("tab-btn-active");
    }

    // ══════════════ Donut Chart ══════════════

    private void setupDonutChart() {
        Connection conn = MyDatabase.getInstance().getConnection();
        int clients  = 0;
        int artisans = 0;
        int admins   = 0;
        try {
            if (conn != null) {
                clients  = queryCount(conn, "SELECT COUNT(*) FROM `user` WHERE role='CLIENT'   AND deleted_at IS NULL");
                artisans = queryCount(conn, "SELECT COUNT(*) FROM `user` WHERE role='ARTISANT' AND deleted_at IS NULL");
                admins   = queryCount(conn, "SELECT COUNT(*) FROM `user` WHERE role='ADMIN'    AND deleted_at IS NULL");
            }
        } catch (Exception ignored) {}

        if (clients + artisans + admins == 0) { clients = 7; artisans = 2; admins = 1; }
        int total = clients + artisans + admins;

        // Pourcentages
        int pctClients  = Math.round(100f * clients  / total);
        int pctArtisans = Math.round(100f * artisans / total);
        int pctAdmins   = 100 - pctClients - pctArtisans;

        lblTotalRoles.setText(String.valueOf(total));
        lblPctClients.setText(pctClients  + "%");
        lblPctArtisans.setText(pctArtisans + "%");
        lblPctAdmins.setText(pctAdmins   + "%");

        // PieChart
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Clients",  clients),
                new PieChart.Data("Artisans", artisans),
                new PieChart.Data("Admins",   admins)
        );
        PieChart pie = new PieChart(data);
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setAnimated(false);
        pie.setPrefSize(190, 190);
        pie.setMaxSize(190, 190);

        // Cercle blanc au centre pour effet donut
        Circle hole = new Circle(58, Color.WHITE);

        // Récupérer le VBox label central (dernier enfant du StackPane)
        javafx.scene.Node labelOverlay = pieChartContainer.getChildren().get(0);
        pieChartContainer.getChildren().clear();
        pieChartContainer.getChildren().addAll(pie, hole, labelOverlay);
    }

    // ══════════════ Utilisateurs récents ══════════════

    private void loadRecentUsers() {
        Connection conn = MyDatabase.getInstance().getConnection();
        List<String[]> users = new ArrayList<>();
        if (conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nom, prenom, role FROM `user` WHERE deleted_at IS NULL ORDER BY id DESC LIMIT 5");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new String[]{
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("role")
                    });
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement utilisateurs récents : " + e.getMessage());
            }
        }
        // Données de démo si vide
        if (users.isEmpty()) {
            users.add(new String[]{"Ben Salah", "Ines", "CLIENT"});
            users.add(new String[]{"Trabelsi", "Mehdi", "ARTISANT"});
            users.add(new String[]{"Amouri", "Sana", "CLIENT"});
        }

        recentUsersContainer.getChildren().clear();
        String[] dates = {"12/04/2026","11/04/2026","10/04/2026","09/04/2026","08/04/2026"};
        for (int i = 0; i < users.size(); i++) {
            String[] u = users.get(i);
            String nom    = u[0] != null ? u[0] : "";
            String prenom = u[1] != null ? u[1] : "";
            String role   = u[2] != null ? u[2] : "USER";
            String date   = i < dates.length ? dates[i] : "";
            String initials = buildInitiales(prenom + " " + nom);

            HBox row = new HBox(12);
            row.getStyleClass().add("u-row-box");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 0, 10, 0));

            Label av = new Label(initials);
            av.getStyleClass().add("u-avatar-lbl");

            VBox info = new VBox(2);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
            Label nameL = new Label(prenom + " " + nom);
            nameL.getStyleClass().add("u-name-lbl");
            Label roleL = new Label(role);
            roleL.getStyleClass().add("u-role-lbl");
            info.getChildren().addAll(nameL, roleL);

            Label dateL = new Label(date);
            dateL.getStyleClass().add("u-date-lbl");

            row.getChildren().addAll(av, info, dateL);
            recentUsersContainer.getChildren().add(row);
        }
    }

    // ══════════════ Activité récente ══════════════

    private void loadRecentActivity() {
        recentActivityContainer.getChildren().clear();
        String[][] activities = {
                {"#4a1e2e", "Nouvel article publié : \"Poterie berbère\"",          "Il y a 2 min"},
                {"#c8763a", "Commande #1042 passée par Ines Ben Salah",             "Il y a 15 min"},
                {"#22a050", "Réclamation résolue pour Mehdi Trabelsi",              "Il y a 1 h"},
                {"#f59e0b", "Nouveau compte artisan : Farid Mansouri",              "Il y a 3 h"},
                {"#4a1e2e", "Article \"Broderie artisanale\" mis en avant",         "Il y a 5 h"},
        };
        for (String[] act : activities) {
            HBox row = new HBox(10);
            row.getStyleClass().add("act-row-box");
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(10, 0, 10, 0));

            javafx.scene.shape.Rectangle dot = new javafx.scene.shape.Rectangle(8, 8);
            dot.setFill(Color.web(act[0]));
            dot.setArcWidth(8);
            dot.setArcHeight(8);
            javafx.scene.layout.VBox dotWrap = new VBox(dot);
            dotWrap.setAlignment(Pos.CENTER);
            dotWrap.setPadding(new Insets(4, 0, 0, 0));

            Label msg = new Label(act[1]);
            msg.getStyleClass().add("act-msg-lbl");
            msg.setWrapText(true);
            HBox.setHgrow(msg, javafx.scene.layout.Priority.ALWAYS);

            Label time = new Label(act[2]);
            time.getStyleClass().add("act-time-lbl");
            time.setMinWidth(javafx.scene.control.Control.USE_PREF_SIZE);

            row.getChildren().addAll(dotWrap, msg, time);
            recentActivityContainer.getChildren().add(row);
        }
    }

    // ══════════════ Navigation ══════════════

    @FXML void handleNavDashboard(ActionEvent e) {
        if (scrollContent != null && dashboardContent != null) {
            scrollContent.setContent(dashboardContent);
            if (topbarTitle != null) topbarTitle.setText("Dashboard");
            setSidebarActive(btnNavDashboard);
        }
    }

    @FXML void handleNavProduits(ActionEvent e) {
        loadSubView("/fxml/AdminProduitsView.fxml", "Produits recyclables", btnNavProduits);
    }

    @FXML void handleNavUsers(ActionEvent e) {
        loadSubView("/fxml/AdminUsers.fxml", "Gestion des utilisateurs", btnNavUsers);
    }

    @FXML void handleNavPropositions(ActionEvent e) {
        loadSubView("/fxml/AdminPropositionsView.fxml", "Propositions", btnNavPropositions);
    }

    @FXML void handleNavReclamations(ActionEvent e) {
        loadSubView("/fxml/ReclamationsAdmin.fxml", "Réclamations", btnNavReclamations);
    }

    @FXML void handleNavArticles(ActionEvent e) {
        loadSubView("/fxml/AfficherArticles.fxml", "Articles", btnNavArticles);
    }

    @FXML void handleNavCommandes(ActionEvent e) {
        loadSubView("/fxml/GestionCommandes.fxml", "Commandes", btnNavCommandes);
    }

    @FXML void handleNavLivraisons(ActionEvent e) {
        loadSubView("/fxml/GestionLivraisons.fxml", "Gestion des Livraisons", btnNavLivraisons);
    }

    @FXML void handleNavSuiviLivraisons(ActionEvent e) {
        // Une seule interface de livraison: suivre redirige vers la meme vue de gestion.
        loadSubView("/fxml/GestionLivraisons.fxml", "Gestion des Livraisons", btnNavLivraisons);
    }

    @FXML void handleNavEvenements(ActionEvent e) {
        loadSubView("/fxml/AdminEvents.fxml", "Gestion des Événements", btnNavEvenements);
    }

    @FXML void handleNavReservations(ActionEvent e) {
        loadSubView("/fxml/AdminReservations.fxml", "Gestion des Réservations", btnNavReservations);
    }

    private void loadSubView(String fxmlPath, String titre, Button activeBtn) {
        try {
            javafx.scene.Node view = javafx.fxml.FXMLLoader.load(getClass().getResource(fxmlPath));
            if (scrollContent != null) {
                scrollContent.setContent(view);
            }
            if (topbarTitle != null) topbarTitle.setText(titre);
            setSidebarActive(activeBtn);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger : " + fxmlPath + "\n" + ex.getMessage()).showAndWait();
        }
    }

    private void setSidebarActive(Button active) {
        Button[] navBtns = {btnNavDashboard, btnNavUsers, btnNavProduits, btnNavPropositions,
                            btnNavReclamations, btnNavArticles, btnNavCommandes,
                            btnNavLivraisons, btnNavSuiviLivraisons,
                            btnNavEvenements, btnNavReservations};
        for (Button b : navBtns) {
            if (b == null) continue;
            b.getStyleClass().removeAll("nav-btn-active");
            if (!b.getStyleClass().contains("nav-btn")) b.getStyleClass().add("nav-btn");
        }
        if (active != null) {
            active.getStyleClass().remove("nav-btn");
            active.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML void handleLogout(ActionEvent event) {
        try {
            SessionManager.logout();
            SceneNavigator.navigate(event, "/fxml/Login.fxml", "Connexion", 520, 420);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // Gardés pour compatibilité avec d'autres appels éventuels
    @FXML void handleVoirPropositions(ActionEvent event) { handleNavPropositions(event); }
    @FXML void handleVoirProduits(ActionEvent event)     { handleNavProduits(event); }
    @FXML void handleVoirArticles(ActionEvent event)     { handleNavArticles(event); }
    @FXML void handleVoirCommandes(ActionEvent event)    { handleNavCommandes(event); }
    @FXML void handleVoirReclamations(ActionEvent event) { handleNavReclamations(event); }

    // ══════════════ Utilitaires ══════════════

    private String buildInitiales(String fullName) {
        if (fullName == null || fullName.isBlank()) return "AD";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
