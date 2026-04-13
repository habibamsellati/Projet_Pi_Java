package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.models.Commentaire;
import org.example.services.ServiceCommentaire;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommentaireController {
    @FXML
    private TextField tfArticleId;
    @FXML
    private TextField tfContenu;
    @FXML
    private ToggleButton star1, star2, star3, star4, star5;
    @FXML
    private ListView<String> lvCommentaires;
    @FXML
    private Label lblRating;

    private int currentArticleId = 0;
    private int currentNote = 5;

    @FXML
    public void initialize() {
        setRating(5);
    }

    @FXML
    void handleChoisirArticle() {
        try {
            int articleId = Integer.parseInt(tfArticleId.getText().trim());
            if (articleId <= 0) {
                throw new IllegalArgumentException("Article ID invalide.");
            }
            currentArticleId = articleId;
            rafraichirCommentaires();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleEnvoyer() {
        try {
            if (currentArticleId <= 0) {
                throw new IllegalArgumentException("Choisissez d'abord un article.");
            }
            String contenu = tfContenu.getText() == null ? "" : tfContenu.getText().trim();
            if (contenu.length() < 5) {
                throw new IllegalArgumentException("Le commentaire doit contenir au moins 5 caractères.");
            }

            Commentaire c = new Commentaire(currentArticleId, 3, contenu, currentNote);
            ServiceCommentaire service = new ServiceCommentaire();
            service.ajouter(c);
            tfContenu.clear();
            setRating(5);
            rafraichirCommentaires();
            new Alert(Alert.AlertType.INFORMATION, "Commentaire publié avec succès !").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleSupprimerSelection() {
        try {
            String selected = lvCommentaires.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.startsWith("#")) {
                throw new IllegalArgumentException("Sélectionnez un commentaire à supprimer.");
            }
            int end = selected.indexOf(' ');
            int commentaireId = Integer.parseInt(selected.substring(1, end));

            ServiceCommentaire service = new ServiceCommentaire();
            service.supprimer(commentaireId);
            rafraichirCommentaires();
            new Alert(Alert.AlertType.INFORMATION, "Commentaire supprimé.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void setStar1() { setRating(1); }
    @FXML
    void setStar2() { setRating(2); }
    @FXML
    void setStar3() { setRating(3); }
    @FXML
    void setStar4() { setRating(4); }
    @FXML
    void setStar5() { setRating(5); }

    public void setArticleId(int articleId) {
        this.currentArticleId = articleId;
        this.tfArticleId.setText(String.valueOf(articleId));
        try {
            rafraichirCommentaires();
        } catch (Exception ignored) {
        }
    }

    private void setRating(int rating) {
        currentNote = rating;
        star1.setSelected(rating >= 1);
        star2.setSelected(rating >= 2);
        star3.setSelected(rating >= 3);
        star4.setSelected(rating >= 4);
        star5.setSelected(rating >= 5);
        lblRating.setText("Note: " + rating + "/5");
    }

    private void rafraichirCommentaires() throws SQLException {
        ServiceCommentaire service = new ServiceCommentaire();
        List<Commentaire> list = service.afficherParArticle(currentArticleId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        lvCommentaires.setItems(FXCollections.observableArrayList(
                list.stream().map(c -> {
                    String date = (c.getDatePublication() == null) ? "-" : c.getDatePublication().format(fmt);
                    String stars = "★".repeat(Math.max(0, c.getNoteStars()));
                    return "#" + c.getId() + "  User " + c.getUserId() + "  [" + stars + "]  " + date + "  -  " + c.getContenu();
                }).toList()
        ));
    }
}

