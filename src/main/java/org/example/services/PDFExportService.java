package org.example.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.example.models.Evenement;
import org.example.models.Reservation;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExportService {

    public void exportReservationsToPDF(Evenement event, List<Reservation> reservations, String destPath) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
        PdfWriter.getInstance(document, new FileOutputStream(destPath));
        document.open();

        // Titre du Document
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.BLACK);
        Paragraph title = new Paragraph("Liste des Réservations", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Informations de l'Événement
        Font eventFont = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.DARK_GRAY);
        Paragraph eventInfo = new Paragraph(
                "Événement : " + event.getNom() + "\n" +
                "Date : " + (event.getDateDebut() != null ? event.getDateDebut().toLocalDate().toString() : "N/A") + "\n" +
                "Lieu : " + event.getLieu() + "\n" +
                "Capacité : " + event.getCapacite() + " places", eventFont);
        eventInfo.setSpacingBefore(10);
        eventInfo.setSpacingAfter(20);
        document.add(eventInfo);

        // Table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Header
        String[] headers = {"ID Réservation", "Client", "Places", "Total (DT)", "Statut", "Date Réservation"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        for (String headerLabel : headers) {
            PdfPCell header = new PdfPCell(new Phrase(headerLabel, headerFont));
            header.setBackgroundColor(BaseColor.BLACK);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setPadding(8);
            table.addCell(header);
        }

        // Rows
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
        double prix = event.getPrix() != null ? event.getPrix().doubleValue() : 0.0;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Reservation r : reservations) {
            table.addCell(new PdfPCell(new Phrase("#RESV-" + r.getId(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(r.getUserId() != null ? "Client #" + r.getUserId() : "Anonyme", rowFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(r.getNbPlaces()), rowFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", r.getNbPlaces() * prix), rowFont)));
            table.addCell(new PdfPCell(new Phrase(r.getStatut().toUpperCase(), rowFont)));
            table.addCell(new PdfPCell(new Phrase(r.getCreatedAt() != null ? r.getCreatedAt().format(timeFormatter) : "N/A", rowFont)));
        }

        document.add(table);
        
        // Footer (Total Places)
        int totalPlaces = reservations.stream().mapToInt(Reservation::getNbPlaces).sum();
        Paragraph footer = new Paragraph("Total Places Réservées : " + totalPlaces + " / " + event.getCapacite(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK));
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
    }
}
