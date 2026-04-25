package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.example.models.Article;
import org.example.services.ServiceArticle;
import java.io.File;
import java.sql.SQLException;

public class AjouterArticleController {
    @FXML
    private TextField tfTitre, tfPrix;
    @FXML
    private TextArea taContenu;
    @FXML
    private ComboBox<String> cbCategorie;
    @FXML
    private Label lblImageName;

    private String selectedImagePath = "default.jpg";

    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir l'Image de l'Article");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString();
            lblImageName.setText(selectedFile.getName());
        }
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            ServiceArticle service = new ServiceArticle();
            String cat = cbCategorie.getValue() == null ? "" : cbCategorie.getValue();
            Article a = new Article(
                    tfTitre.getText(),
                    taContenu.getText(),
                    Double.parseDouble(tfPrix.getText()),
                    cat,
                    selectedImagePath,
                    org.example.utils.SessionStore.getUserId(),
                    1 // Default shop id or category?
            );
            service.ajouter(a);
            new Alert(Alert.AlertType.INFORMATION, "Article publié avec succès !").show();
            handleAnnuler(event); // Re-use navigation logic to go back to catalogue
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        try {
            String fxml = "/fxml/ModuleEcommerceV1.fxml"; // Default
            if (org.example.utils.SessionStore.isAdmin()) {
                fxml = "/fxml/AdminLayout.fxml";
            } else if (org.example.utils.SessionStore.isArtisan()) {
                fxml = "/fxml/ModuleEcommerceV1.fxml";
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene()
                    .getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}