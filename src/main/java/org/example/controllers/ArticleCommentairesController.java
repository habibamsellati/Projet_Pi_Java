package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.User;
import org.example.services.ServiceCommentaire;
import org.example.services.CommentTranslationService;
import org.example.services.ServiceReactionArticle;
import org.example.services.ServiceReactionCommentaire;
import org.example.utils.I18nManager;
import org.example.utils.SessionManager;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ArticleCommentairesController {

    // â”€â”€ FXML â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label    lblPageTitle;
    @FXML private Label    lblTitreArticle;
    @FXML private Label    lblPrixArticle;
    @FXML private Label    lblDateArticle;
    @FXML private Label    lblAucunCommentaire;
    @FXML private Label    lblAddComment;
    @FXML private VBox     vboxCommentaires;
    @FXML private VBox     vboxScroll;
    @FXML private VBox     vboxFormCommentaire;
    @FXML private FlowPane emojiBar;
    @FXML private TextArea taNouveauCommentaire;
    @FXML private Button   btnLike;
    @FXML private Button   btnDislike;
    @FXML private Button   btnPublier;
    @FXML private Button   btnFermer;
    @FXML private ComboBox<String> langCombo;

    // â”€â”€ Styles boutons article â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static final String STYLE_INACTIF   = "-fx-background-color:#f5f5f5;-fx-text-fill:#555;-fx-background-radius:22;-fx-padding:9 20;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:#ddd;-fx-border-radius:22;";
    private static final String STYLE_LIKE_ON   = "-fx-background-color:#fce4ec;-fx-text-fill:#c62828;-fx-background-radius:22;-fx-padding:9 20;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:#c62828;-fx-border-radius:22;-fx-font-weight:bold;";
    private static final String STYLE_DISLIKE_ON= "-fx-background-color:#e3f2fd;-fx-text-fill:#1565c0;-fx-background-radius:22;-fx-padding:9 20;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:#1565c0;-fx-border-radius:22;-fx-font-weight:bold;";
    private static final String EMOJI_LIKE = "\u2764\uFE0F";
    private static final String EMOJI_DISLIKE = "\uD83D\uDC4E";
    private static final String CHECK_MARK = "\u2713";
    private static final String[] COMMENT_EMOJIS = {
            "\uD83D\uDE0A", // 😊
            "\uD83D\uDE04", // 😄
            "\uD83D\uDE0D", // 😍
            "\u2764\uFE0F", // ❤️
            "\uD83D\uDC4D", // 👍
            "\uD83D\uDC4F", // 👏
            "\uD83E\uDD29", // 🤩
            "\uD83D\uDE21"  // 😡
    };

    // â”€â”€ Services â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private final ServiceCommentaire         serviceCommentaire         = new ServiceCommentaire();
    private final CommentTranslationService  commentTranslationService  = new CommentTranslationService();
    private final ServiceReactionArticle     serviceReaction            = new ServiceReactionArticle();
    private final ServiceReactionCommentaire serviceReactionCommentaire = new ServiceReactionCommentaire();

    // Cache simple: commentaireId|lang -> texte traduit
    private final Map<String, String> commentTranslationCache = new ConcurrentHashMap<>();

    private Article article;

    // â”€â”€â”€ Initialisation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    public void initialize() {
        // Remplir le sÃ©lecteur de langue
        langCombo.getItems().addAll(
            I18nManager.get("lang.fr"),
            I18nManager.get("lang.en"),
            I18nManager.get("lang.ar")
        );
        // SÃ©lectionner la langue courante
        syncLangCombo();
        initialiserEmojiBar();
    }

    public void setArticle(Article article) {
        this.article = article;
        appliquerTraductions();
        rafraichirEntete();
        rafraichirReactions();
        rafraichirCommentaires();
    }

    // â”€â”€â”€ Changement de langue â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    void handleChangerLangue() {
        int idx = langCombo.getSelectionModel().getSelectedIndex();
        Locale nouvelleLocale = switch (idx) {
            case 1  -> I18nManager.ENGLISH;
            case 2  -> I18nManager.ARABIC;
            default -> I18nManager.FRENCH;
        };

        I18nManager.setLocale(nouvelleLocale);
        appliquerTraductions();

        // RTL pour l'arabe
        NodeOrientation orientation = I18nManager.isRTL()
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT;
        if (vboxScroll != null)         vboxScroll.setNodeOrientation(orientation);
        if (vboxFormCommentaire != null) vboxFormCommentaire.setNodeOrientation(orientation);
        if (lblPageTitle != null)        lblPageTitle.getParent().setNodeOrientation(orientation);

        rafraichirReactions();
        rafraichirCommentaires();
    }

    /** Met Ã  jour tous les textes fixes de la vue selon la langue active. */
    private void appliquerTraductions() {
        if (lblPageTitle      != null) lblPageTitle.setText(I18nManager.get("article.title"));
        if (lblAucunCommentaire != null) lblAucunCommentaire.setText(I18nManager.get("comment.none"));
        if (lblAddComment     != null) lblAddComment.setText(I18nManager.get("comment.add.title"));
        if (taNouveauCommentaire != null) taNouveauCommentaire.setPromptText(I18nManager.get("comment.placeholder"));
        if (btnPublier        != null) btnPublier.setText(I18nManager.get("comment.publish"));
        if (btnFermer         != null) btnFermer.setText(I18nManager.get("comment.close"));
        initialiserEmojiBar();
    }

    private void initialiserEmojiBar() {
        if (emojiBar == null) return;

        emojiBar.getChildren().clear();
        for (String emoji : COMMENT_EMOJIS) {
            Button emojiButton = new Button(emoji);
            emojiButton.setFocusTraversable(false);
            emojiButton.setTooltip(new Tooltip(I18nManager.get("comment.emoji.insert")));
            emojiButton.setStyle("-fx-background-color:#fff8f0;-fx-text-fill:#5D4037;-fx-background-radius:14;-fx-padding:6 10;-fx-font-size:16;-fx-cursor:hand;-fx-border-color:#e6d5c3;-fx-border-radius:14;");
            emojiButton.setOnAction(event -> insererEmojiDansCommentaire(emoji));
            emojiBar.getChildren().add(emojiButton);
        }
    }

    private void insererEmojiDansCommentaire(String emoji) {
        if (taNouveauCommentaire == null || emoji == null || emoji.isBlank()) return;

        int start = Math.max(0, taNouveauCommentaire.getSelection().getStart());
        int end = Math.max(0, taNouveauCommentaire.getSelection().getEnd());
        String current = taNouveauCommentaire.getText() == null ? "" : taNouveauCommentaire.getText();
        String prefix = current.substring(0, Math.min(start, current.length()));
        String suffix = current.substring(Math.min(end, current.length()));
        String separator = prefix.isEmpty() || Character.isWhitespace(prefix.charAt(prefix.length() - 1)) ? "" : " ";
        String updated = prefix + separator + emoji + " " + suffix;

        taNouveauCommentaire.setText(updated.stripLeading());
        int caret = Math.min(updated.length(), prefix.length() + separator.length() + emoji.length() + 1);
        taNouveauCommentaire.positionCaret(caret);
        taNouveauCommentaire.requestFocus();
    }

    private void syncLangCombo() {
        if (langCombo == null) return;
        String lang = I18nManager.getLocale().getLanguage();
        langCombo.getSelectionModel().select(
            "ar".equals(lang) ? 2 : "en".equals(lang) ? 1 : 0
        );
    }

    // â”€â”€â”€ Like / Dislike article â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML void handleLike()    { voter(ServiceReactionArticle.TypeReaction.LIKE); }
    @FXML void handleDislike() { voter(ServiceReactionArticle.TypeReaction.DISLIKE); }

    private void voter(ServiceReactionArticle.TypeReaction demande) {
        if (article == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showWarn(I18nManager.get("error.login.required"));
            return;
        }
        try {
            serviceReaction.voter(article.getId(), user.getId(), demande);
            rafraichirReactionsDepuisBDD();
        } catch (SQLException e) {
            showError(I18nManager.get("error.reaction", e.getMessage()));
        }
    }

    private void rafraichirReactionsDepuisBDD() throws SQLException {
        try (var ps = org.example.utils.MyDatabase.getInstance().getConnection()
                .prepareStatement("SELECT likes, dislikes FROM article WHERE id = ?")) {
            ps.setInt(1, article.getId());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    article.setLikes(rs.getInt("likes"));
                    article.setDislikes(rs.getInt("dislikes"));
                }
            }
        }
        rafraichirReactions();
    }

    private void rafraichirReactions() {
        if (btnLike == null || btnDislike == null || article == null) return;

        int likes    = article.getLikes();
        int dislikes = article.getDislikes();

        User user = SessionManager.getCurrentUser();
        String reactionActuelle = "";
        if (user != null) {
            try { reactionActuelle = serviceReaction.getReactionActuelle(article.getId(), user.getId()); }
            catch (SQLException ignored) {}
        }

        if ("like".equals(reactionActuelle)) {
            btnLike.setStyle(STYLE_LIKE_ON);
            btnLike.setText(I18nManager.get("btn.like.active", likes));
        } else {
            btnLike.setStyle(STYLE_INACTIF);
            btnLike.setText(I18nManager.get("btn.like", likes));
        }

        if ("dislike".equals(reactionActuelle)) {
            btnDislike.setStyle(STYLE_DISLIKE_ON);
            btnDislike.setText(I18nManager.get("btn.dislike.active", dislikes));
        } else {
            btnDislike.setStyle(STYLE_INACTIF);
            btnDislike.setText(I18nManager.get("btn.dislike", dislikes));
        }
    }

    // â”€â”€â”€ Commentaires â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    void handlePublierCommentaire() {
        if (article == null) { showError(I18nManager.get("error.article.missing")); return; }
        User user = SessionManager.getCurrentUser();
        if (user == null) { showWarn(I18nManager.get("error.login.required")); return; }

        try {
            Commentaire c = new Commentaire();
            c.setContenu(taNouveauCommentaire.getText());
            c.setArticle(article);
            c.setAuteur(user);
            serviceCommentaire.publier(c, user);
            rafraichirCommentaires();
            taNouveauCommentaire.clear();
        } catch (IllegalArgumentException e) {
            showWarn(e.getMessage());
        } catch (SQLException e) {
            showError(I18nManager.get("error.comment.save", e.getMessage()));
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    void handleFermer() {
        if (taNouveauCommentaire.getScene() != null)
            taNouveauCommentaire.getScene().getWindow().hide();
    }

    private void rafraichirEntete() {
        if (article == null) {
            lblTitreArticle.setText(I18nManager.get("article.unknown"));
            lblPrixArticle.setText(I18nManager.get("article.price.unknown"));
            lblDateArticle.setText(I18nManager.get("article.date.unknown"));
            return;
        }
        lblTitreArticle.setText(article.getTitre() == null ? I18nManager.get("article.unknown") : article.getTitre());
        lblPrixArticle.setText(article.getPrix() == null
                ? I18nManager.get("article.price.unknown")
                : I18nManager.get("article.price") + " " + String.format("%.2f DT", article.getPrix()));

        if (article.getDatePublication() != null) {
            String pattern = I18nManager.isRTL() ? "dd/MM/yyyy HH:mm" : "dd MMM yyyy HH:mm";
            String date = article.getDatePublication().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern(pattern));
            lblDateArticle.setText(I18nManager.get("article.date") + " " + date);
        } else {
            lblDateArticle.setText(I18nManager.get("article.date.unknown"));
        }
    }

    private void rafraichirCommentaires() {
        vboxCommentaires.getChildren().clear();
        if (article == null) { updateEtatVide(); return; }

        try {
            List<Commentaire> commentaires = serviceCommentaire.afficherParArticle(article);
            for (Commentaire c : commentaires) addCommentCard(c);
        } catch (SQLException e) {
            showError(I18nManager.get("error.comment.load", e.getMessage()));
        }
        updateEtatVide();
    }

    private void addCommentCard(Commentaire commentaire) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("article-card", "comment-card");
        if (I18nManager.isRTL()) card.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        User auteur = commentaire.getAuteur();
        String auteurTexte = auteur == null ? I18nManager.get("comment.author.unknown")
                : auteur.getNomComplet() != null && !auteur.getNomComplet().isBlank() ? auteur.getNomComplet()
                : auteur.getEmail() != null ? auteur.getEmail()
                : I18nManager.get("comment.author.unknown");

        Label lblAuteur = new Label(auteurTexte);
        lblAuteur.setStyle("-fx-font-weight:bold;-fx-text-fill:#5D4037;");

        Label lblTexte = new Label(commentaire.getContenu());
        lblTexte.setWrapText(true);
        lblTexte.setStyle("-fx-text-fill:#4E342E;-fx-font-size:13;");

        // Bouton traduire par commentaire
        HBox translateBar = new HBox(8);
        MenuButton btnTraduire = new MenuButton(I18nManager.get("comment.translate"));
        btnTraduire.setStyle("-fx-background-color:#eee;-fx-text-fill:#444;-fx-background-radius:14;-fx-padding:4 10;-fx-font-size:12;");

        MenuItem itemFr = new MenuItem("FR");
        MenuItem itemEn = new MenuItem("EN");
        MenuItem itemAr = new MenuItem("AR");
        MenuItem itemOrigine = new MenuItem(I18nManager.get("comment.translate.original"));

        itemFr.setOnAction(e -> traduireCommentaire(commentaire, lblTexte, btnTraduire, "fr"));
        itemEn.setOnAction(e -> traduireCommentaire(commentaire, lblTexte, btnTraduire, "en"));
        itemAr.setOnAction(e -> traduireCommentaire(commentaire, lblTexte, btnTraduire, "ar"));
        itemOrigine.setOnAction(e -> {
            lblTexte.setText(commentaire.getContenu());
            btnTraduire.setText(I18nManager.get("comment.translate"));
        });

        btnTraduire.getItems().addAll(itemFr, itemEn, itemAr, itemOrigine);
        translateBar.getChildren().add(btnTraduire);

        // â”€â”€ Boutons rÃ©action commentaire â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        HBox reactions = new HBox(8);
        String styleBase     = "-fx-background-color:#f5f5f5;-fx-text-fill:#666;-fx-background-radius:14;-fx-padding:4 12;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:#e0e0e0;-fx-border-radius:14;";
        String styleLikeOn   = "-fx-background-color:#fce4ec;-fx-text-fill:#c62828;-fx-background-radius:14;-fx-padding:4 12;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:#c62828;-fx-border-radius:14;-fx-font-weight:bold;";
        String styleDislikeOn= "-fx-background-color:#e3f2fd;-fx-text-fill:#1565c0;-fx-background-radius:14;-fx-padding:4 12;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:#1565c0;-fx-border-radius:14;-fx-font-weight:bold;";

        Button btnCLike    = new Button(EMOJI_LIKE + " " + commentaire.getLikes());
        Button btnCDislike = new Button(EMOJI_DISLIKE + " " + commentaire.getDislikes());

        User currentUser = SessionManager.getCurrentUser();
        String reactionInit = "";
        if (currentUser != null && commentaire.getId() > 0) {
            try { reactionInit = serviceReactionCommentaire.getReactionActuelle(commentaire.getId(), currentUser.getId()); }
            catch (SQLException ignored) {}
        }
        appliquerStyleBtnCommentaire(btnCLike, btnCDislike, reactionInit,
                commentaire.getLikes(), commentaire.getDislikes(), styleBase, styleLikeOn, styleDislikeOn);

        btnCLike.setOnAction(e -> voterCommentaire(commentaire,
                ServiceReactionCommentaire.TypeReaction.LIKE, btnCLike, btnCDislike, styleBase, styleLikeOn, styleDislikeOn));
        btnCDislike.setOnAction(e -> voterCommentaire(commentaire,
                ServiceReactionCommentaire.TypeReaction.DISLIKE, btnCLike, btnCDislike, styleBase, styleLikeOn, styleDislikeOn));

        reactions.getChildren().addAll(btnCLike, btnCDislike);

        // â”€â”€ Boutons modif/suppr â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        HBox actions = new HBox(8);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        actions.getChildren().add(spacer);

        if (canModifyCommentaire(commentaire)) {
            Button btnM = new Button(I18nManager.get("comment.modify"));
            btnM.getStyleClass().add("btn-modifier");
            btnM.setOnAction(ev -> handleModifierCommentaire(commentaire));
            actions.getChildren().add(btnM);
        }
        if (canSupprimerCommentaire(commentaire)) {
            Button btnS = new Button(I18nManager.get("comment.delete"));
            btnS.getStyleClass().add("btn-supprimer");
            btnS.setOnAction(ev -> handleSupprimerCommentaire(commentaire));
            actions.getChildren().add(btnS);
        }

        card.getChildren().addAll(lblAuteur, lblTexte, translateBar, reactions, actions);
        vboxCommentaires.getChildren().add(card);
    }

    private void traduireCommentaire(Commentaire commentaire, Label lblTexte, MenuButton btnTraduire, String targetLang) {
        String cacheKey = commentaire.getId() + "|" + targetLang;
        String cached = commentTranslationCache.get(cacheKey);
        if (cached != null) {
            lblTexte.setText(cached);
            btnTraduire.setText(I18nManager.get("comment.translate.done", targetLang.toUpperCase(Locale.ROOT)));
            return;
        }

        btnTraduire.setDisable(true);
        btnTraduire.setText(I18nManager.get("comment.translate.loading"));

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return commentTranslationService.translate(commentaire.getContenu(), targetLang);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((translated, throwable) -> Platform.runLater(() -> {
                    btnTraduire.setDisable(false);
                    if (throwable != null || translated == null || translated.isBlank()) {
                        btnTraduire.setText(I18nManager.get("comment.translate"));
                        showWarn(I18nManager.get("error.comment.translate"));
                        return;
                    }

                    commentTranslationCache.put(cacheKey, translated);
                    lblTexte.setText(translated);
                    btnTraduire.setText(I18nManager.get("comment.translate.done", targetLang.toUpperCase(Locale.ROOT)));
                }));
    }

    private void voterCommentaire(Commentaire commentaire, ServiceReactionCommentaire.TypeReaction demande,
                                   Button btnCLike, Button btnCDislike,
                                   String styleBase, String styleLikeOn, String styleDislikeOn) {
        User user = SessionManager.getCurrentUser();
        if (user == null) { showWarn(I18nManager.get("error.login.required")); return; }
        try {
            serviceReactionCommentaire.voter(commentaire.getId(), user.getId(), demande);
            try (var ps = org.example.utils.MyDatabase.getInstance().getConnection()
                    .prepareStatement("SELECT likes, dislikes FROM commentaire WHERE id = ?")) {
                ps.setInt(1, commentaire.getId());
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        commentaire.setLikes(rs.getInt("likes"));
                        commentaire.setDislikes(rs.getInt("dislikes"));
                    }
                }
            }
            String nouvelleReaction = serviceReactionCommentaire.getReactionActuelle(commentaire.getId(), user.getId());
            appliquerStyleBtnCommentaire(btnCLike, btnCDislike, nouvelleReaction,
                    commentaire.getLikes(), commentaire.getDislikes(), styleBase, styleLikeOn, styleDislikeOn);
        } catch (SQLException ex) {
            showError(I18nManager.get("error.reaction", ex.getMessage()));
        }
    }

    private void appliquerStyleBtnCommentaire(Button btnCLike, Button btnCDislike, String reaction,
                                               int likes, int dislikes,
                                               String styleBase, String styleLikeOn, String styleDislikeOn) {
        if ("like".equals(reaction)) {
            btnCLike.setStyle(styleLikeOn);   btnCLike.setText(EMOJI_LIKE + " " + likes + " " + CHECK_MARK);
            btnCDislike.setStyle(styleBase);  btnCDislike.setText(EMOJI_DISLIKE + " " + dislikes);
        } else if ("dislike".equals(reaction)) {
            btnCLike.setStyle(styleBase);     btnCLike.setText(EMOJI_LIKE + " " + likes);
            btnCDislike.setStyle(styleDislikeOn); btnCDislike.setText(EMOJI_DISLIKE + " " + dislikes + " " + CHECK_MARK);
        } else {
            btnCLike.setStyle(styleBase);     btnCLike.setText(EMOJI_LIKE + " " + likes);
            btnCDislike.setStyle(styleBase);  btnCDislike.setText(EMOJI_DISLIKE + " " + dislikes);
        }
    }

    private void handleModifierCommentaire(Commentaire commentaire) {
        TextInputDialog dialog = new TextInputDialog(commentaire.getContenu());
        dialog.setTitle(I18nManager.get("dialog.modify.title"));
        dialog.setHeaderText(I18nManager.get("dialog.modify.header"));
        dialog.setContentText(I18nManager.get("dialog.modify.content"));
        dialog.showAndWait().ifPresent(nouveauContenu -> {
            User currentUser = SessionManager.getCurrentUser();
            try {
                serviceCommentaire.modifier(commentaire, nouveauContenu, currentUser);
                rafraichirCommentaires();
            } catch (IllegalArgumentException ex) {
                showWarn(ex.getMessage());
            } catch (SQLException ex) {
                showError(I18nManager.get("error.comment.modify", ex.getMessage()));
            }
        });
    }

    private void handleSupprimerCommentaire(Commentaire commentaire) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                I18nManager.get("comment.confirm.delete"));
        confirm.setHeaderText(I18nManager.get("comment.confirm.header"));
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                User currentUser = SessionManager.getCurrentUser();
                try {
                    serviceCommentaire.supprimer(commentaire, currentUser, article);
                    commentaire.clearRepliesCascade();
                    rafraichirCommentaires();
                } catch (Exception e) {
                    showError(I18nManager.get("error.comment.delete", e.getMessage()));
                }
            }
        });
    }

    // â”€â”€â”€ Autorisations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private boolean canModifyCommentaire(Commentaire c)  { return serviceCommentaire.peutModifier(c, SessionManager.getCurrentUser()); }
    private boolean canSupprimerCommentaire(Commentaire c) { return serviceCommentaire.peutSupprimer(c, SessionManager.getCurrentUser(), article); }

    private void updateEtatVide() {
        boolean empty = vboxCommentaires.getChildren().isEmpty();
        lblAucunCommentaire.setVisible(empty);
        lblAucunCommentaire.setManaged(empty);
    }

    // â”€â”€â”€ Alertes helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR,   msg).showAndWait(); }
    private void showWarn(String msg)  { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
}
