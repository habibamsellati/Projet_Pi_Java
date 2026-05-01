package org.example.controllers;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Insets;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.example.models.Article;
import org.example.models.Commentaire;
import org.example.models.User;
import org.example.services.ServiceArticle;
import org.example.services.ServiceCommentaire;
import org.example.services.CommentTranslationService;
import org.example.services.ServiceReactionArticle;
import org.example.services.ServiceReactionCommentaire;
import org.example.utils.I18nManager;
import org.example.utils.SessionManager;

import java.io.File;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String STYLE_INACTIF    = "-fx-background-color:#ffffff;-fx-text-fill:#666;-fx-background-radius:20;-fx-padding:8 18;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:#e0e0e0;-fx-border-radius:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.06),4,0,0,1);";
    private static final String STYLE_LIKE_ON    = "-fx-background-color:linear-gradient(to right,#ff6b6b,#ee5a24);-fx-text-fill:#fff;-fx-background-radius:20;-fx-padding:8 18;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:transparent;-fx-border-radius:20;-fx-font-weight:bold;-fx-effect:dropshadow(three-pass-box,rgba(238,90,36,0.35),6,0,0,2);";
    private static final String STYLE_DISLIKE_ON = "-fx-background-color:linear-gradient(to right,#4facfe,#00f2fe);-fx-text-fill:#fff;-fx-background-radius:20;-fx-padding:8 18;-fx-font-size:13;-fx-cursor:hand;-fx-border-color:transparent;-fx-border-radius:20;-fx-font-weight:bold;-fx-effect:dropshadow(three-pass-box,rgba(79,172,254,0.35),6,0,0,2);";
    private static final String EMOJI_LIKE    = "\u2764\uFE0F";   // ❤️
    private static final String EMOJI_DISLIKE = "\uD83D\uDC94";   // 💔
    private static final String CHECK_MARK    = "\u2713";

    // Emojis professionnels et expressifs pour les commentaires
    private static final String[][] COMMENT_EMOJIS = {
        {"\uD83D\uDE0D", "Adorable"},      // 😍
        {"\uD83D\uDE04", "Super"},         // 😄
        {"\uD83E\uDD29", "Incroyable"},    // 🤩
        {"\uD83D\uDC4F", "Bravo"},         // 👏
        {"\uD83D\uDC4D", "J'approuve"},    // 👍
        {"\u2764\uFE0F", "J'adore"},       // ❤️
        {"\uD83D\uDD25", "Chaud"},         // 🔥
        {"\uD83C\uDF1F", "Excellent"},     // 🌟
        {"\uD83D\uDCAF", "Parfait"},       // 💯
        {"\uD83E\uDD14", "Intéressant"},   // 🤔
        {"\uD83D\uDE22", "Triste"},        // 😢
        {"\uD83D\uDE21", "Déçu"},          // 😡
    };

    // â”€â”€ Services â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private final ServiceCommentaire         serviceCommentaire         = new ServiceCommentaire();
    private final CommentTranslationService  commentTranslationService  = new CommentTranslationService();
    private final ServiceReactionArticle     serviceReaction            = new ServiceReactionArticle();
    private final ServiceReactionCommentaire serviceReactionCommentaire = new ServiceReactionCommentaire();
    private final ServiceArticle             serviceArticle             = new ServiceArticle();

    // Cache simple: commentaireId|lang -> texte traduit
    private final Map<String, String> commentTranslationCache = new ConcurrentHashMap<>();

    private Article article;

    // â”€â”€â”€ Initialisation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    public void initialize() {
        // Remplir le selecteur de langue
        langCombo.getItems().addAll(
            I18nManager.get("lang.fr"),
            I18nManager.get("lang.en"),
            I18nManager.get("lang.ar")
        );
        // Selectionner la langue courante
        syncLangCombo();
        initialiserEmojiBar();
    }

    public void setArticle(Article article) {
        this.article = article;
        appliquerTraductions();
        rafraichirEntete();
        rafraichirReactions();
        rafraichirCommentaires();
        afficherArticlesSimilaires();
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
        emojiBar.setHgap(6);
        emojiBar.setVgap(6);

        for (String[] entry : COMMENT_EMOJIS) {
            String emoji = entry[0];
            String label = entry[1];

            Button btn = new Button(emoji);
            btn.setFocusTraversable(false);
            btn.setTooltip(new Tooltip(label));

            // Forcer la police Segoe UI Emoji (Windows) pour affichage couleur
            btn.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 20));
            btn.setStyle(
                "-fx-background-color:#ffffff;" +
                "-fx-background-radius:12;" +
                "-fx-border-color:#ede8e3;" +
                "-fx-border-radius:12;" +
                "-fx-padding:5 9;" +
                "-fx-cursor:hand;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.07),3,0,0,1);"
            );

            btn.setOnMouseEntered(e -> {
                btn.setStyle(
                    "-fx-background-color:#fff8f2;" +
                    "-fx-background-radius:12;" +
                    "-fx-border-color:#c8a882;" +
                    "-fx-border-radius:12;" +
                    "-fx-padding:5 9;" +
                    "-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.18),8,0,0,3);" +
                    "-fx-scale-x:1.2;-fx-scale-y:1.2;"
                );
                btn.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 20));
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle(
                    "-fx-background-color:#ffffff;" +
                    "-fx-background-radius:12;" +
                    "-fx-border-color:#ede8e3;" +
                    "-fx-border-radius:12;" +
                    "-fx-padding:5 9;" +
                    "-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.07),3,0,0,1);"
                );
                btn.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 20));
            });

            btn.setOnAction(ev -> insererEmojiDansCommentaire(emoji));
            emojiBar.getChildren().add(btn);
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

        // Forcer la police emoji couleur sur Windows
        javafx.scene.text.Font emojiFont = javafx.scene.text.Font.font("Segoe UI Emoji", 14);
        btnLike.setFont(emojiFont);
        btnDislike.setFont(emojiFont);

        User user = SessionManager.getCurrentUser();
        String reactionActuelle = "";
        if (user != null) {
            try { reactionActuelle = serviceReaction.getReactionActuelle(article.getId(), user.getId()); }
            catch (SQLException ignored) {}
        }

        if ("like".equals(reactionActuelle)) {
            btnLike.setStyle(STYLE_LIKE_ON);
            btnLike.setText(EMOJI_LIKE + "  J'aime  " + likes + "  " + CHECK_MARK);
        } else {
            btnLike.setStyle(STYLE_INACTIF);
            btnLike.setText(EMOJI_LIKE + "  J'aime  " + likes);
        }

        if ("dislike".equals(reactionActuelle)) {
            btnDislike.setStyle(STYLE_DISLIKE_ON);
            btnDislike.setText(EMOJI_DISLIKE + "  Je n'aime pas  " + dislikes + "  " + CHECK_MARK);
        } else {
            btnDislike.setStyle(STYLE_INACTIF);
            btnDislike.setText(EMOJI_DISLIKE + "  Je n'aime pas  " + dislikes);
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
        String styleBase      = "-fx-background-color:#ffffff;-fx-text-fill:#666;-fx-background-radius:16;-fx-padding:5 14;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:#e0e0e0;-fx-border-radius:16;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.05),3,0,0,1);";
        String styleLikeOn    = "-fx-background-color:linear-gradient(to right,#ff6b6b,#ee5a24);-fx-text-fill:#fff;-fx-background-radius:16;-fx-padding:5 14;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:transparent;-fx-border-radius:16;-fx-font-weight:bold;-fx-effect:dropshadow(three-pass-box,rgba(238,90,36,0.3),5,0,0,2);";
        String styleDislikeOn = "-fx-background-color:linear-gradient(to right,#4facfe,#00f2fe);-fx-text-fill:#fff;-fx-background-radius:16;-fx-padding:5 14;-fx-font-size:12;-fx-cursor:hand;-fx-border-color:transparent;-fx-border-radius:16;-fx-font-weight:bold;-fx-effect:dropshadow(three-pass-box,rgba(79,172,254,0.3),5,0,0,2);";

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
        javafx.scene.text.Font emojiFont = javafx.scene.text.Font.font("Segoe UI Emoji", 13);
        btnCLike.setFont(emojiFont);
        btnCDislike.setFont(emojiFont);

        if ("like".equals(reaction)) {
            btnCLike.setStyle(styleLikeOn);
            btnCLike.setText(EMOJI_LIKE + "  " + likes + "  " + CHECK_MARK);
            btnCDislike.setStyle(styleBase);
            btnCDislike.setText(EMOJI_DISLIKE + "  " + dislikes);
        } else if ("dislike".equals(reaction)) {
            btnCLike.setStyle(styleBase);
            btnCLike.setText(EMOJI_LIKE + "  " + likes);
            btnCDislike.setStyle(styleDislikeOn);
            btnCDislike.setText(EMOJI_DISLIKE + "  " + dislikes + "  " + CHECK_MARK);
        } else {
            btnCLike.setStyle(styleBase);
            btnCLike.setText(EMOJI_LIKE + "  " + likes);
            btnCDislike.setStyle(styleBase);
            btnCDislike.setText(EMOJI_DISLIKE + "  " + dislikes);
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

    // ── Articles similaires ───────────────────────────────────────────────────

    private void afficherArticlesSimilaires() {
        if (article == null || vboxScroll == null) return;

        try {
            List<Article> similaires = serviceArticle.getSimilarArticles(article, 3);
            if (similaires.isEmpty()) return;

            Label titre = new Label("Articles similaires");
            titre.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#5D4037;-fx-padding:18 0 8 0;");
            titre.setId("similaires-titre");

            HBox cartes = new HBox(12);
            cartes.setPadding(new Insets(0, 0, 16, 0));
            cartes.setId("similaires-cartes");

            for (Article sim : similaires) {
                cartes.getChildren().add(buildSimilaireCard(sim));
            }

            // Supprimer l'ancienne section si elle existe déjà (rechargement)
            vboxScroll.getChildren().removeIf(n ->
                    "similaires-titre".equals(n.getId()) || "similaires-cartes".equals(n.getId()));

            vboxScroll.getChildren().addAll(titre, cartes);

        } catch (SQLException e) {
            System.err.println("[ArticlesSimilaires] Erreur: " + e.getMessage());
        }
    }

    private VBox buildSimilaireCard(Article sim) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color:#FFF8F0;-fx-background-radius:10;-fx-border-color:#E6D5C3;-fx-border-radius:10;-fx-cursor:hand;");

        // Image
        ImageView iv = new ImageView();
        iv.setFitWidth(180);
        iv.setFitHeight(110);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        Image img = loadImageSafely(sim.getImageUrl());
        if (img != null) iv.setImage(img);
        Rectangle clip = new Rectangle(180, 110);
        clip.setArcWidth(8); clip.setArcHeight(8);
        iv.setClip(clip);

        // Catégorie
        Label lblCat = new Label(sim.getCategorie() != null ? sim.getCategorie() : "");
        lblCat.setStyle("-fx-font-size:11;-fx-text-fill:#A1887F;-fx-background-color:#F3E5DC;-fx-background-radius:6;-fx-padding:2 6;");

        // Titre
        Label lblTitre = new Label(sim.getTitre());
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(180);
        lblTitre.setStyle("-fx-font-weight:bold;-fx-font-size:13;-fx-text-fill:#4E342E;");

        // Prix
        Label lblPrix = new Label(sim.getPrix() == null ? "" : String.format("%.2f DT", sim.getPrix()));
        lblPrix.setStyle("-fx-font-size:12;-fx-text-fill:#8D5E3C;-fx-font-weight:bold;");

        card.getChildren().addAll(iv, lblCat, lblTitre, lblPrix);

        // Clic → ouvrir la page détail de cet article similaire
        card.setOnMouseClicked(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/fxml/ArticleCommentairesView.fxml"));
                javafx.scene.Parent root = loader.load();
                ArticleCommentairesController ctrl = loader.getController();
                ctrl.setArticle(sim);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle(sim.getTitre());
                stage.setScene(new javafx.scene.Scene(root, 920, 700));
                stage.show();
            } catch (Exception ex) {
                showError("Impossible d'ouvrir l'article : " + ex.getMessage());
            }
        });

        // Hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#F3E5DC;-fx-background-radius:10;-fx-border-color:#8D5E3C;-fx-border-radius:10;-fx-cursor:hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#FFF8F0;-fx-background-radius:10;-fx-border-color:#E6D5C3;-fx-border-radius:10;-fx-cursor:hand;"));

        return card;
    }

    private Image loadImageSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        List<String> candidates = new ArrayList<>();
        String raw = imagePath.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:") || raw.startsWith("jar:"))
            candidates.add(raw);
        File f = new File(raw);
        if (f.exists()) candidates.add(f.toURI().toString());
        String cp = raw.startsWith("/") ? raw : "/" + raw;
        java.net.URL res = getClass().getResource(cp);
        if (res != null) candidates.add(res.toExternalForm());
        for (String c : candidates) {
            try { Image img = new Image(c, false); if (!img.isError()) return img; }
            catch (Exception ignored) {}
        }
        return null;
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR,   msg).showAndWait(); }
    private void showWarn(String msg)  { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
}
