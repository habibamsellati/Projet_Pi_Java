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
    @FXML private TextField tfTitre, tfPrix;
    @FXML private TextArea taContenu;
    @FXML private ComboBox<String> cbCategorie;

    private String selectedImagePath = "default.jpg";

    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString();
            // Optionnel: Afficher une miniature ici
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
                    1,
                    1
            );
            service.ajouter(a);
            new Alert(Alert.AlertType.INFORMATION, "Article publié avec succès !").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML void handleAnnuler() { /* Logique pour vider les champs ou fermer */ }
}