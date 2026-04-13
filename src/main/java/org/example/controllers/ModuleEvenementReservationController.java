package org.example.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.example.models.Evenement;
import org.example.models.Reservation;
import org.example.services.ServiceReservation;
import org.example.services.ServiceEvenement;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class ModuleEvenementReservationController {

    @FXML private FlowPane fpEvenements;
    @FXML private StackPane popupOverlay;
    @FXML private ImageView ivPopupHeader, ivPopupGallery;
    @FXML private Label lblPopupTag, lblPopupTitre, lblPopupDate, lblPopupLieu, lblPopupDesc, lblPopupPrix, lblPopupDateDebut, lblPopupDateFin, lblPopupPlaces;
    
    @FXML private ComboBox<String> cbRole;
    @FXML private Button btnNouvelEvenement, btnMesReservations, btnReserver;
    @FXML private VBox boxActionsArtisan;

    private Evenement selectedEvenement = null;

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("Client / Visiteur", "Espace Artisan", "Administration (Admin)"));
        cbRole.setValue("Client / Visiteur");

        // Action sur changement de rôle (Remplacement de Lambda par ChangeListener anonyme)
        cbRole.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                if ("Administration (Admin)".equals(newVal)) {
                    switchToAdmin();
                } else {
                    updateRoleView("Espace Artisan".equals(newVal));
                }
            }
        });
        
        // Initialiser la vue (Client par défaut)
        updateRoleView(false);

        // Initialiser la vue (Client par défaut)
        updateRoleView(false);

        handleRefreshListeEvenements();
    }

    private void updateRoleView(boolean estArtisan) {
        // Dashboard
        btnNouvelEvenement.setVisible(estArtisan);
        btnNouvelEvenement.setManaged(estArtisan);
        
        // Le client doit aussi pouvoir voir ses réservations !
        btnMesReservations.setVisible(true);
        btnMesReservations.setManaged(true);
        btnMesReservations.setText(estArtisan ? "📋 Gestion Réservations" : "🎟️ Mes Réservations");
        
        // Popup (s'il est ouvert, ça le met à jour direct)
        btnReserver.setVisible(!estArtisan);
        btnReserver.setManaged(!estArtisan);
        boxActionsArtisan.setVisible(estArtisan);
        boxActionsArtisan.setManaged(estArtisan);
    }

    private void switchToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminLayout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cbRole.getScene().getWindow();
            stage.getScene().setRoot(root);
            
            // On peut aussi maximiser ou agrandir la fenêtre pour le dashboard
            stage.setWidth(1200);
            stage.setHeight(850);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            String cause = e.getCause() != null ? "\nCause: " + e.getCause().getMessage() : "";
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage() + cause).showAndWait();
        }
    }

    @FXML
    void handleRefreshListeEvenements() {
        try {
            ServiceEvenement service = new ServiceEvenement();
            List<Evenement> list = service.listerTousParDateDebutAsc();
            System.out.println("DEBUG: Rafraîchissement de la liste. Nombre d'événements: " + list.size());
            fpEvenements.getChildren().clear();
            
            for (Evenement e : list) {
                fpEvenements.getChildren().add(createEventCard(e));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'actualisation: " + e.getMessage()).show();
        }
    }

    private VBox createEventCard(Evenement e) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(320);
        card.setPrefHeight(380);

        // Header image
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(180);
        imageContainer.setStyle("-fx-background-radius: 20 20 0 0; -fx-background-color: #d3d3d3;");
        ImageView iv = new ImageView();
        iv.setFitWidth(320);
        iv.setFitHeight(180);
        iv.setPreserveRatio(false);
        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                iv.setImage(new Image(new File(e.getImage()).toURI().toString()));
            } catch (Exception ex) {}
        }
        
        // Tags overlay
        VBox tagsBox = new VBox();
        tagsBox.setPadding(new Insets(15));
        
        HBox topTags = new HBox();
        topTags.setAlignment(Pos.TOP_LEFT);
        Label tagType = new Label(e.getTypeArt() != null ? e.getTypeArt().toUpperCase() : "ART");
        tagType.setStyle("-fx-background-color: white; -fx-text-fill: #a06e4a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10;");
        topTags.getChildren().add(tagType);
        
        Region spacer1 = new Region(); VBox.setVgrow(spacer1, Priority.ALWAYS);
        
        HBox bottomTags = new HBox();
        bottomTags.setAlignment(Pos.BOTTOM_RIGHT);
        Label lblPrix = new Label(e.getPrix() != null ? e.getPrix() + " TND" : "Gratuit");
        lblPrix.setStyle("-fx-background-color: #c47653; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
        bottomTags.getChildren().add(lblPrix);
        
        tagsBox.getChildren().addAll(topTags, spacer1, bottomTags);
        imageContainer.getChildren().addAll(iv, tagsBox);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(20));
        
        Label lblLieu = new Label("📍 " + (e.getLieu() != null ? e.getLieu().toUpperCase() : ""));
        lblLieu.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        Label lblTitre = new Label(e.getNom());
        lblTitre.setStyle("-fx-font-family: 'Serif'; -fx-font-size: 24px; -fx-text-fill: #2d2d2d;");
        
        Label lblDesc = new Label(e.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setPrefHeight(45);
        lblDesc.setStyle("-fx-text-fill: #6b5b4f; -fx-font-size: 11px;");

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        VBox dateBox = new VBox();
        Label lblDateTitre = new Label("DATE");
        lblDateTitre.setStyle("-fx-font-size: 8px; -fx-text-fill: #9e8e82;");
        Label lblDateStr = new Label(e.getDateDebut() != null ? e.getDateDebut().toLocalDate().toString() : "");
        lblDateStr.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d;");
        dateBox.getChildren().addAll(lblDateTitre, lblDateStr);
        
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);
        Button btnDecouvrir = new Button("Découvrir");
        btnDecouvrir.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        
        final Evenement currentEvt = e;
        btnDecouvrir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleOuvrirPopup(currentEvt);
            }
        });
        
        footer.getChildren().addAll(dateBox, spacer2, btnDecouvrir);

        body.getChildren().addAll(lblLieu, lblTitre, lblDesc, new Separator(), footer);
        card.getChildren().addAll(imageContainer, body);
        return card;
    }

    private void handleOuvrirPopup(Evenement e) {
        this.selectedEvenement = e;
        lblPopupTitre.setText(e.getNom());
        lblPopupTag.setText(e.getTypeArt() != null ? e.getTypeArt().toUpperCase() : "ART & CULTURE");
        lblPopupDate.setText("📅 " + (e.getDateDebut() != null ? e.getDateDebut().toLocalDate().toString() : ""));
        lblPopupLieu.setText("📍 " + e.getLieu());
        lblPopupDesc.setText(e.getDescription());
        lblPopupPrix.setText(e.getPrix() != null ? e.getPrix().toString() : "0.0");
        lblPopupDateDebut.setText("Du " + (e.getDateDebut() != null ? e.getDateDebut().toString() : ""));
        lblPopupDateFin.setText("Au " + (e.getDateFin() != null ? e.getDateFin().toString() : ""));
        
        try {
            org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
            int reservees = sr.sommePlacesReserveesActives(e.getId());
            lblPopupPlaces.setText((e.getCapacite() - reservees) + " restantes");
        } catch(Exception ex) {}

        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                Image img = new Image(new File(e.getImage()).toURI().toString());
                ivPopupHeader.setImage(img);
                ivPopupGallery.setImage(img);
            } catch(Exception ex) {}
        }
        
        // S'assurer que le popup affiche les bons boutons selon le rôle
        updateRoleView("Espace Artisan".equals(cbRole.getValue()));
        popupOverlay.setVisible(true);
    }

    @FXML
    void handleFermerPopup() {
        popupOverlay.setVisible(false);
        selectedEvenement = null;
    }

    @FXML
    void handleSupprimerEvenement() {
        if (selectedEvenement == null) return;
        try {
            ServiceEvenement service = new ServiceEvenement();
            service.supprimer(selectedEvenement.getId());
            handleFermerPopup();
            handleRefreshListeEvenements();
            new Alert(Alert.AlertType.INFORMATION, "Événement supprimé.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    void handleModifierEvenement(ActionEvent event) {
        if (selectedEvenement == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterEvenement.fxml"));
            Parent root = loader.load();
            AjouterEvenementController controller = loader.getController();
            controller.setEvenementToEdit(selectedEvenement);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleGoToNouvelEvenement(ActionEvent event) {
        switchScene(event, "/fxml/AjouterEvenement.fxml");
    }

    @FXML
    void handleGoToReservations(ActionEvent event) {
        try {
            ServiceReservation sr = new ServiceReservation();
            java.util.List<Reservation> list = sr.listerTous();
            boolean estArtisan = "Espace Artisan".equals(cbRole.getValue());

            if (estArtisan) {
                // TableView classique pour l'Artisan (Gestion)
                showArtisanReservationTable(list);
            } else {
                // Vue artistique pour le Client (Billetterie)
                // On affiche TOUTES les réservations pour garantir que l'utilisateur voit son travail (Validation)
                java.util.List<Reservation> allList = sr.listerTous();
                System.out.println("DEBUG: Affichage de toutes les réservations (Mode Validation)");
                showClientTickets(allList);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    private void showArtisanReservationTable(List<Reservation> list) {
        VBox mainContainer = new VBox(30);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: #f7f3f0;");

        Label title = new Label("Tableau de Bord des Réservations");
        title.setStyle("-fx-font-family: 'Serif'; -fx-font-size: 28px; -fx-text-fill: #2d2d2d;");
        mainContainer.getChildren().add(title);

        ServiceEvenement se = new ServiceEvenement();
        
        // Grouper les réservations par événement
        java.util.Map<Integer, java.util.List<Reservation>> groups = new java.util.HashMap<Integer, java.util.List<Reservation>>();
        for (Reservation r : list) {
            if (!groups.containsKey(r.getEvenementId())) {
                groups.put(r.getEvenementId(), new java.util.ArrayList<Reservation>());
            }
            groups.get(r.getEvenementId()).add(r);
        }

        for (Integer evtId : groups.keySet()) {
            try {
                Evenement e = se.trouverParId(evtId);
                if (e != null) {
                    FXMLLoader groupLoader = new FXMLLoader(getClass().getResource("/fxml/ArtisanReservationGroup.fxml"));
                    VBox groupNode = groupLoader.load();
                    
                    // Récupérer les éléments du header
                    Label lblTitre = (Label) groupNode.lookup("#lblEventTitre");
                    Label lblSubtitle = (Label) groupNode.lookup("#lblEventSubtitle");
                    Label lblFilling = (Label) groupNode.lookup("#lblFillingRatio");
                    ProgressBar pb = (ProgressBar) groupNode.lookup("#pbFilling");
                    VBox vboxRows = (VBox) groupNode.lookup("#vboxRows");
                    ImageView ivThumb = (ImageView) groupNode.lookup("#ivEventThumb");

                    java.util.List<Reservation> resList = groups.get(evtId);
                    int totalPlaces = 0;
                    for (Reservation r : resList) totalPlaces += r.getNbPlaces();

                    lblTitre.setText(e.getNom().toUpperCase());
                    lblSubtitle.setText((e.getDateDebut() != null ? e.getDateDebut().toLocalDate().toString() : "") + "  " + e.getLieu().toUpperCase());
                    lblFilling.setText(totalPlaces + "/" + e.getCapacite());
                    pb.setProgress((double) totalPlaces / e.getCapacite());

                    if (e.getImage() != null && !e.getImage().isEmpty()) {
                        try {
                            File file = new File(e.getImage());
                            if (file.exists()) ivThumb.setImage(new Image(file.toURI().toString()));
                        } catch (Exception ex) {}
                    }

                    // Ajouter les lignes
                    for (Reservation r : resList) {
                        final Reservation currentRes = r; // Fix pour l'erreur "non-final variable"
                        
                        FXMLLoader rowLoader = new FXMLLoader(getClass().getResource("/fxml/ArtisanReservationRow.fxml"));
                        HBox rowNode = rowLoader.load();
                        
                        Label lblNom = (Label) rowNode.lookup("#lblNomClient");
                        Label lblRid = (Label) rowNode.lookup("#lblResId");
                        Label lblPl = (Label) rowNode.lookup("#lblPlaces");
                        Label lblHr = (Label) rowNode.lookup("#lblHeure");
                        Label lblEm = (Label) rowNode.lookup("#lblEmail");
                        Label lblTot = (Label) rowNode.lookup("#lblTotal");
                        Label lblSt = (Label) rowNode.lookup("#lblStatus");

                        // Mock des données client pour le CRUD
                        lblNom.setText(currentRes.getUserId() != null ? "Client #" + currentRes.getUserId() : "Anonyme");
                        lblRid.setText("#RESV-" + currentRes.getId());
                        lblPl.setText(currentRes.getNbPlaces() + " places");
                        lblHr.setText(currentRes.getCreatedAt() != null ? currentRes.getCreatedAt().toLocalTime().toString().substring(0,5) : "--:--");
                        lblEm.setText("client" + currentRes.getId() + "@example.tn");
                        lblTot.setText(String.format("%.2f", currentRes.getNbPlaces() * (e.getPrix() != null ? e.getPrix().doubleValue() : 0.0)) + " DT");
                        
                        lblSt.setText(currentRes.getStatut().equals("confirme") ? "Confirmé" : (currentRes.getStatut().equals("annule") ? "Annulé" : "En attente"));
                        
                        Button btnC = (Button) rowNode.lookup("#btnConfirm");
                        Button btnD = (Button) rowNode.lookup("#btnDelete");

                        btnC.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent ev) {
                                try {
                                    ServiceReservation sr = new ServiceReservation();
                                    sr.mettreAJourStatut(currentRes.getId(), "confirme");
                                    handleRefreshListeEvenements(); 
                                    handleGoToReservations(null); 
                                } catch (Exception ex) { ex.printStackTrace(); }
                            }
                        });

                        btnD.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent ev) {
                                try {
                                    ServiceReservation sr = new ServiceReservation();
                                    sr.supprimer(currentRes.getId());
                                    handleGoToReservations(null); 
                                } catch (Exception ex) { ex.printStackTrace(); }
                            }
                        });

                        vboxRows.getChildren().add(rowNode);
                    }
                    
                    mainContainer.getChildren().add(groupNode);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        ScrollPane sp = new ScrollPane(mainContainer);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        
        Stage stage = new Stage();
        stage.setTitle("Gestion des Réservations - AfkArt");
        stage.setScene(new Scene(sp, 900, 700));
        stage.show();
    }

    private void showClientTickets(List<Reservation> list) {
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setStyle("-fx-background-color: #faf7f2;");
        
        Label title = new Label("Tableau de Bord des Réservations");
        title.setStyle("-fx-font-family: 'Serif'; -fx-font-size: 32px; -fx-text-fill: #2d2d2d; -fx-font-weight: bold;");
        container.getChildren().add(title);

        ServiceEvenement se = new ServiceEvenement();

        for (Reservation r : list) {
            try {
                Evenement e = se.trouverParId(r.getEvenementId());
                if (e != null) {
                    VBox card = new VBox();
                    card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 10);");
                    
                    // 1. EN-TÊTE NOIR (STYLE ARTISAN)
                    HBox header = new HBox(20);
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setPadding(new Insets(20));
                    header.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 20 20 0 0;");
                    
                    ImageView iv = new ImageView();
                    iv.setFitHeight(70); iv.setFitWidth(100); iv.setPreserveRatio(false);
                    iv.setStyle("-fx-background-radius: 10;");
                    if (e.getImage() != null && !e.getImage().isEmpty()) {
                        File f = new File(e.getImage());
                        if (f.exists()) iv.setImage(new Image(f.toURI().toString()));
                    }
                    
                    VBox evtInfo = new VBox(5);
                    Label lblTitle = new Label(e.getNom().toUpperCase());
                    lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-family: 'Serif'; -fx-font-weight: bold;");
                    Label lblMeta = new Label(e.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "  " + e.getLieu().toUpperCase());
                    lblMeta.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px; -fx-font-weight: bold;");
                    evtInfo.getChildren().addAll(lblTitle, lblMeta);
                    
                    Region spacerH = new Region(); HBox.setHgrow(spacerH, Priority.ALWAYS);
                    
                    VBox capacityInfo = new VBox(5);
                    capacityInfo.setAlignment(Pos.CENTER_RIGHT);
                    Label lblCap = new Label("COMPLET DANS VOTRE LISTE");
                    lblCap.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                    ProgressBar pb = new ProgressBar(1.0);
                    pb.setPrefWidth(150);
                    pb.setStyle("-fx-accent: white;");
                    capacityInfo.getChildren().addAll(lblCap, pb);
                    
                    header.getChildren().addAll(iv, evtInfo, spacerH, capacityInfo);
                    
                    // 2. LIGNE DE DÉTAILS (STYLE TABLEAU ARTISAN)
                    HBox row = new HBox(40);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(25, 30, 25, 30));
                    
                    VBox colIdentite = new VBox(5);
                    Label lblClient = new Label("Réservation");
                    lblClient.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d2d2d;");
                    Label lblResId = new Label("#RESV-" + r.getId());
                    lblResId.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px;");
                    colIdentite.getChildren().addAll(lblClient, lblResId);
                    
                    Label lblNbPl = new Label(r.getNbPlaces() + " places");
                    lblNbPl.setMinWidth(100);
                    lblNbPl.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 13px;");
                    
                    Label lblHeure = new Label(r.getDateReservation().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                    lblHeure.setMinWidth(80);
                    lblHeure.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 13px;");
                    
                    VBox colContact = new VBox(5);
                    Label lblMail = new Label("mariem@artisan.tn");
                    lblMail.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px;");
                    Label lblTel = new Label("N/A");
                    lblTel.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px;");
                    colContact.getChildren().addAll(lblMail, lblTel);
                    
                    double total = r.getNbPlaces() * (e.getPrix() != null ? e.getPrix().doubleValue() : 0.0);
                    Label lblTotal = new Label(String.format("%.2f DT", total));
                    lblTotal.setMinWidth(120);
                    lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2d2d2d;");
                    
                    // BADGE STATUT
                    Label badge = new Label(r.getStatut().substring(0, 1).toUpperCase() + r.getStatut().substring(1).toLowerCase());
                    badge.setMinWidth(100); badge.setAlignment(Pos.CENTER);
                    if ("confirme".equalsIgnoreCase(r.getStatut())) {
                        badge.setStyle("-fx-background-color: #e6f7ef; -fx-text-fill: #2ecc71; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: #fef5ed; -fx-text-fill: #f39c12; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
                    }
                    
                    row.getChildren().addAll(colIdentite, lblNbPl, lblHeure, colContact, lblTotal, badge);
                    
                    card.getChildren().addAll(header, row);
                    container.getChildren().add(card);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setPrefSize(1000, 800);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #faf7f2;");
        
        Stage stage = new Stage();
        stage.setTitle("Mes Réservations Premium - AfkArt Dashboard");
        stage.setScene(new Scene(sp));
        stage.show();
    }

    @FXML
    void handleReserverDepuisPopup(ActionEvent event) {
        // Here we should normally pass the selected event to the NouvelleReservation controller to preselect it.
        // For simplicity with FXML switch, we will store it in a static variable or just switch immediately 
        // and let the reservation UI handle selection. In a real app we'd get the controller.
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NouvelleReservation.fxml"));
            Parent root = loader.load();
            NouvelleReservationController c = loader.getController();
            c.preselectEvenement(selectedEvenement.getId()); // will implement this method
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            String cause = e.getCause() != null ? e.getCause().toString() : e.getMessage();
            new Alert(Alert.AlertType.ERROR, "Impossible de charger l'interface: " + cause).show();
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger l'interface: " + e.getMessage()).show();
        }
    }
}
