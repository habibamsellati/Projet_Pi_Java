package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class AfficherLivraisoncontrollers {

    @FXML private TableView<livraison> tableLivraisons;
    @FXML private TableColumn<livraison, String> colDate;
    @FXML private TableColumn<livraison, String> colAdresse;
    @FXML private TableColumn<livraison, String> colStatut;
    @FXML private TableColumn<livraison, Void> colActions;

    private final livraisonServices service = new livraisonServices();
    private ObservableList<livraison> livraisonList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateLivraison"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("addressLivraison"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutLivraison"));
        chargerDonnees();
        ajouterBoutonsActions();
    }

    private void chargerDonnees() {
        try {
            livraisonList.clear();
            livraisonList.addAll(service.findALL());
            tableLivraisons.setItems(livraisonList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void ajouterBoutonsActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMod = new Button();
            private final Button btnSupp = new Button();
            private final HBox pane = new HBox(btnMod, btnSupp);

            {
                pane.setSpacing(15);
                pane.setStyle("-fx-alignment: CENTER;");
                chargerIcones(btnMod, btnSupp);

                // ACTION MODIFIER
                btnMod.setOnAction(event -> {
                    livraison l = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterLivraison.fxml"));
                        Parent root = loader.load();

                        // Envoyer les données au contrôleur du formulaire
                        AjouterLivraisoncontrollers controller = loader.getController();
                        controller.setLivraisonData(l);

                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                    } catch (IOException e) { e.printStackTrace(); }
                });

                // ACTION SUPPRIMER
                btnSupp.setOnAction(event -> {
                    livraison l = getTableView().getItems().get(getIndex());
                    try {
                        service.deleteOne(l.getId());
                        chargerDonnees();
                    } catch (SQLException e) { e.printStackTrace(); }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }

            private void chargerIcones(Button m, Button s) {
                try {
                    InputStream isM = getClass().getResourceAsStream("/images/edit.png");
                    InputStream isS = getClass().getResourceAsStream("/images/delete.png");
                    if (isM != null) m.setGraphic(new ImageView(new Image(isM, 20, 20, true, true)));
                    if (isS != null) s.setGraphic(new ImageView(new Image(isS, 20, 20, true, true)));
                    m.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    s.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    @FXML
    void versAjouter(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AjouterLivraison.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}