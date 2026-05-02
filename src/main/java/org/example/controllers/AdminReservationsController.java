package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.example.models.Reservation;
import org.example.models.Evenement;
import org.example.services.ServiceEvenement;
import org.example.services.ServiceReservation;

public class AdminReservationsController {

    @FXML
    private Label lblTotalReservations, lblPending, lblConfirmed, lblCanceled;
    @FXML
    private ProgressBar pbTotal, pbPending, pbConfirmed, pbCanceled;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> comboSort, comboOrder;
    @FXML
    private VBox vboxReservations;

    private ServiceEvenement serviceEvenement = new ServiceEvenement();
    private ServiceReservation serviceReservation = new ServiceReservation();

    private ObservableList<Reservation> allReservations = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Init ComboBoxes
        comboSort.getItems().addAll("Date", "Statut", "Événement", "ID");
        comboSort.setValue("Date");
        comboOrder.getItems().addAll("ASC", "DESC");
        comboOrder.setValue("DESC");

        loadStats();
        loadReservations();

        // Real-time search listener
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadStats() {
        try {
            int total = serviceReservation.compterTotal();
            int pending = serviceReservation.compterParStatut("en_attente");
            int confirmed = serviceReservation.compterParStatut("confirme");
            int canceled = serviceReservation.compterParStatut("annule");

            lblTotalReservations.setText(String.valueOf(total));
            lblPending.setText(String.valueOf(pending));
            lblConfirmed.setText(String.valueOf(confirmed));
            lblCanceled.setText(String.valueOf(canceled));

            pbTotal.setProgress(1.0);
            pbPending.setProgress(total > 0 ? (double) pending / total : 0);
            pbConfirmed.setProgress(total > 0 ? (double) confirmed / total : 0);
            pbCanceled.setProgress(total > 0 ? (double) canceled / total : 0);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadData() {
        loadReservations();
        loadStats();
    }

    private void loadReservations() {
        try {
            allReservations.setAll(serviceReservation.listerTous());
            applyFilters();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        vboxReservations.getChildren().clear();

        String query = tfSearch.getText() != null ? tfSearch.getText().toLowerCase() : "";
        String sortBy = comboSort.getValue();
        boolean asc = "ASC".equals(comboOrder.getValue());

        List<Reservation> filtered = allReservations.stream()
                .filter(r -> {
                    if (query.isEmpty())
                        return true;
                    return String.valueOf(r.getId()).contains(query) ||
                            r.getStatut().toLowerCase().contains(query);
                })
                .sorted((r1, r2) -> {
                    int res = 0;
                    switch (sortBy != null ? sortBy : "Date") {
                        case "Date":
                            res = r1.getCreatedAt().compareTo(r2.getCreatedAt());
                            break;
                        case "Statut":
                            res = r1.getStatut().compareTo(r2.getStatut());
                            break;
                        case "ID":
                            res = Integer.compare(r1.getId(), r2.getId());
                            break;
                        case "Événement":
                            res = Integer.compare(r1.getEvenementId(), r2.getEvenementId());
                            break;
                    }
                    return asc ? res : -res;
                })
                .collect(Collectors.toList());

        for (Reservation r : filtered) {
            addReservationRow(r);
        }
    }

    private void addReservationRow(Reservation r) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/AdminDashboardRow.fxml"));
            HBox row = loader.load();

            Label lblId = (Label) row.lookup("#lblId");
            if (lblId != null)
                lblId.setText("#" + r.getId());

            Evenement evt = serviceEvenement.trouverParId(r.getEvenementId());
            Label lblEvent = (Label) row.lookup("#lblEventTitle");
            if (lblEvent != null)
                lblEvent.setText(evt != null ? evt.getNom() : "Événement #" + r.getEvenementId());

            Label lblDate = (Label) row.lookup("#lblDate");
            if (lblDate != null)
                lblDate.setText(r.getCreatedAt() != null ? r.getCreatedAt().format(formatter) : "---");

            StackPane badge = (StackPane) row.lookup("#badgeContainer");
            Label lblStatus = (Label) row.lookup("#lblStatus");
            if (lblStatus != null)
                lblStatus.setText(r.getStatut().toUpperCase());

            if (badge != null) {
                badge.getStyleClass().removeAll("badge-confirmed", "badge-pending", "badge-cancelled");
                if (r.getStatut().contains("confirme"))
                    badge.getStyleClass().add("badge-confirmed");
                else if (r.getStatut().contains("attente"))
                    badge.getStyleClass().add("badge-pending");
                else
                    badge.getStyleClass().add("badge-cancelled");
            }

            vboxReservations.getChildren().add(row);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("Rapport_Reservations_AfkArt.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            exportToPDF(file);
        }
    }

    private void exportToPDF(File file) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);

            Paragraph title = new Paragraph("GESTION DES RÉSERVATIONS - RAPPORT ADMIN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            String[] headers = { "ID", "Événement", "Utilisateur", "Date", "Statut" };
            for (String h : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new BaseColor(139, 94, 74));
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Reservation r : allReservations) {
                table.addCell("#" + r.getId());
                Evenement e = serviceEvenement.trouverParId(r.getEvenementId());
                table.addCell(e != null ? e.getNom() : "ID: " + r.getEvenementId());
                table.addCell("User ID: " + r.getUserId());
                table.addCell(r.getCreatedAt() != null ? r.getCreatedAt().format(formatter) : "---");
                table.addCell(r.getStatut());
            }

            document.add(table);
            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le rapport PDF a été généré avec succès !");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de générer le PDF : " + e.getMessage()).show();
        }
    }
}
