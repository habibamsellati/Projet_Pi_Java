package org.example.controllers;

import java.time.LocalDateTime;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.models.livraison;
import org.example.services.livraisonServices;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private TableView<livraison> tableLivraisons;
    @FXML private TableColumn<livraison, LocalDateTime> colDate;  // ✅ CORRIGÉ
    @FXML private TableColumn<livraison, String> colAdresse;
    @FXML private TableColumn<livraison, String> colStatut;
    @FXML private TableColumn<livraison, Void> colActions;
    @FXML private TextField searchField;

    @FXML private Label lblTotal;
    @FXML private Label lblLivrees;
    @FXML private Label lblAttente;

    private final livraisonServices service = new livraisonServices();
    private ObservableList<livraison> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateLivraison"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("addressLivraison"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutLivraison"));

        // ✅ FORMAT DATE PROPRE
        colDate.setCellFactory(column -> new TableCell<livraison, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String date = item.toString().replace("T", "  🕐  ");
                    setText(date.length() > 22 ? date.substring(0, 22) : date);
                    setStyle("-fx-text-fill: #4A3728; -fx-font-size: 12px;");
                }
            }
        });

        // ✅ ADRESSE STYLÉE
        colAdresse.setCellFactory(column -> new TableCell<livraison, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lbl = new Label("📍  " + item);
                    lbl.setStyle("-fx-text-fill: #2C1810; -fx-font-size: 12px; -fx-font-weight: bold;");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        // ✅ BADGE STATUT COLORÉ
        colStatut.setCellFactory(column -> new TableCell<livraison, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label badge = new Label();
                    if ("Livré".equalsIgnoreCase(item)) {
                        badge.setText("✅  Livré");
                        badge.setStyle("-fx-background-color: #D4EDDA; -fx-text-fill: #1B4332; " +
                                "-fx-background-radius: 20; -fx-padding: 5 14 5 14; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setText("⏳  En attente");
                        badge.setStyle("-fx-background-color: #FFF3CD; -fx-text-fill: #7C3D08; " +
                                "-fx-background-radius: 20; -fx-padding: 5 14 5 14; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER-LEFT;");
                }
            }
        });

        // ✅ LIGNES ALTERNÉES
        tableLivraisons.setRowFactory(tv -> new TableRow<livraison>() {
            @Override
            protected void updateItem(livraison item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #FFFFFF;");
                } else {
                    setStyle("-fx-background-color: #FAF6F2;");
                }
            }
        });

        chargerDonneesEtFiltrer();
        ajouterBoutonsActions();
    }

    private void mettreAJourStats() {
        int total = masterData.size();
        long livrees = masterData.stream()
                .filter(l -> "Livré".equalsIgnoreCase(l.getStatutLivraison()))
                .count();
        long attente = masterData.stream()
                .filter(l -> "En attente".equalsIgnoreCase(l.getStatutLivraison()))
                .count();
        lblTotal.setText(String.valueOf(total));
        lblLivrees.setText(String.valueOf(livrees));
        lblAttente.setText(String.valueOf(attente));
    }

    private void chargerDonneesEtFiltrer() {
        try {
            masterData.clear();
            masterData.addAll(service.findALL());
            mettreAJourStats();

            FilteredList<livraison> filteredData = new FilteredList<>(masterData, p -> true);
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(liv -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String filter = newValue.toLowerCase();
                    return liv.getAddressLivraison().toLowerCase().contains(filter) ||
                            (liv.getStatutLivraison() != null && liv.getStatutLivraison().toLowerCase().contains(filter)) ||
                            liv.getDateLivraison().toString().contains(filter);
                });
            });
            tableLivraisons.setItems(filteredData);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        Document document = new Document(PageSize.A4);
        try {
            String fileName = "Rapport_Livraisons_Afkart.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            Font fontTitre = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(93, 64, 55));
            Font fontHeader = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
            Font fontNormal = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

            Paragraph titre = new Paragraph("AFK'ART - GESTION DES LIVRAISONS", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(10f);
            document.add(titre);

            Paragraph sousTitre = new Paragraph("Rapport généré le : " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            sousTitre.setAlignment(Element.ALIGN_CENTER);
            sousTitre.setSpacingAfter(25f);
            document.add(sousTitre);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{3f, 4f, 2f});

            String[] headers = {"Date de Livraison", "Adresse", "Statut"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, fontHeader));
                cell.setBackgroundColor(new Color(141, 110, 99));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (livraison l : tableLivraisons.getItems()) {
                table.addCell(creerCellule(l.getDateLivraison().toString().replace("T", " "), fontNormal));
                table.addCell(creerCellule(l.getAddressLivraison(), fontNormal));
                table.addCell(creerCellule(l.getStatutLivraison(), fontNormal));
            }

            document.add(table);
            document.close();

            File pdfFile = new File(fileName);
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de générer le PDF", Alert.AlertType.ERROR);
        }
    }

    private PdfPCell creerCellule(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(texte, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void ajouterBoutonsActions() {
        colActions.setCellFactory(param -> new TableCell<livraison, Void>() {
            private final Button btnMod = new Button();
            private final Button btnSupp = new Button();
            private final HBox pane = new HBox(btnMod, btnSupp);
            {
                pane.setSpacing(15);
                pane.setStyle("-fx-alignment: CENTER;");
                chargerIcones(btnMod, btnSupp);
                btnMod.setOnAction(e -> changerVersFormulaire(getTableView().getItems().get(getIndex()), e));
                btnSupp.setOnAction(e -> {
                    try {
                        service.deleteOne(getTableView().getItems().get(getIndex()).getId());
                        chargerDonneesEtFiltrer();
                    } catch (SQLException ex) { ex.printStackTrace(); }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void changerVersFormulaire(livraison l, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterLivraison.fxml"));
            Parent root = loader.load();
            AjouterLivraisoncontrollers controller = loader.getController();
            controller.setBackOffice(true);
            if (l != null) controller.setLivraisonData(l);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML void versAjouter(ActionEvent event) { changerVersFormulaire(null, event); }

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

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setContentText(message);
        a.show();
    }
}