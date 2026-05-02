# Compose - Explication complete gestion article

## 1) Vue d'ensemble du programme

Le module **gestion article** suit une architecture JavaFX classique en couches:

- **UI (FXML + Controller)**: capte les actions utilisateur, met a jour l'affichage.
- **Service metier**: applique les regles (validation, droits, moderation, reaction, traduction, similarite).
- **Repository / SQL**: persiste les donnees en MySQL.
- **Model**: objets metier (`Article`, `Commentaire`, `User`, etc.).
- **Utils transverses**: session (`SessionManager`), i18n (`I18nManager`), connexion DB (`MyDatabase`).

Flux general:
1. L'utilisateur clique dans une vue JavaFX.
2. Le controller appelle un service.
3. Le service valide + execute la logique metier.
4. Le repository/service SQL met a jour la base.
5. Le controller recharge et re-affiche l'etat.

---

## 2) Architecture technique (classes principales)

### Controllers
- `src/main/java/org/example/controllers/AfficherArticlesController.java`
  - Liste, recherche, tri, publication article.
- `src/main/java/org/example/controllers/ArticleCardController.java`
  - Carte article, ouvrir detail, modifier/supprimer selon droits.
- `src/main/java/org/example/controllers/ArticleCommentairesController.java`
  - Coeur des features demandees: emojis, likes/dislikes, traduction, commentaires, articles similaires.

### Services
- `src/main/java/org/example/services/ServiceArticle.java`
  - CRUD article + algo articles similaires (`getSimilarArticles`).
- `src/main/java/org/example/services/ServiceCommentaire.java`
  - Publication/modification/suppression commentaire + moderation bad words.
- `src/main/java/org/example/services/BadWordService.java`
  - Detection, censure, blocage, signalement.
- `src/main/java/org/example/services/BadWordRepository.java`
  - Tables `bad_words` + `commentaire_moderation_flag`.
- `src/main/java/org/example/services/ServiceReactionArticle.java`
  - Like/dislike article (toggle + switch).
- `src/main/java/org/example/services/ServiceReactionCommentaire.java`
  - Like/dislike commentaire (meme logique).
- `src/main/java/org/example/services/CommentTranslationService.java`
  - Traduction externe (MyMemory puis fallback Google endpoint public).
- `src/main/java/org/example/services/PersonalizedMessageService.java`
  - Generation de message personnalise (IA + fallback template) pour les commandes.

### Repository / DB
- `src/main/java/org/example/services/CommentaireRepository.java`
  - Persistance commentaire.
- `src/main/java/org/example/utils/MyDatabase.java`
  - Singleton connexion MySQL, resiliente et configurable (`db.properties`/env).

### Utils
- `src/main/java/org/example/utils/SessionManager.java`
  - Utilisateur connecte courant.
- `src/main/java/org/example/utils/I18nManager.java`
  - Langues FR/EN/AR + support RTL.

### Vue principale detail article
- `src/main/resources/fxml/ArticleCommentairesView.fxml`
  - Header article, boutons like/dislike, liste commentaires, barre emoji, formulaire.

---

## 3) Fonctionnalite BAD WORD (comment ca marche)

### Point d'entree
- Publication commentaire via `ArticleCommentairesController.handlePublierCommentaire()`.
- Appel metier: `ServiceCommentaire.publier(...)`.

### Logique metier
Dans `ServiceCommentaire.publier(...)`:
1. Verifie utilisateur connecte.
2. Verifie coherence auteur/commentaire/article.
3. Valide contenu via `Commentaire.setContenu()` (5..255 caracteres).
4. Lance moderation: `badWordService.verifierCommentaire(contenu)`.
5. Applique la strategie:
   - `BLOQUER`: refuse publication (`IllegalArgumentException`).
   - `CENSURER`: remplace mots detectes par `***` puis publie.
   - `SIGNALER`: publie mais marque comme flag.
6. Sauvegarde commentaire (`CommentaireRepository.save`).
7. Si signale, enregistre trace `commentaire_moderation_flag`.

### Detection technique
`BadWordService.detectForbiddenWords(...)`:
- Normalise texte (lowercase + suppression accents via `Normalizer`).
- Match avec regex bornes de mots: `(?<!\p{L})mot(?!\p{L})`.
- Retourne liste unique des mots detectes.

### Administration des mots interdits
- `BadWordService.ajouterBadWord(...)` et `supprimerBadWord(...)` reserve ADMIN.
- Persistance via `BadWordRepository`.
- Initialisation auto table + seed mots par defaut (`idiot`, `nul`, etc.).

### Choix techniques
- Moderation centralisee dans le service (pas dans UI).
- Normalisation unicode pour robustesse accents.
- Strategies configurables pour adapter politique produit.

---

## 4) Fonctionnalite EMOJI (saisie commentaire)

### Point d'entree UI
`ArticleCommentairesController.initialiserEmojiBar()`:
- Construit dynamiquement des boutons a partir de `COMMENT_EMOJIS`.
- Ajoute tooltip metier (ex: "Adorable", "Bravo").
- Style visuel + hover.
- Police forcee `Segoe UI Emoji` (Windows) pour rendu couleur.

### Insertion dans le texte
`insererEmojiDansCommentaire(String emoji)`:
- Recupere selection/caret du `TextArea`.
- Insere emoji au bon index.
- Gere espace avant/apres pour ne pas coller les mots.
- Replace le curseur proprement.

### Logique metier
- C'est une aide de saisie UX.
- Le contenu final reste soumis a validation + bad words au moment publication.

### Choix techniques
- Liste d'emojis codifiee localement (rapide, stable, offline).
- Insertion cote client, sans appel reseau.

---

## 5) Fonctionnalite LIKE / DISLIKE

## 5.1 Article

### Point d'entree
- `handleLike()` / `handleDislike()` dans `ArticleCommentairesController`.
- Appel: `voter(TypeReaction)` -> `ServiceReactionArticle.voter(...)`.

### Regles metier (ServiceReactionArticle)
- 1 user = 1 reaction active par article.
- Recliquer meme bouton = annuler vote (toggle).
- Cliquer oppose = switch (retire ancien puis ajoute nouveau).

### Persistance
- Table `article_reaction(article_id, user_id, type)` cle primaire composite.
- Colonnes agregats dans `article.likes` et `article.dislikes` incrementees/decrementees.
- `GREATEST(0, col-1)` evite valeurs negatives.

### UI
- Rechargement des compteurs depuis DB (`rafraichirReactionsDepuisBDD`).
- Style actif/inactif + check mark pour reaction courante.

## 5.2 Commentaire

Meme logique dans:
- `ServiceReactionCommentaire.voter(...)`
- table `commentaire_reaction`
- colonnes `commentaire.likes` / `commentaire.dislikes`
- affichage dans `addCommentCard(...)` et `appliquerStyleBtnCommentaire(...)`.

### Choix techniques
- Stocker relation vote (source de verite) + colonnes compteurs (performance lecture UI).
- Toggle/switch cote service pour garantir coherence.

---

## 6) Fonctionnalite TRADUCTION

### Deux niveaux de traduction
1. **Traduction de l'interface** (labels/boutons): `I18nManager` + `messages_fr/en/ar.properties`.
2. **Traduction du texte de commentaire**: `CommentTranslationService`.

### 6.1 Traduction UI (i18n)
Dans `ArticleCommentairesController`:
- `initialize()` remplit `langCombo` (FR/EN/AR).
- `handleChangerLangue()` change locale via `I18nManager.setLocale(...)`.
- `appliquerTraductions()` recharge textes fixes.
- Si arabe: applique orientation RTL (`NodeOrientation.RIGHT_TO_LEFT`).

### 6.2 Traduction contenu commentaire
Dans `addCommentCard(...)`:
- Chaque commentaire a un `MenuButton` Traduire (FR/EN/AR/original).
- Appel `traduireCommentaire(...)`.

`traduireCommentaire(...)`:
1. Verifie cache local `commentTranslationCache` (cle `commentId|lang`).
2. Si absent: desactive bouton + affiche "Traduction...".
3. Lance tache async (`CompletableFuture.supplyAsync`).
4. Service: `CommentTranslationService.translate(text, lang)`.
5. MAJ UI thread JavaFX via `Platform.runLater`.
6. Stocke traduction en cache.

### Service externe
`CommentTranslationService`:
- Priorite 1: API MyMemory.
- Fallback: endpoint Google `translate.googleapis.com`.
- Extraction via regex de la reponse JSON.
- Validation basique (`isUsableTranslation`).

### Choix techniques
- Async pour ne pas bloquer UI.
- Cache in-memory pour eviter appels repetes.
- Fallback multi-provider pour resilence reseau.

---

## 7) Fonctionnalite MESSAGE PERSONNALISE

> Important: dans ce projet, cette fonctionnalite est principalement liee au module **commande**, pas directement au module article.

### Classes impliquees
- `src/main/java/org/example/services/PersonalizedMessageService.java`
- `src/main/java/org/example/services/ServiceCommande.java` (integration via `generarMessagePersonnalise(...)`)

### Logique
1. `ServiceCommande` collecte contexte commande (nom client, titres articles, total, numero).
2. Appelle `messageService.generateOrderConfirmationMessage(...)`.
3. `PersonalizedMessageService` tente appel IA Hugging Face (Mistral).
4. Si cle absente, erreur API, parsing invalide: fallback templates locaux.
5. Stocke dans commande:
   - `message_personnalise`
   - `ai_generated` (boolean provenance)

### Choix techniques
- Pattern robustesse: "AI first, deterministic fallback".
- Nettoyage output IA (`nettoyerReponseIA`) pour eviter texte parasite.
- Timeout court pour ne pas bloquer traitement commande.

### Lecture architecture
- C'est un service transverse reutilisable.
- Aujourd'hui branche sur commandes; pourrait etre reutilise pour notifications article/commentaire.

---

## 8) Fonctionnalite ARTICLES SIMILAIRES

### Point d'entree
`ArticleCommentairesController.setArticle(...)` appelle `afficherArticlesSimilaires()`.

### Algo metier
Dans `ServiceArticle.getSimilarArticles(article, limit)`:
1. Priorite 1: recupere articles meme categorie, excluant article courant, tri date desc, limite N.
2. Priorite 2: si pas assez, complete avec articles recents hors deja pris.
3. Retourne liste finale (max `limit`, ici 3).

### Affichage
`afficherArticlesSimilaires()`:
- Cree section "Articles similaires" dynamiquement.
- Construit chaque mini-carte via `buildSimilaireCard(...)`:
  - image
  - categorie
  - titre
  - prix
- Click sur carte: ouvre une nouvelle fenetre `ArticleCommentairesView` sur l'article selectionne.

### Choix techniques
- Heuristique simple et efficace (meme categorie + recence).
- Cout SQL faible, pas de moteur de recommandation externe.
- UX immediate dans l'ecran detail.

---

## 9) Logique metier transversale (droits, validation, securite)

### Auth/session
- `SessionManager.getCurrentUser()` utilise partout pour verifier connexion avant action sensible.

### Roles
- Publication commentaire: user connecte obligatoire.
- Modification commentaire: auteur client uniquement (`ServiceCommentaire.peutModifier`).
- Suppression commentaire: auteur client OU artisan proprietaire article (`peutSupprimer`).
- Gestion bad words (add/delete): ADMIN seulement.

### Validation
- `Commentaire.setContenu`: 5..255 chars.
- `ServiceArticle.validerArticle`: titre/contenu/prix/categorie/image.
- Reactions: ids > 0.

### Integrite donnees
- FK en base (`ON DELETE CASCADE`) pour reactions/comments/flags.
- Compteurs proteges contre negatif.

---

## 10) Schema simplifie des donnees utilisees

- `article`:
  - `id`, `titre`, `contenu`, `date`, `categorie`, `prix`, `image`, `likes`, `dislikes`, `artisan_id`, `user_id`
- `commentaire`:
  - `id`, `contenu`, `datepub`, `likes`, `dislikes`, `article_id`, `user_id`, `parent_id`
- `article_reaction`:
  - `article_id`, `user_id`, `type`
- `commentaire_reaction`:
  - `commentaire_id`, `user_id`, `type`
- `bad_words`:
  - `mot`, `langue`, `gravite`, `ajoute_par`, `date_ajout`
- `commentaire_moderation_flag`:
  - `commentaire_id`, `user_id`, `mots_detectes`, `date_flag`
- `commande` (hors article mais lie au message personnalise):
  - `message_personnalise`, `ai_generated`, etc.

---

## 11) Pourquoi ces choix techniques (lecture architecture)

1. **Separation des responsabilites**
   - Controllers = orchestration UI
   - Services = logique metier
   - Repositories = SQL/persistance

2. **Performance UX**
   - Colonnes compteurs likes/dislikes pre-calculees.
   - Cache traduction en memoire.
   - Actions async pour appels reseau.

3. **Robustesse**
   - Fallback DB schema (`ensureSchema`) et API externes (traduction/message IA).
   - Validations metier centralisees.

4. **Evolutivite**
   - Strategies moderation bad words parametrables.
   - Service message personnalise reutilisable.
   - Similarite extensible (tags, embeddings, historique utilisateur).

---

## 12) Limites actuelles et ameliorations conseillees

### Limites
- Traduction commentaire depend APIs publiques (quota/stabilite variables).
- Similarite uniquement categorie + recence (pas de scoring semantique).
- Reactions gerent coherence applicative, mais sans transaction explicite multi-etapes.
- Certains textes de boutons like/dislike sont hardcodes en FR dans le controller.

### Ameliorations
1. Ajouter transactions SQL autour des operations vote (insert/update + compteur).
2. Uniformiser i18n de tous les textes (y compris "J'aime", "Je n'aime pas", "Articles similaires").
3. Ajouter tests unitaires/services pour moderation, vote toggle, algo similarite.
4. Ajouter circuit breaker/cache persistant pour traduction externe.
5. Etendre similarite avec poids sur titre/contenu/tags/prix.

---

## 13) Resume operationnel (comment le programme fonctionne concretement)

- Un artisan publie un article via `AfficherArticlesController` -> `ServiceArticle`.
- Un utilisateur ouvre le detail via `ArticleCardController.handleLireSuite()`.
- Le detail (`ArticleCommentairesController`) charge:
  - infos article
  - compteurs reactions
  - commentaires
  - section articles similaires
  - outils UX (emoji + traduction + langue)
- Quand l'utilisateur commente:
  - validation + moderation bad words via `ServiceCommentaire`/`BadWordService`
  - persistence en DB
  - re-affichage immediat
- Quand il vote like/dislike:
  - service reaction applique toggle/switch
  - DB mise a jour
  - UI rafraichie
- Quand il traduit:
  - appel async service traduction
  - cache resultat
  - UI mise a jour sans blocage

Ce design donne un module article fonctionnel, lisible, et deja pret pour des extensions IA/recommandation plus avancees.

