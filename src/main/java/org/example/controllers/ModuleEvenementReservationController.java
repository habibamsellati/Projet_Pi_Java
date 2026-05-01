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
import org.example.utils.SessionStore;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class ModuleEvenementReservationController {

    @FXML
    private FlowPane fpEvenements;
    @FXML
    private StackPane popupOverlay;
    @FXML
    private ImageView ivPopupHeader, ivPopupGallery;
    @FXML
    private Label lblPopupTag, lblPopupTitre, lblPopupDate, lblPopupLieu, lblPopupDesc, lblPopupPrix, lblPopupDateDebut,
            lblPopupDateFin, lblPopupPlaces;

    @FXML
    private ComboBox<String> cbRole;
    @FXML
    private Button btnLogin, btnLogout, btnNouvelEvenement, btnMesReservations, btnReserver;
    @FXML
    private VBox boxActionsArtisan;

    private Evenement selectedEvenement = null;

    @FXML
    public void initialize() {
        // Cacher le simulateur si présent (pour rétrocompatibilité FXML)
        if (cbRole != null) {
            cbRole.setVisible(false);
            cbRole.setManaged(false);
        }

        // Initialiser la vue selon le rôle réel stocké en session
        updateRoleView();

        handleRefreshListeEvenements();
    }

    private void updateRoleView() {
        boolean connected = SessionStore.isConnected();
        boolean estArtisan = SessionStore.isArtisan();

        // Header Buttons
        if (btnLogin != null) {
            btnLogin.setVisible(!connected);
            btnLogin.setManaged(!connected);
        }
        if (btnLogout != null) {
            btnLogout.setVisible(connected);
            btnLogout.setManaged(connected);
        }

        // Dashboard/Actions
        btnNouvelEvenement.setVisible(connected && estArtisan);
        btnNouvelEvenement.setManaged(connected && estArtisan);

        btnMesReservations.setVisible(connected);
        btnMesReservations.setManaged(connected);
        if (connected) {
            btnMesReservations.setText(estArtisan ? "📋 Gestion Réservations" : "🎟️ Mes Réservations");
        }

        // Popup
        btnReserver.setVisible(!estArtisan); // Un guest peut voir mais sera redirigé au clic
        btnReserver.setManaged(!estArtisan);
        boxActionsArtisan.setVisible(connected && estArtisan);
        boxActionsArtisan.setManaged(connected && estArtisan);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        org.example.utils.SessionStore.logout();
        updateRoleView();
        // Redirect to Home after logout
        handleGoToHome(event);
    }

    @FXML
    void handleGoToHome(ActionEvent event) {
        navigate(event, "/fxml/Home.fxml");
    }

    @FXML
    void handleGoToArticles(ActionEvent event) {
        navigate(event, "/fxml/ModuleEcommerceV1.fxml");
    }

    private void navigate(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleGoToLogin(ActionEvent event) {
        switchScene(event, "/fxml/Login.fxml");
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
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
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
            } catch (Exception ex) {
            }
        }

        // Tags overlay
        VBox tagsBox = new VBox();
        tagsBox.setPadding(new Insets(15));

        HBox topTags = new HBox();
        topTags.setAlignment(Pos.TOP_LEFT);
        Label tagType = new Label(e.getTypeArt() != null ? e.getTypeArt().toUpperCase() : "ART");
        tagType.setStyle(
                "-fx-background-color: white; -fx-text-fill: #a06e4a; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 3 10;");
        topTags.getChildren().add(tagType);

        Region spacer1 = new Region();
        VBox.setVgrow(spacer1, Priority.ALWAYS);

        HBox bottomTags = new HBox();
        bottomTags.setAlignment(Pos.BOTTOM_RIGHT);
        Label lblPrix = new Label(e.getPrix() != null ? e.getPrix() + " TND" : "Gratuit");
        lblPrix.setStyle(
                "-fx-background-color: #c47653; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
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

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        int restantesTmp = 1;
        try {
            org.example.services.ServiceReservation sr = new org.example.services.ServiceReservation();
            int placesPrises = sr.sommePlacesReserveesActives(e.getId());
            restantesTmp = e.getCapacite() - placesPrises;
        } catch (Exception ex) {
        }
        final int restantes = restantesTmp;

        Button btnDecouvrir = new Button(restantes <= 0 ? "COMPLET" : "Découvrir");
        if (restantes <= 0) {
            // Premium soft red/pink style for full events, but still clickable
            btnDecouvrir.setStyle(
                    "-fx-background-color: #fca5a5; -fx-text-fill: #7f1d1d; -fx-background-radius: 15; -fx-padding: 5 15; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btnDecouvrir.setStyle(
                    "-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        }
        
        final Evenement currentEvt = e;
        btnDecouvrir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!org.example.utils.SessionStore.isConnected()) {
                    new Alert(Alert.AlertType.WARNING,
                            "Veuillez vous connecter pour découvrir les détails de cet événement.").show();
                    switchScene(event, "/fxml/Login.fxml");
                    return;
                }

                // L'artisan peut ouvrir la popup pour voir "Modifier / Supprimer"
                // Le client peut ouvrir pour voir les détails (même si c'est complet, le bouton réserver sera grisé dans la popup)
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
            int restantes = e.getCapacite() - reservees;
            lblPopupPlaces.setText(restantes + " restantes");

            if (restantes <= 0) {
                btnReserver.setText("COMPLET");
                btnReserver.setDisable(true);
                btnReserver.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10; -fx-pref-width: 250;");
            } else {
                btnReserver.setText("Réserver ma place");
                btnReserver.setDisable(false);
                btnReserver.setStyle(
                        "-fx-background-color: #C4704B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10; -fx-pref-width: 250; -fx-cursor: hand;");
            }
        } catch (Exception ex) {
        }

        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                Image img = new Image(new File(e.getImage()).toURI().toString());
                ivPopupHeader.setImage(img);
                ivPopupGallery.setImage(img);
            } catch (Exception ex) {
            }
        }

        // S'assurer que le popup affiche les bons boutons selon le rôle
        updateRoleView();
        popupOverlay.setVisible(true);
    }

    @FXML
    void handleFermerPopup() {
        popupOverlay.setVisible(false);
        selectedEvenement = null;
    }

    @FXML
    void handleSupprimerEvenement() {
        if (selectedEvenement == null)
            return;
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
        if (selectedEvenement == null)
            return;
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
            java.util.List<Reservation> list;
            boolean estArtisan = SessionStore.isArtisan();

            if (estArtisan) {
                // L'Artisan voit TOUTES les réservations (pour les gérer)
                list = sr.listerTous();
                showArtisanReservationTable(list);
            } else {
                // Le Client ne voit que LES SIENNES (ou toutes pour la validation si demandé)
                // Pour coller au web et à la demande "identique", on filtre par user_id
                list = sr.listerParUtilisateur(SessionStore.getUserId());
                System.out.println("DEBUG: Affichage des réservations du client ID: " + SessionStore.getUserId());
                showClientTickets(list);
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
                    FXMLLoader groupLoader = new FXMLLoader(
                            getClass().getResource("/fxml/ArtisanReservationGroup.fxml"));
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
                    for (Reservation r : resList)
                        totalPlaces += r.getNbPlaces();

                    lblTitre.setText(e.getNom().toUpperCase());
                    lblSubtitle.setText((e.getDateDebut() != null ? e.getDateDebut().toLocalDate().toString() : "")
                            + "  " + e.getLieu().toUpperCase());
                    lblFilling.setText(totalPlaces + "/" + e.getCapacite());
                    pb.setProgress((double) totalPlaces / e.getCapacite());

                    if (e.getImage() != null && !e.getImage().isEmpty()) {
                        try {
                            File file = new File(e.getImage());
                            if (file.exists())
                                ivThumb.setImage(new Image(file.toURI().toString()));
                        } catch (Exception ex) {
                        }
                    }

                    // ADD AI STRATEGY BUTTON
                    Button btnAI = new Button("🧠 IA Stratégie");
                    btnAI.setStyle("-fx-background-color: #fdf2f2; -fx-text-fill: #c4704b; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 10px;");
                    final Evenement finalE = e;
                    btnAI.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            showAIStrategyPopup(finalE);
                        }
                    });

                    // ADD PDF EXPORT BUTTON
                    Button btnPDF = new Button("📄 Exporter PDF");
                    btnPDF.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 10px;");
                    btnPDF.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            try {
                                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                                fileChooser.setTitle("Enregistrer la liste des réservations");
                                fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                                String defaultFileName = "Reservations_" + finalE.getNom().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
                                fileChooser.setInitialFileName(defaultFileName);
                                
                                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                java.io.File file = fileChooser.showSaveDialog(currentStage);
                                
                                if (file != null) {
                                    org.example.services.PDFExportService pdfService = new org.example.services.PDFExportService();
                                    pdfService.exportReservationsToPDF(finalE, resList, file.getAbsolutePath());
                                    new Alert(Alert.AlertType.INFORMATION, "Le fichier PDF a été exporté avec succès !\n" + file.getAbsolutePath()).show();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'exportation PDF : " + ex.getMessage()).show();
                            }
                        }
                    });

                    // On l'ajoute au header (Le premier HBox de groupNode)
                    HBox headerTop = (HBox) groupNode.getChildren().get(0);
                    headerTop.getChildren().add(3, btnAI);
                    headerTop.getChildren().add(4, btnPDF);

                    // Ajouter les lignes
                    for (Reservation r : resList) {
                        final Reservation currentRes = r; // Fix pour l'erreur "non-final variable"

                        FXMLLoader rowLoader = new FXMLLoader(
                                getClass().getResource("/fxml/ArtisanReservationRow.fxml"));
                        HBox rowNode = rowLoader.load();

                        Label lblNom = (Label) rowNode.lookup("#lblNomClient");
                        Label lblRid = (Label) rowNode.lookup("#lblResId");
                        Label lblPl = (Label) rowNode.lookup("#lblPlaces");
                        Label lblHr = (Label) rowNode.lookup("#lblHeure");
                        Label lblEm = (Label) rowNode.lookup("#lblEmail");
                        Label lblTot = (Label) rowNode.lookup("#lblTotal");
                        Label lblSt = (Label) rowNode.lookup("#lblStatus");

                        // Mock des données client pour le CRUD
                        lblNom.setText(
                                currentRes.getUserId() != null ? "Client #" + currentRes.getUserId() : "Anonyme");
                        lblRid.setText("#RESV-" + currentRes.getId());
                        lblPl.setText(currentRes.getNbPlaces() + " places");
                        lblHr.setText(currentRes.getCreatedAt() != null
                                ? currentRes.getCreatedAt().toLocalTime().toString().substring(0, 5)
                                : "--:--");
                        lblEm.setText("client" + currentRes.getId() + "@example.tn");
                        lblTot.setText(String.format("%.2f",
                                currentRes.getNbPlaces() * (e.getPrix() != null ? e.getPrix().doubleValue() : 0.0))
                                + " DT");

                        lblSt.setText(currentRes.getStatut().equals("confirme") ? "Confirmé"
                                : (currentRes.getStatut().equals("annule") ? "Annulé" : "En attente"));

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
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });

                        btnD.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent ev) {
                                try {
                                    ServiceReservation sr = new ServiceReservation();
                                    sr.supprimer(currentRes.getId());
                                    handleGoToReservations(null);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });

                        vboxRows.getChildren().add(rowNode);
                    }

                    mainContainer.getChildren().add(groupNode);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
                    card.setStyle(
                            "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 10);");

                    // 1. EN-TÊTE NOIR (STYLE ARTISAN)
                    HBox header = new HBox(20);
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setPadding(new Insets(20));
                    header.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 20 20 0 0;");

                    ImageView iv = new ImageView();
                    iv.setFitHeight(70);
                    iv.setFitWidth(100);
                    iv.setPreserveRatio(false);
                    iv.setStyle("-fx-background-radius: 10;");
                    if (e.getImage() != null && !e.getImage().isEmpty()) {
                        File f = new File(e.getImage());
                        if (f.exists())
                            iv.setImage(new Image(f.toURI().toString()));
                    }

                    VBox evtInfo = new VBox(5);
                    Label lblTitle = new Label(e.getNom().toUpperCase());
                    lblTitle.setStyle(
                            "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-family: 'Serif'; -fx-font-weight: bold;");
                    Label lblMeta = new Label(
                            e.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "  "
                                    + e.getLieu().toUpperCase());
                    lblMeta.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px; -fx-font-weight: bold;");
                    evtInfo.getChildren().addAll(lblTitle, lblMeta);

                    Region spacerH = new Region();
                    HBox.setHgrow(spacerH, Priority.ALWAYS);

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

                    Label lblHeure = new Label(
                            r.getDateReservation().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
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
                    Label badge = new Label(
                            r.getStatut().substring(0, 1).toUpperCase() + r.getStatut().substring(1).toLowerCase());
                    badge.setMinWidth(100);
                    badge.setAlignment(Pos.CENTER);
                    if ("confirme".equalsIgnoreCase(r.getStatut())) {
                        badge.setStyle(
                                "-fx-background-color: #e6f7ef; -fx-text-fill: #2ecc71; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
                    } else if ("annule".equalsIgnoreCase(r.getStatut())) {
                        badge.setStyle(
                                "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #fef5ed; -fx-text-fill: #f39c12; -fx-padding: 8 20; -fx-background-radius: 20; -fx-font-weight: bold;");
                    }

                    // BOUTON ACTION (Voir Billet / Payer)
                    Button btnAction = new Button("👁 Billet");
                    btnAction.setMinWidth(Region.USE_PREF_SIZE);
                    btnAction.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: #2d2d2d; -fx-border-color: #e6e6e6; -fx-border-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
                    btnAction.setOnAction(ev -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
                            Parent root = loader.load();
                            TicketController controller = loader.getController();
                            controller.setReservationData(r, e);
                            Stage stage = new Stage();
                            stage.setTitle("Billet Artisanal - " + e.getNom());
                            stage.setScene(new Scene(root));
                            stage.show();

                            // Recharger la fenêtre des résas lors de la fermeture du ticket pour update le
                            // statut visuellement (si payé)
                            stage.setOnHidden(hiddenEvent -> {
                                Stage currentStage = (Stage) container.getScene().getWindow();
                                currentStage.close();
                                handleGoToReservations(null);
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur d'Ouverture");
                            alert.setHeaderText(null);
                            alert.setContentText(
                                    "Impossible d'ouvrir le billet : " + ex.getMessage() + "\n" + ex.toString());
                            alert.showAndWait();
                        }
                    });

                    row.getChildren().addAll(colIdentite, lblNbPl, lblHeure, colContact, lblTotal, badge, btnAction);

                    card.getChildren().addAll(header, row);
                    container.getChildren().add(card);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
        if (!org.example.utils.SessionStore.isConnected()) {
            new Alert(Alert.AlertType.WARNING, "Vous devez vous connecter pour réserver une place.").show();
            switchScene(event, "/fxml/Login.fxml");
            return;
        }

        if (org.example.utils.SessionStore.isArtisan() || org.example.utils.SessionStore.isAdmin()) {
            new Alert(Alert.AlertType.WARNING, "Seuls les clients peuvent effectuer une réservation.").show();
            return;
        }

        // Here we should normally pass the selected event to the NouvelleReservation
        // controller.
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

    private void showAIStrategyPopup(Evenement e) {
        org.example.services.SmartFillService smart = new org.example.services.SmartFillService();
        double prediction = smart.predictFillRate(e);
        Map<String, Integer> analysis = smart.calculateStrategicAnalysis(e, prediction);
        List<Map<String, String>> checklist = smart.getStrategicChecklist(e, analysis);
        Map<String, Double> elasticity = smart.calculatePriceElasticity(prediction);
        int successScore = smart.calculateSuccessScore(e, prediction);

        VBox root = new VBox(30);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #faf7f2;");

        // Header
        Label lblHeader = new Label("Centre de Performance Stratégique IA");
        lblHeader.setStyle("-fx-font-family: 'Serif'; -fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #3D3229;");
        Label lblEvent = new Label("Analyse cognitive pour : " + e.getNom());
        lblEvent.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox headerText = new VBox(5, lblHeader, lblEvent);

        Button btnClose = new Button("✖ Fermer");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9e8e82; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #e8ddd4; -fx-border-radius: 20; -fx-padding: 5 15;");
        btnClose.setOnMouseEntered(ev -> btnClose.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #e53e3e; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #fecaca; -fx-border-radius: 20; -fx-padding: 5 15;"));
        btnClose.setOnMouseExited(ev -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9e8e82; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: #e8ddd4; -fx-border-radius: 20; -fx-padding: 5 15;"));
        btnClose.setOnAction(ev -> {
            ((Stage) btnClose.getScene().getWindow()).close();
        });

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        headerBox.getChildren().addAll(headerText, btnClose);

        HBox scoreBox = new HBox(20);
        scoreBox.setAlignment(Pos.CENTER_LEFT);
        Label lblScore = new Label(successScore + "%");
        String scoreColor = successScore >= 75 ? "#48bb78" : (successScore >= 50 ? "#ed8936" : "#e53e3e");
        lblScore.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        Label lblHealth = new Label("SCORE DE SANTÉ GLOBAL");
        lblHealth.setStyle("-fx-text-fill: #9e8e82; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        VBox vScore = new VBox(5, lblHealth, lblScore);
        scoreBox.getChildren().add(vScore);

        // Piliers
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);

        addScoreToGrid(grid, "Attractivité Prix", analysis.get("price_score"), 0, 0);
        addScoreToGrid(grid, "Qualité Contenu", analysis.get("content_score"), 1, 0);
        addScoreToGrid(grid, "Opportunité Marché", analysis.get("timing_score"), 0, 1);
        addScoreToGrid(grid, "Portée Visibilité", analysis.get("reach_score"), 1, 1);

        // Checklist
        VBox checkContainer = new VBox(20);
        checkContainer.setStyle("-fx-background-color: white; -fx-padding: 35; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");
        
        HBox planHeader = new HBox(10);
        planHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblPlanIcon = new Label("🧠");
        lblPlanIcon.setStyle("-fx-font-size: 20px;");
        Label lblPlan = new Label("PLAN D'ACTION COGNITIF");
        lblPlan.setStyle("-fx-text-fill: #3D3229; -fx-font-weight: bold; -fx-font-size: 14px; -fx-letter-spacing: 1px;");
        planHeader.getChildren().addAll(lblPlanIcon, lblPlan);
        
        checkContainer.getChildren().add(planHeader);

        for (Map<String, String> item : checklist) {
            VBox taskBox = new VBox(8);
            taskBox.setStyle("-fx-background-color: #faf7f2; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #e8ddd4; -fx-border-radius: 12;");
            
            HBox taskTitleBox = new HBox(10);
            taskTitleBox.setAlignment(Pos.CENTER_LEFT);
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #C4704B; -fx-font-size: 18px; -fx-font-weight: bold;");
            Label t = new Label(item.get("task"));
            t.setStyle("-fx-text-fill: #3D3229; -fx-font-weight: bold; -fx-font-size: 13px;");
            taskTitleBox.getChildren().addAll(dot, t);
            
            Label d = new Label(item.get("detail"));
            d.setStyle("-fx-text-fill: #6b5b4f; -fx-font-size: 12px; -fx-padding: 0 0 0 20;");
            d.setWrapText(true);
            
            taskBox.getChildren().addAll(taskTitleBox, d);

            if (item.containsKey("actionCode")) {
                Button btnAction = new Button(item.get("actionLabel"));
                btnAction.setStyle("-fx-background-color: #3D3229; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 15; -fx-font-size: 11px; -fx-cursor: hand;");
                HBox actionBox = new HBox(btnAction);
                actionBox.setPadding(new Insets(5, 0, 0, 20));
                
                btnAction.setOnAction(ev -> {
                    try {
                        org.example.services.ServiceEvenement service = new org.example.services.ServiceEvenement();
                        if ("GENERATE_DESC".equals(item.get("actionCode"))) {
                            org.example.services.AIService ai = new org.example.services.AIService();
                            String newDesc = ai.generateDescription(e.getNom(), e.getTypeArt(), e.getTheme());
                            e.setDescription(newDesc);
                            service.modifier(e);
                            new Alert(Alert.AlertType.INFORMATION, "La description a été générée et mise à jour avec succès !").show();
                        } else if ("OPTIMIZE_PRICE".equals(item.get("actionCode"))) {
                            if (e.getPrix() != null) {
                                java.math.BigDecimal newPrice = e.getPrix().multiply(new java.math.BigDecimal("0.80"));
                                e.setPrix(newPrice);
                                service.modifier(e);
                                new Alert(Alert.AlertType.INFORMATION, "Le prix a été ajusté de -20% pour booster les ventes !").show();
                            }
                        } else if ("PROMOTE".equals(item.get("actionCode"))) {
                            new Alert(Alert.AlertType.INFORMATION, "Une campagne de visibilité automatisée a été planifiée sur les réseaux !").show();
                        }
                        
                        btnAction.setDisable(true);
                        btnAction.setText("✔ Appliqué");
                        btnAction.setStyle("-fx-background-color: #48bb78; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 15; -fx-font-size: 11px;");
                        handleRefreshListeEvenements();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).show();
                    }
                });
                taskBox.getChildren().add(actionBox);
            }

            checkContainer.getChildren().add(taskBox);
        }

        root.getChildren().addAll(headerBox, scoreBox, grid, checkContainer);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #faf7f2; -fx-background: #faf7f2;");

        Stage stage = new Stage();
        stage.setTitle("IA Stratégie - " + e.getNom());
        stage.setScene(new Scene(sp, 800, 900));
        stage.show();
    }

    private void addScoreToGrid(GridPane grid, String title, int score, int c, int r) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        HBox hb = new HBox();
        hb.setAlignment(Pos.CENTER_LEFT);
        Label lblT = new Label(title);
        lblT.setStyle("-fx-font-weight: bold; -fx-text-fill: #3D3229; -fx-font-size: 14px;");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        Label lblS = new Label(score + "%");
        String color = score >= 75 ? "#48bb78" : (score >= 50 ? "#ed8936" : "#e53e3e");
        lblS.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        hb.getChildren().addAll(lblT, space, lblS);

        // Web-like Flat Progress Bar
        StackPane track = new StackPane();
        track.setPrefHeight(8);
        track.setPrefWidth(280);
        track.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 10;");
        track.setAlignment(Pos.CENTER_LEFT);

        Region fill = new Region();
        fill.setPrefHeight(8);
        fill.setPrefWidth(280 * (score / 100.0));
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10;");

        track.getChildren().add(fill);
        
        box.getChildren().addAll(hb, track);
        grid.add(box, c, r);
    }

    @FXML
    void handleAIChat(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Chatbot.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("AfkArt AI Concierge");
            stage.setScene(new Scene(root));
            stage.setAlwaysOnTop(true);
            stage.show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur Chatbot: " + e.getMessage()).show();
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
