package org.example.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import org.example.models.Evenement;
import org.example.models.Reservation;
import org.example.services.ServiceEvenement;
import org.example.services.ServiceReservation;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;

public class NouvelleReservationController {

    @FXML private ComboBox<Evenement> cbEvenement;
    @FXML private DatePicker dpDateReservation;
    @FXML private TextField tfNbPlaces;
    @FXML private RadioButton rbAttente, rbConfirme;
    @FXML private ImageView ivRecapImage;
    @FXML private Label lblRecapTitre, lblRecapLieu, lblRecapTag, lblRecapPrixUnitaire, lblRecapTotal;
    @FXML private Label lblErrEvenement, lblErrDate, lblErrPlaces;

    @FXML
    public void initialize() {
        // Load events
        try {
            ServiceEvenement se = new ServiceEvenement();
            List<Evenement> events = se.listerTousParDateDebutAsc();
            cbEvenement.setItems(FXCollections.observableArrayList(events));
            
            cbEvenement.setConverter(new StringConverter<Evenement>() {
                @Override public String toString(Evenement e) { return e == null ? "" : e.getNom(); }
                @Override public Evenement fromString(String string) { return null; }
            });
            
        } catch(Exception ex) {}

        dpDateReservation.setValue(LocalDate.now().plusDays(1));
        tfNbPlaces.setText("1");

        // Listeners for dynamic updates and validation (Classes Anonymes pour compatibilité)
        cbEvenement.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Evenement>() {
            @Override
            public void changed(ObservableValue<? extends Evenement> obs, Evenement oldV, Evenement newV) {
                updateRecap();
                validerEvenement();
            }
        });
        
        tfNbPlaces.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldV, String newV) {
                updateRecap();
                validerPlaces();
            }
        });

        dpDateReservation.valueProperty().addListener(new ChangeListener<LocalDate>() {
            @Override
            public void changed(ObservableValue<? extends LocalDate> obs, LocalDate oldV, LocalDate newV) {
                validerDate();
            }
        });
    }

    public void preselectEvenement(int evenementId) {
        for (Evenement e : cbEvenement.getItems()) {
            if (e.getId() == evenementId) {
                cbEvenement.setValue(e);
                break;
            }
        }
    }

    private void updateRecap() {
        Evenement e = cbEvenement.getValue();
        if (e == null) {
            lblRecapTitre.setText("...");
            lblRecapLieu.setText("📍 ...");
            lblRecapTag.setText("...");
            lblRecapPrixUnitaire.setText("0.00 TND");
            lblRecapTotal.setText("0.00 TND");
            ivRecapImage.setImage(null);
            return;
        }

        lblRecapTitre.setText(e.getNom());
        lblRecapLieu.setText("📍 " + (e.getLieu() != null ? e.getLieu() : "N/A"));
        lblRecapTag.setText(e.getTypeArt() != null ? e.getTypeArt() : "Général");
        
        double prix = e.getPrix() != null ? e.getPrix().doubleValue() : 0.0;
        lblRecapPrixUnitaire.setText(String.format("%.2f TND", prix));

        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                ivRecapImage.setImage(new Image(new File(e.getImage()).toURI().toString()));
            } catch(Exception ex) {}
        } else {
            ivRecapImage.setImage(null);
        }

        try {
            int nb = Integer.parseInt(tfNbPlaces.getText().trim());
            double total = nb * prix;
            lblRecapTotal.setText(String.format("%.2f TND", total));
        } catch(Exception ex) {
            lblRecapTotal.setText("0.00 TND");
        }
    }

    @FXML
    void handleCreerReservation(ActionEvent event) {
        if (!validerSaisie()) return;

        Evenement evt = cbEvenement.getValue();
        int nbPlaces = Integer.parseInt(tfNbPlaces.getText().trim());

        try {
            LocalDateTime dt = LocalDateTime.of(dpDateReservation.getValue(), LocalTime.NOON);
            String statut = rbConfirme.isSelected() ? ServiceReservation.STATUT_CONFIRME : ServiceReservation.STATUT_EN_ATTENTE;
            
            // Logique de Session Réelle et Dynamique (Fix bug list empty)
            int userId = org.example.utils.SessionStore.getUserId();
            
            Reservation r = new Reservation(evt.getId(), userId, dt, nbPlaces, statut);

            ServiceReservation service = new ServiceReservation();
            service.ajouter(r);
            
            // Message de succès (Urgent demandé par l'utilisateur)
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Réservation faite avec succès !");
            success.showAndWait();

            // Redirection vers le catalogue d'accueil (ModuleEvenementReservation)
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModuleEvenementReservation.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    private boolean validerSaisie() {
        boolean vEvt = validerEvenement();
        boolean vDate = validerDate();
        boolean vPlaces = validerPlaces();
        
        return vEvt && vDate && vPlaces;
    }

    private boolean validerEvenement() {
        if (cbEvenement.getValue() == null) {
            lblErrEvenement.setText("Veuillez sélectionner un événement.");
            lblErrEvenement.setVisible(true);
            lblErrEvenement.setManaged(true);
            cbEvenement.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
            return false;
        } else {
            lblErrEvenement.setVisible(false);
            lblErrEvenement.setManaged(false);
            cbEvenement.setStyle("-fx-border-color: #2ecc71;");
            return true;
        }
    }

    private boolean validerDate() {
        if (dpDateReservation.getValue() == null) {
            lblErrDate.setText("Date requise.");
            lblErrDate.setVisible(true);
            lblErrDate.setManaged(true);
            dpDateReservation.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
            return false;
        } else if (dpDateReservation.getValue().isBefore(LocalDate.now())) {
            lblErrDate.setText("Impossible de voyager dans le passé !");
            lblErrDate.setVisible(true);
            lblErrDate.setManaged(true);
            dpDateReservation.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
            return false;
        } else {
            lblErrDate.setVisible(false);
            lblErrDate.setManaged(false);
            dpDateReservation.setStyle("-fx-border-color: #2ecc71;");
            return true;
        }
    }

    private boolean validerPlaces() {
        try {
            int nb = Integer.parseInt(tfNbPlaces.getText().trim());
            if (nb < 1) throw new NumberFormatException();
            
            // Vérification capacité si événement sélectionné
            if (cbEvenement.getValue() != null) {
                Evenement e = cbEvenement.getValue();
                ServiceReservation sr = new ServiceReservation();
                int placesPrises = sr.sommePlacesReserveesActives(e.getId());
                int restantes = e.getCapacite() - placesPrises;
                
                if (nb > restantes) {
                    lblErrPlaces.setText("Plus assez de places ! (Reste: " + restantes + ")");
                    lblErrPlaces.setVisible(true);
                    lblErrPlaces.setManaged(true);
                    tfNbPlaces.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-background-radius: 5;");
                    return false;
                } else {
                    lblErrPlaces.setVisible(false);
                    lblErrPlaces.setManaged(false);
                    tfNbPlaces.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 1px; -fx-background-radius: 5;");
                    return true;
                }
            } else {
                tfNbPlaces.setStyle("");
                return true;
            }
        } catch (Exception e) {
            lblErrPlaces.setText("Veuillez saisir un nombre valide (>0).");
            lblErrPlaces.setVisible(true);
            lblErrPlaces.setManaged(true);
            tfNbPlaces.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-background-radius: 5;");
            return false;
        }
    }

    private void showTicket(Reservation r, Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
            Parent root = loader.load();
            
            TicketController controller = loader.getController();
            controller.setReservationData(r, e);
            
            Stage stage = new Stage();
            stage.setTitle("Billet Artisanal - " + e.getNom());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.INFORMATION, "Réservation confirmée.").show();
        }
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModuleEvenementReservation.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
