package org.example.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.Evenement;
import org.example.services.ServiceEvenement;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class AjouterEvenementController {

    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private TextField tfNom, tfTheme, tfLieu, tfCapacite, tfPrix, tfHeureDebut, tfHeureFin;
    @FXML
    private ComboBox<String> cbTypeArt;
    @FXML
    private TextArea taDescription;
    @FXML
    private DatePicker dpDateDebut, dpDateFin;

    // Validation Labels
    @FXML
    private Label lblErrNom, lblErrDescription, lblErrDebut, lblErrFin, lblErrLieu, lblErrCapacite, lblErrPrix,
            lblImage;

    @FXML
    private Button btnSubmit;

    private Integer editingEvenementId = null;
    private String imagePath = null;

    @FXML
    public void initialize() {
        // Populate Art Categories
        cbTypeArt.setItems(javafx.collections.FXCollections.observableArrayList(
                "Peinture", "Sculpture", "Artisanat", "Céramique", "Décoration", "Mix artistique"));

        // Defaults
        dpDateDebut.setValue(LocalDate.now().plusDays(7));
        dpDateFin.setValue(LocalDate.now().plusDays(7));
        tfHeureDebut.setText("10:00");
        tfHeureFin.setText("12:00");

        // Real-time validation listeners (Remplacement de Lambda par Classes Anonymes
        // pour compatibilité)
        tfNom.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerNom();
            }
        });
        taDescription.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerDescription();
            }
        });
        tfLieu.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerLieu();
            }
        });
        tfCapacite.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerCapacite();
            }
        });
        tfPrix.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerPrix();
            }
        });

        dpDateDebut.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
                validerDates();
            }
        });
        dpDateFin.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldVal, LocalDate newVal) {
                validerDates();
            }
        });
        tfHeureDebut.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerDates();
            }
        });
        tfHeureFin.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                validerDates();
            }
        });
    }

    public void setEvenementToEdit(Evenement e) {
        if (e == null)
            return;
        editingEvenementId = e.getId();
        tfNom.setText(e.getNom());
        cbTypeArt.setValue(e.getTypeArt());
        tfTheme.setText(e.getTheme());
        taDescription.setText(e.getDescription());
        tfLieu.setText(e.getLieu());
        tfCapacite.setText(String.valueOf(e.getCapacite()));
        if (e.getPrix() != null)
            tfPrix.setText(e.getPrix().toPlainString());

        if (e.getDateDebut() != null) {
            dpDateDebut.setValue(e.getDateDebut().toLocalDate());
            tfHeureDebut.setText(String.format("%02d:%02d", e.getDateDebut().getHour(), e.getDateDebut().getMinute()));
        }
        if (e.getDateFin() != null) {
            dpDateFin.setValue(e.getDateFin().toLocalDate());
            tfHeureFin.setText(String.format("%02d:%02d", e.getDateFin().getHour(), e.getDateFin().getMinute()));
        }
        if (e.getImage() != null && !e.getImage().isBlank()) {
            imagePath = e.getImage();
            lblImage.setText("Image existante (conservée)");
        }
        btnSubmit.setText("Mettre à jour l'Événement");
    }

    @FXML
    void handleGenerateAIDesc(ActionEvent event) {
        String title = tfNom.getText();
        if (title == null || title.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez d'abord saisir un titre pour que l'IA puisse s'en inspirer.")
                    .show();
            return;
        }
        org.example.services.AIService ai = new org.example.services.AIService();
        String magicDesc = ai.generateDescription(title, cbTypeArt.getValue(), tfTheme.getText());
        taDescription.setText(magicDesc);
    }

    @FXML
    void handleSuggestPrice(ActionEvent event) {
        org.example.services.AIService ai = new org.example.services.AIService();
        double price = ai.suggestPricing(cbTypeArt.getValue());
        tfPrix.setText(String.format("%.2f", price));
        new Alert(Alert.AlertType.INFORMATION, "L'IA suggère un prix de " + price + " TND basé sur le type d'art.")
                .show();
    }

    @FXML
    void handleChoisirImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choisir l'affiche de l'événement");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (file != null) {
            imagePath = file.getAbsolutePath();
            lblImage.setText(file.getName());
        }
    }

    @FXML
    void handleCreerEvenement(ActionEvent event) {
        if (!validerSaisie()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez corriger les erreurs indiquées en rouge dans le formulaire.")
                    .show();
            return; // Saisie invalide
        }

        try {
            Evenement e = new Evenement();
            e.setNom(tfNom.getText().trim());
            // Utilisation du nom réel de l'artisan connecté
            String artisanName = "Artisan";
            if (org.example.utils.SessionStore.isConnected()) {
                artisanName = org.example.utils.SessionStore.getCurrentUser().getNom() + " " +
                        org.example.utils.SessionStore.getCurrentUser().getPrenom();
            }
            e.setArtisan(artisanName);
            e.setDescription(taDescription.getText().trim());
            e.setLieu(tfLieu.getText().trim());
            e.setCapacite(Integer.parseInt(tfCapacite.getText().trim()));

            String tArt = cbTypeArt.getValue();
            e.setTypeArt(tArt != null && !tArt.isBlank() ? tArt.trim() : null);

            String tTheme = tfTheme.getText();
            e.setTheme(tTheme != null && !tTheme.isBlank() ? tTheme.trim() : null);

            String px = tfPrix.getText();
            if (px != null && !px.isBlank()) {
                e.setPrix(new BigDecimal(px.replace(",", ".").trim()));
            }

            e.setDateDebut(LocalDateTime.of(dpDateDebut.getValue(), LocalTime.parse(tfHeureDebut.getText().trim())));
            e.setDateFin(LocalDateTime.of(dpDateFin.getValue(), LocalTime.parse(tfHeureFin.getText().trim())));
            e.setStatut("publie"); // Statut par défaut
            e.setImage(imagePath);

            ServiceEvenement service = new ServiceEvenement();
            if (editingEvenementId != null) {
                e.setId(editingEvenementId);
                service.modifier(e);
            } else {
                service.ajouter(e);
            }

            System.out.println("DEBUG: Evenement " + (editingEvenementId != null ? "modifié" : "ajouté")
                    + " avec succès ! ID: " + e.getId());
            new Alert(Alert.AlertType.INFORMATION, "Félicitations ! L'événement a été "
                    + (editingEvenementId != null ? "mis à jour" : "créé") + " avec succès.").showAndWait();

            // Back to dashboard
            handleGoBack(event);

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement: " + ex.getMessage()).show();
        }
    }

    private boolean validerSaisie() {
        boolean vNom = validerNom();
        boolean vDesc = validerDescription();
        boolean vLieu = validerLieu();
        boolean vCap = validerCapacite();
        boolean vPrix = validerPrix();
        boolean vDates = validerDates();

        return vNom && vDesc && vLieu && vCap && vPrix && vDates;
    }

    private void applyError(Control c, Label lbl, String msg) {
        c.getStyleClass().remove("field-success");
        if (!c.getStyleClass().contains("field-error")) {
            c.getStyleClass().add("field-error");
        }
        if (lbl != null) {
            lbl.setText(msg);
            lbl.setVisible(true);
            lbl.setManaged(true);
            lbl.getStyleClass().add("error-label");
        }
    }

    private void applySuccess(Control c, Label lbl) {
        c.getStyleClass().remove("field-error");
        if (!c.getStyleClass().contains("field-success")) {
            c.getStyleClass().add("field-success");
        }
        if (lbl != null) {
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void clearValidation(Control c, Label lbl) {
        c.getStyleClass().removeAll("field-error", "field-success");
        if (lbl != null) {
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private boolean validerNom() {
        String nom = tfNom.getText() == null ? "" : tfNom.getText().trim();
        if (nom.length() < 3 || nom.length() > 255) {
            applyError(tfNom, lblErrNom, "Le titre doit contenir entre 3 et 255 caractères.");
            return false;
        } else {
            applySuccess(tfNom, lblErrNom);
            return true;
        }
    }

    private boolean validerDescription() {
        String desc = taDescription.getText() == null ? "" : taDescription.getText().trim();
        if (desc.length() < 10) {
            applyError(taDescription, lblErrDescription, "Description trop courte (minimum 10 caractères).");
            return false;
        } else {
            applySuccess(taDescription, lblErrDescription);
            return true;
        }
    }

    private boolean validerLieu() {
        String lieu = tfLieu.getText() == null ? "" : tfLieu.getText().trim();
        if (lieu.length() < 3) {
            applyError(tfLieu, lblErrLieu, "Veuillez indiquer un espace d'accueil.");
            return false;
        } else {
            applySuccess(tfLieu, lblErrLieu);
            return true;
        }
    }

    private boolean validerCapacite() {
        try {
            int cap = Integer.parseInt(tfCapacite.getText().trim());
            if (cap < 1 || cap > 10000)
                throw new NumberFormatException();
            applySuccess(tfCapacite, lblErrCapacite);
            return true;
        } catch (NumberFormatException e) {
            applyError(tfCapacite, lblErrCapacite, "Jauge invalide (doit être entre 1 et 10 000).");
            return false;
        }
    }

    private boolean validerPrix() {
        String px = tfPrix.getText();
        if (px == null || px.isBlank()) {
            clearValidation(tfPrix, lblErrPrix);
            return true;
        }
        try {
            BigDecimal val = new BigDecimal(px.replace(",", ".").trim());
            if (val.compareTo(BigDecimal.ZERO) < 0)
                throw new Exception();
            applySuccess(tfPrix, lblErrPrix);
            return true;
        } catch (Exception e) {
            applyError(tfPrix, lblErrPrix, "Montant invalide (ex: 15.50).");
            return false;
        }
    }

    private boolean validerDates() {
        boolean valid = true;
        LocalDate dDebut = dpDateDebut.getValue();
        LocalTime hDebut = null;

        // Reset Styles
        clearValidation(dpDateDebut, null);
        clearValidation(tfHeureDebut, lblErrDebut);
        clearValidation(dpDateFin, null);
        clearValidation(tfHeureFin, lblErrFin);

        try {
            if (dDebut == null) throw new Exception();
            hDebut = LocalTime.parse(tfHeureDebut.getText().trim());
            applySuccess(dpDateDebut, null);
            applySuccess(tfHeureDebut, lblErrDebut);
        } catch (Exception e) {
            applyError(tfHeureDebut, lblErrDebut, "Format heure invalide (HH:mm)");
            applyError(dpDateDebut, null, null);
            valid = false;
        }

        LocalDate dFin = dpDateFin.getValue();
        LocalTime hFin = null;
        try {
            if (dFin == null) throw new Exception();
            hFin = LocalTime.parse(tfHeureFin.getText().trim());
            applySuccess(dpDateFin, null);
            applySuccess(tfHeureFin, lblErrFin);
        } catch (Exception e) {
            applyError(tfHeureFin, lblErrFin, "Format heure invalide (HH:mm)");
            applyError(dpDateFin, null, null);
            valid = false;
        }

        if (valid && hDebut != null && hFin != null && dDebut != null && dFin != null) {
            LocalDateTime dtDebut = LocalDateTime.of(dDebut, hDebut);
            LocalDateTime dtFin = LocalDateTime.of(dFin, hFin);

            // Règle: Doit être dans le futur
            if (dtDebut.isBefore(LocalDateTime.now())) {
                applyError(tfHeureDebut, lblErrDebut, "L'inauguration doit être dans le futur.");
                applyError(dpDateDebut, null, null);
                valid = false;
            }

            // Clôture après inauguration
            if (dtFin.isBefore(dtDebut)) {
                applyError(tfHeureFin, lblErrFin, "La clôture doit être après l'inauguration.");
                applyError(dpDateFin, null, null);
                valid = false;
            }
        }
        return valid;
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            String fxml = "/fxml/ModuleEvenementReservation.fxml"; // Default
            if (org.example.utils.SessionStore.isAdmin()) {
                fxml = "/fxml/AdminLayout.fxml";
            } else if (org.example.utils.SessionStore.isArtisan()) {
                fxml = "/fxml/ModuleEvenementReservation.fxml";
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
