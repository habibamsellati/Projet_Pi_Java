package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.models.Commande;
import org.example.services.ServiceCommande;

import java.io.File;
import java.util.List;

public class PasserCommandeController {
    @FXML
    private TextField tfProduitId, tfQuantite, tfPrixUnitaire, tfAdresseLivraison, tfTelephone, tfCommandeId;
    @FXML
    private ComboBox<String> cbStatut;
    @FXML
    private Label lblProduitNom, lblImage, lblStock, lblTotalPreview, lblCommandeStatut;
    @FXML
    private ImageView imageViewProduit;
    @FXML
    private TextArea taAdminCommandes;

    @FXML
    public void initialize() {
        cbStatut.getItems().addAll(ServiceCommande.STATUT_EN_ATTENTE, ServiceCommande.STATUT_CONFIRMEE, ServiceCommande.STATUT_LIVREE);
        cbStatut.setValue(ServiceCommande.STATUT_EN_ATTENTE);
    }

    @FXML
    void handleChargerProduit(ActionEvent event) {
        try {
            int produitId = Integer.parseInt(tfProduitId.getText().trim());
            ServiceCommande service = new ServiceCommande();
            ServiceCommande.ProduitApercu apercu = service.getProduitApercu(produitId);

            if (apercu == null) {
                throw new IllegalArgumentException("Aucun produit trouvé pour cet ID.");
            }

            lblProduitNom.setText(apercu.getNom());
            lblImage.setText(apercu.getImage() == null ? "-" : apercu.getImage());
            lblStock.setText(String.valueOf(apercu.getStock()));
            chargerImage(apercu.getImage());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleCalculerTotal(ActionEvent event) {
        try {
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            double prixUnitaire = Double.parseDouble(tfPrixUnitaire.getText().trim());
            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive.");
            }
            if (prixUnitaire < 0) {
                throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif.");
            }
            double total = quantite * prixUnitaire;
            lblTotalPreview.setText(String.format("%.2f DT", total));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handlePasserCommande(ActionEvent event) {
        try {
            int produitId = Integer.parseInt(tfProduitId.getText().trim());
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            double prixUnitaire = Double.parseDouble(tfPrixUnitaire.getText().trim());

            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive.");
            }
            if (prixUnitaire < 0) {
                throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif.");
            }

            String statut = cbStatut.getValue() == null ? ServiceCommande.STATUT_EN_ATTENTE : cbStatut.getValue();
            ServiceCommande service = new ServiceCommande();
            Commande commande = service.creerCommande(
                    3,
                    produitId,
                    quantite,
                    prixUnitaire,
                    tfAdresseLivraison.getText().trim(),
                    tfTelephone.getText().trim()
            );
            if (statut != null && !statut.equalsIgnoreCase(ServiceCommande.STATUT_EN_ATTENTE)) {
                service.mettreAJourStatut(commande.getId(), statut);
                commande.setStatut(statut);
            }

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Commande confirmée !\nID commande: " + commande.getId() +
                            "\nMontant total à payer: " + String.format("%.2f DT", commande.getTotal())
            ).show();
            lblCommandeStatut.setText(commande.getStatut());
            chargerBackoffice();

            handleAnnuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnulerCommande(ActionEvent event) {
        try {
            int commandeId = Integer.parseInt(tfCommandeId.getText().trim());
            ServiceCommande service = new ServiceCommande();
            service.annulerCommandeClient(commandeId, 3);
            new Alert(Alert.AlertType.INFORMATION, "Commande annulée avec succès.").show();
            chargerBackoffice();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleMettreAJourStatut(ActionEvent event) {
        try {
            int commandeId = Integer.parseInt(tfCommandeId.getText().trim());
            String statut = cbStatut.getValue();
            if (statut == null || statut.isBlank()) {
                throw new IllegalArgumentException("Sélectionnez un statut.");
            }
            ServiceCommande service = new ServiceCommande();
            service.mettreAJourStatut(commandeId, statut);
            lblCommandeStatut.setText(statut);
            new Alert(Alert.AlertType.INFORMATION, "Statut de commande mis à jour.").show();
            chargerBackoffice();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAfficherBackoffice(ActionEvent event) {
        try {
            chargerBackoffice();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    void handleAnnuler() {
        tfProduitId.clear();
        tfQuantite.clear();
        tfPrixUnitaire.clear();
        tfAdresseLivraison.clear();
        tfTelephone.clear();
        cbStatut.setValue(ServiceCommande.STATUT_EN_ATTENTE);
        lblProduitNom.setText("-");
        lblImage.setText("-");
        lblStock.setText("-");
        lblTotalPreview.setText("0.00 DT");
        lblCommandeStatut.setText("-");
        imageViewProduit.setImage(null);
    }

    private void chargerImage(String imageName) {
        imageViewProduit.setImage(null);
        if (imageName == null || imageName.isBlank()) {
            return;
        }
        try {
            File file = new File(imageName);
            if (file.exists()) {
                imageViewProduit.setImage(new Image(file.toURI().toString(), true));
                return;
            }
            imageViewProduit.setImage(new Image(imageName, true));
        } catch (Exception ignored) {
            // L'image peut être un simple nom de fichier en base; le label affiche quand même la valeur.
        }
    }

    private void chargerBackoffice() throws Exception {
        ServiceCommande service = new ServiceCommande();
        List<Commande> commandes = service.afficherToutesLesCommandes();
        if (commandes.isEmpty()) {
            taAdminCommandes.setText("Aucune commande.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Commande c : commandes) {
            sb.append("ID: ").append(c.getId())
                    .append(" | User: ").append(c.getUserId())
                    .append(" | Total: ").append(String.format("%.2f", c.getTotal())).append(" DT")
                    .append(" | Statut: ").append(c.getStatut())
                    .append(" | Adresse: ").append(c.getAdresseLivraison())
                    .append(" | Téléphone: ").append(c.getTelephone())
                    .append("\n");
        }
        taAdminCommandes.setText(sb.toString());
    }
}
