package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.models.Article;
import org.example.services.PanierService;
import org.example.utils.SceneNavigator;

public class PanierClientController {

    @FXML private VBox     vboxEmpty;
    @FXML private ScrollPane scrollFilled;
    @FXML private VBox     vbItems;
    @FXML private Label    lblTotal;
    @FXML private HBox     hboxBottom;

    private final PanierService panierService = PanierService.getInstance();

    @FXML
    void initialize() {
        renderPanier();
    }

    @FXML
    void handleRetourCatalogue() {
        navigate("/fxml/ClientCatalogue.fxml", "Catalogue", 1320, 860);
    }

    @FXML
    void handleVoirHistorique() {
        navigate("/fxml/HistoriqueCommandes.fxml", "Historique des commandes", 1100, 760);
    }

    @FXML
    void handleValiderPanier() {
        if (panierService.getCount() == 0) {
            new Alert(Alert.AlertType.WARNING, "Votre panier est vide.").showAndWait();
            return;
        }
        navigate("/fxml/ValiderCommande.fxml", "Valider la commande", 1200, 760);
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    private void renderPanier() {
        boolean empty = panierService.getCount() == 0;

        vboxEmpty.setVisible(empty);
        vboxEmpty.setManaged(empty);
        scrollFilled.setVisible(!empty);
        scrollFilled.setManaged(!empty);
        hboxBottom.setVisible(!empty);
        hboxBottom.setManaged(!empty);

        if (!empty) {
            vbItems.getChildren().clear();
            for (Article article : panierService.getItems()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PanierItem.fxml"));
                    Node node = loader.load();
                    PanierItemController ctrl = loader.getController();
                    ctrl.setData(article);
                    ctrl.setOnRemove(() -> { panierService.retirer(article); renderPanier(); });
                    ctrl.setOnVoir(() -> navigate("/fxml/ClientCatalogue.fxml", "Catalogue", 1320, 860));
                    vbItems.getChildren().add(node);
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur chargement article : " + e.getMessage()).showAndWait();
                    break;
                }
            }
            lblTotal.setText(String.format("Total : %.2f DT", panierService.getTotal()));
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigate(String fxml, String title, double w, double h) {
        try {
            Stage stage = (Stage) hboxBottom.getScene().getWindow();
            SceneNavigator.navigate(stage, fxml, title, w, h);
        } catch (Exception e) {
            // hboxBottom might be hidden; try via vboxEmpty
            try {
                Stage stage = (Stage) vboxEmpty.getScene().getWindow();
                SceneNavigator.navigate(stage, fxml, title, w, h);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        }
    }
}
