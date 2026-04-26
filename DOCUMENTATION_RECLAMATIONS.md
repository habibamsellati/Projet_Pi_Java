# Documentation Technique — Gestion des Réclamations AfkArt

## Table des matières

1. [Architecture générale](#1-architecture-générale)
2. [Modèle de données](#2-modèle-de-données)
3. [Fonctionnalité 1 — Filtre Bad Words](#3-fonctionnalité-1--filtre-bad-words)
4. [Fonctionnalité 2 — Résumé IA](#4-fonctionnalité-2--résumé-ia)
5. [Fonctionnalité 3 — Visioconférence Jitsi](#5-fonctionnalité-3--visioconférence-jitsi)
6. [Fonctionnalité 4 — Mail d'alerte Admin](#6-fonctionnalité-4--mail-dalerte-admin)
7. [Service Email partagé](#7-service-email-partagé)
8. [Flux complet d'une réclamation](#8-flux-complet-dune-réclamation)
9. [Choix techniques](#9-choix-techniques)

---

## 1. Architecture générale

```
┌─────────────────────────────────────────────────────────────┐
│                        COUCHE VUE (FXML)                    │
│  ReclamationsAdmin.fxml   ReclamationAdminCard.fxml         │
│  ReclamationsClient.fxml  ReclamationCard.fxml              │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    COUCHE CONTRÔLEUR                        │
│  ReclamationsAdminController   → liste, filtre, refresh     │
│  ReclamationAdminCardController → actions sur 1 réclamation │
│  ReclamationsClientController  → vue client                 │
│  ReclamationCardController     → carte client               │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    COUCHE SERVICE                           │
│  ServiceReclamation        → CRUD réclamations + réponses   │
│  BadWordService            → modération du texte            │
│  BadWordRepository         → accès BD table bad_words       │
│  ReclamationSummaryService → résumé heuristique local       │
│  ReclamationWarningService → surveillance + alerte email    │
│  EmailService              → envoi SMTP / preview console   │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    COUCHE DONNÉES (MySQL)                   │
│  reclamation   reponse_reclamation   bad_words              │
│  commentaire_moderation_flag   user                         │
└─────────────────────────────────────────────────────────────┘
```

**Pattern utilisé :** MVC (Model-View-Controller) avec séparation stricte des responsabilités.

**Thread model :**
- UI JavaFX → thread JavaFX Application Thread
- Surveillance réclamations → thread daemon séparé (ScheduledExecutorService)
- Traductions commentaires → CompletableFuture (thread pool)

---

## 2. Modèle de données

### Table `reclamation`
| Colonne | Type | Description |
|---|---|---|
| id | INT PK | Identifiant unique |
| titre | VARCHAR(255) | Titre de la réclamation (3-255 car.) |
| description | TEXT | Description détaillée (10-2000 car.) |
| image | VARCHAR | Chemin image optionnelle |
| statut | VARCHAR | en_attente / en_cours / resolu / rejete |
| date_creation | DATETIME | Date de création automatique |
| client_id | INT FK | Référence vers user |

### Table `reponse_reclamation`
| Colonne | Type | Description |
|---|---|---|
| id | INT PK | Identifiant unique |
| contenu | TEXT | Texte de la réponse (5-2000 car.) |
| date_reponse | DATETIME | Date de la réponse |
| reclamation_id | INT FK | Réclamation concernée |
| admin_id | INT FK | Admin qui a répondu |

### Table `bad_words`
| Colonne | Type | Description |
|---|---|---|
| id | INT PK | Identifiant unique |
| mot | VARCHAR(120) | Mot interdit (unique, utf8mb4) |
| langue | VARCHAR(10) | Langue (fr par défaut) |
| gravite | VARCHAR(10) | LEGER / MOYEN / GRAVE |
| ajoute_par | VARCHAR(120) | Nom de l'admin qui a ajouté |
| date_ajout | DATETIME | Date d'ajout |

### Table `commentaire_moderation_flag`
| Colonne | Type | Description |
|---|---|---|
| id | INT PK | Identifiant unique |
| commentaire_id | INT FK | Commentaire signalé |
| user_id | INT FK | Utilisateur concerné |
| mots_detectes | VARCHAR(500) | Mots détectés (CSV) |
| date_flag | DATETIME | Date du signalement |

---

## 3. Fonctionnalité 1 — Filtre Bad Words

### Objectif
Empêcher les utilisateurs de soumettre des commentaires ou descriptions contenant des mots offensants ou inappropriés.

### Classes impliquées

#### `BadWordRepository.java`
Couche d'accès aux données pour la table `bad_words`.

| Méthode | Description |
|---|---|
| `findAllWords()` | Retourne tous les mots interdits triés alphabétiquement |
| `addWord(word, langue, gravite, addedBy)` | Insère un nouveau mot interdit |
| `deleteWord(word)` | Supprime un mot interdit |
| `saveFlag(commentaireId, userId, csv)` | Enregistre un signalement de commentaire |
| `ensureSchema()` | Crée les tables `bad_words` et `commentaire_moderation_flag` si elles n'existent pas, et insère 5 mots par défaut |

**Initialisation automatique :** Au démarrage, `ensureSchema()` crée les tables et insère les mots par défaut : `idiot`, `nul`, `stupide`, `horrible`, `mauvais`.

#### `BadWordService.java`
Logique métier de modération.

**Enum `ModerationStrategy`**
```
BLOQUER  → refuse le texte si mot interdit détecté
CENSURER → remplace les mots interdits par des *** et accepte
SIGNALER → accepte le texte mais enregistre un flag en BD
```

**Classe interne `ModerationResult`**
```java
boolean allowed       // le texte est-il accepté ?
String  finalText     // texte final (censuré ou original)
List<String> detectedWords  // mots interdits trouvés
boolean flagged       // a-t-il été signalé ?
```

| Méthode | Description |
|---|---|
| `verifierCommentaire(texte)` | Point d'entrée principal — détecte les mots et applique la stratégie |
| `detectForbiddenWords(text, badWords)` | Détection par regex avec gestion des accents |
| `censurerTexte(text, detectedWords)` | Remplace chaque mot par `***` (longueur préservée) |
| `normalize(input)` | Supprime les accents et met en minuscule pour comparaison insensible |
| `ajouterBadWord(word, user)` | Ajoute un mot (réservé aux admins) |
| `supprimerBadWord(word, user)` | Supprime un mot (réservé aux admins) |

### Logique de détection

```
texte saisi
    │
    ▼
normalize() → suppression accents + minuscules
    │
    ▼
Pour chaque mot interdit :
    regex : (?<!\p{L})MOT(?!\p{L})
    → détection mot entier uniquement (pas de sous-chaîne)
    │
    ▼
Liste des mots détectés
    │
    ├── vide → ModerationResult(allowed=true, texte original)
    │
    └── non vide → selon stratégie :
            BLOQUER  → allowed=false
            CENSURER → allowed=true, texte censuré
            SIGNALER → allowed=true, flagged=true
```

**Exemple :**
- Texte : `"Ce produit est vraiment nul et idiot"`
- Normalisé : `"ce produit est vraiment nul et idiot"`
- Détectés : `["nul", "idiot"]`
- Stratégie BLOQUER → refus avec message d'erreur
- Stratégie CENSURER → `"Ce produit est vraiment *** et *****"`

### Intégration dans le projet

`BadWordService` est appelé dans :
- `ServiceCommentaire.publier()` → avant d'enregistrer un commentaire
- `ServiceReclamation.ajouterReclamation()` → avant d'enregistrer une réclamation
- `ServiceReclamation.modifierDerniereReponse()` → avant de modifier une réponse admin

---

## 4. Fonctionnalité 2 — Résumé IA

### Objectif
Aider l'admin à comprendre rapidement une réclamation sans lire tout le texte. Génère automatiquement un résumé, détecte l'urgence, la catégorie et recommande une action.

### Classe `ReclamationSummaryService.java`

**Choix technique important :** Résumé 100% local, sans appel API externe. Utilise des heuristiques (scoring de phrases, mots-clés, patterns) pour être fiable même sans connexion internet.

#### Enums

```java
Urgence  : BASSE | MOYENNE | ELEVEE | CRITIQUE
Categorie: LIVRAISON | PAIEMENT | QUALITE | COMPTE | SERVICE_CLIENT | AUTRE
```

#### Record `SummaryInsights`
```java
String shortSummary      // résumé court (1-2 phrases)
Urgence urgence          // niveau d'urgence calculé
Categorie categorie      // catégorie détectée
List<String> pointsCles  // points importants extraits
String prochaineAction   // action recommandée à l'admin
String delaiTraitement   // temps écoulé depuis création
```

#### Méthodes principales

| Méthode | Description |
|---|---|
| `analyzeReclamationOnly(reclamation)` | Analyse basée uniquement sur la description |
| `analyze(reclamation, reponses)` | Analyse complète avec historique des réponses |
| `buildReclamationOnlyDigest(reclamation)` | Génère le digest texte complet (pour copier-coller) |
| `buildAdminDigest(reclamation, reponses)` | Digest complet avec historique réponses |
| `generateAISummary(description)` | Génère le résumé court par scoring de phrases |

#### Algorithme de résumé (`generateAISummary`)

```
1. Découper le texte en phrases (split sur . ! ?)
2. Scorer chaque phrase :
   - Position 0 (première) → +2.0
   - Position 1 → +1.0
   - Contient un mot-clé métier → +2.0 par mot
   - Contient un nombre → +1.2
   - Longueur 40-220 caractères → +0.8
3. Garder les 2 phrases avec le score le plus élevé
4. Les réordonner dans l'ordre original
5. Si résultat trop proche de l'original → fallback 2 premières phrases
```

**Mots-clés métier utilisés pour le scoring :**
`probleme, erreur, livraison, remboursement, retard, defaut, panne, urgent, annulation, commande, paiement, casse, manquant, qualite, sav`

#### Algorithme de détection d'urgence

```
Score de départ : 0

+ 3 si "urgent", "bloque", "impossible"
+ 2 si "remboursement", "paiement"
+ 1 si "retard", "defaut", "casse"
+ 1 si le texte contient "!"
+ 1 si statut = "en_attente"
+ 3 si aucune réponse depuis >= 72h
+ 2 si aucune réponse depuis >= 24h

Score >= 7 → CRITIQUE
Score >= 5 → ELEVEE
Score >= 3 → MOYENNE
Score < 3  → BASSE
```

#### Détection de catégorie

Chaque catégorie a une liste de mots-clés. La catégorie avec le plus de correspondances dans la description gagne.

```
LIVRAISON    : livraison, retard, colis, transport, manquant
PAIEMENT     : paiement, remboursement, carte, facture, transaction
QUALITE      : defaut, casse, qualite, abime, endommag
COMPTE       : compte, connexion, mot de passe, profil, bloque
SERVICE_CLIENT: support, sav, reponse, service, assistance
AUTRE        : aucune correspondance
```

### Intégration dans l'interface

Dans `ReclamationAdminCardController.handleVoirResume()` :

```
Admin clique "Voir Résumé"
    │
    ▼
summaryService.analyzeReclamationOnly(reclamation)
    → SummaryInsights (résumé, urgence, catégorie, points, action)
    │
    ▼
summaryService.buildReclamationOnlyDigest(reclamation)
    → texte complet formaté
    │
    ▼
Affichage dans une Alert JavaFX avec :
    - Chips colorés (urgence + catégorie)
    - Résumé court
    - Points clés
    - Action recommandée
    - Zone texte copiable (digest complet)
    - Boutons "Copier résumé court" / "Copier digest complet"
```

---

## 5. Fonctionnalité 3 — Visioconférence Jitsi

### Objectif
Permettre à l'admin de démarrer une visioconférence avec le client concerné par une réclamation, directement depuis l'interface, sans installation d'application.

### Technologie choisie : Jitsi Meet

**Pourquoi Jitsi ?**
- Gratuit et open source
- Aucune installation côté client (navigateur suffit)
- Pas d'API key nécessaire
- URL unique par salle : `https://meet.jit.si/NOM_SALLE`

### Flux complet

```
Admin clique "Démarrer Meet"
    │
    ▼
Génération d'un identifiant de salle unique :
    roomId = "afkart-reclamation-{id_reclamation}-{8 chars UUID}"
    meetUrl = "https://meet.jit.si/" + roomId
    │
    ▼
Ouverture du navigateur par défaut (java.awt.Desktop)
    Desktop.getDesktop().browse(URI.create(meetUrl))
    │
    ▼
Récupération de l'email du client :
    reclamation.getClient().getEmail()
    │
    ├── email null/vide → showError("Email client introuvable")
    │
    └── email valide →
            emailService.envoyerInvitationMeetReclamation(
                emailClient, nomClient, titreReclamation, meetUrl
            )
            │
            ├── sent=true  → showInfo("Visio ouverte et invitation envoyée")
            └── sent=false → showError("Visio ouverte, email non envoyé")
```

### Méthode `handleDemarrerMeet()` dans `ReclamationAdminCardController`

```java
// Génération de la salle unique
String roomId = "afkart-reclamation-" + reclamation.getId()
              + "-" + UUID.randomUUID().toString().substring(0, 8);
String meetUrl = "https://meet.jit.si/" + roomId;

// Ouverture navigateur
Desktop.getDesktop().browse(URI.create(meetUrl));

// Envoi email invitation au client
emailService.envoyerInvitationMeetReclamation(emailClient, nomClient, titre, meetUrl);
```

### Template email d'invitation

L'email envoyé au client contient :
- En-tête vert avec titre "Invitation visioconférence"
- Message personnalisé avec le nom du client et le titre de la réclamation
- Bouton cliquable "Rejoindre la visioconférence" (lien direct)
- Lien texte en fallback si le bouton ne fonctionne pas
- Signature "Équipe AfkArt"

### Sécurité et contrôle d'accès

- Seul l'admin peut déclencher la visio (bouton visible uniquement dans `ReclamationAdminCard.fxml`)
- L'URL de salle est unique par réclamation + UUID aléatoire → impossible à deviner
- Le client reçoit le lien uniquement par email → accès contrôlé

---

## 6. Fonctionnalité 4 — Mail d'alerte Admin

### Objectif
Notifier automatiquement tous les admins par email quand une réclamation reste sans réponse pendant plus de 5 heures, pour éviter les oublis.

### Classe `ReclamationWarningService.java`

#### Constantes configurables
```java
SEUIL_HEURES = 5                    // délai avant alerte
INTERVALLE_VERIFICATION_MINUTES = 30 // fréquence de vérification
```

#### Cycle de vie du service

```
MainAPP.start()
    │
    ▼
warningService.demarrer()
    │
    ▼
ScheduledExecutorService (thread daemon)
    │
    ├── Délai initial : 1 minute
    │
    └── Puis toutes les 30 minutes :
            verifierEtAlerter()
    │
primaryStage.setOnCloseRequest()
    │
    ▼
warningService.arreter()
    → scheduler.shutdownNow()
```

**Thread daemon :** Le thread est marqué `daemon=true`, ce qui signifie qu'il s'arrête automatiquement quand la JVM se ferme, sans bloquer la fermeture de l'application.

#### Méthode `verifierEtAlerter()`

```
1. obtenirReclamationsNonTraitees()
   → SQL : réclamations en_attente/en_cours
           sans aucune réponse (LEFT JOIN IS NULL)
           depuis >= SEUIL_HEURES heures
   │
   ├── liste vide → log "Aucune réclamation" → fin
   │
   └── liste non vide →
           obtenirEmailsAdmins()
           → SQL : SELECT email FROM user WHERE role='ADMIN'
           │
           ├── aucun admin → log erreur → fin
           │
           └── pour chaque email admin :
                   buildWarningHtml(reclamations)
                   emailService.envoyerEmailWarning(email, subject, html)
```

#### Requête SQL de détection

```sql
SELECT r.id, r.titre, r.description, r.statut, r.date_creation,
       CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient,
       u.email AS emailClient,
       TIMESTAMPDIFF(HOUR, r.date_creation, NOW()) AS heures_ecoulees
FROM reclamation r
LEFT JOIN user u ON r.client_id = u.id
LEFT JOIN reponse_reclamation rep ON r.id = rep.reclamation_id
WHERE r.statut IN ('en_attente', 'en_cours')
  AND rep.id IS NULL                          -- aucune réponse
  AND TIMESTAMPDIFF(HOUR, r.date_creation, NOW()) >= 5
ORDER BY r.date_creation ASC
```

**Logique clé :** Le `LEFT JOIN ... IS NULL` sur `reponse_reclamation` garantit que seules les réclamations sans aucune réponse sont retournées.

#### Template email d'alerte

L'email envoyé aux admins contient :
- En-tête orange vif avec icône ⚠️
- Nombre de réclamations concernées
- Tableau HTML avec : ID, Titre (+ heures écoulées), Client, Statut coloré, Date création
- Encadré rouge "Action requise"
- Pied de page automatique

---

## 7. Service Email partagé

### Classe `EmailService.java`

Service central utilisé par toutes les fonctionnalités nécessitant un envoi d'email.

#### Configuration (`email.properties`)
```properties
mail.enabled=true
mail.preview=true
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.username=votre@gmail.com
email.smtp.password=MOT_DE_PASSE_APPLICATION_16_CHARS
email.from=votre@gmail.com
email.from.name=AfkArt
```

#### Méthodes publiques

| Méthode | Utilisée par |
|---|---|
| `envoyerConfirmationCommande(commande, email)` | ServiceCommande après validation commande |
| `envoyerInvitationMeetReclamation(email, nom, titre, url)` | ReclamationAdminCardController |
| `envoyerEmailWarning(email, subject, html)` | ReclamationWarningService |

#### Logique d'envoi

```
send(to, subject, html)
    │
    ├── smtpEnabled=true →
    │       sendViaSmtp() via Jakarta Mail
    │       STARTTLS sur port 587
    │       Timeout 10s connexion/lecture/écriture
    │       │
    │       ├── succès → log "Email envoyé à X"
    │       └── échec  → log erreur + printPreview() (fallback)
    │
    └── smtpEnabled=false →
            printPreview() → affiche l'email complet dans la console
```

**SMTP actif si :** `smtpUsername` non vide ET `smtpPassword` non vide ET password ≠ `VOTRE_MOT_DE_PASSE_APPLICATION`.

---

## 8. Flux complet d'une réclamation

```
CLIENT                          SYSTÈME                         ADMIN
  │                               │                               │
  │── Soumet réclamation ─────────▶│                               │
  │                               │ BadWordService.verifier()     │
  │                               │ → BLOQUER si mot interdit     │
  │                               │ → Enregistrement en BD        │
  │                               │                               │
  │                               │──── Notification admin ──────▶│
  │                               │     (liste réclamations)      │
  │                               │                               │
  │                               │                               │── Voir résumé
  │                               │◀── analyzeReclamationOnly() ──│
  │                               │    urgence + catégorie        │
  │                               │    points clés + action       │
  │                               │                               │
  │                               │                               │── Démarrer Meet
  │                               │    Génération roomId unique   │
  │◀── Email invitation Meet ─────│    Ouverture navigateur       │
  │    (lien Jitsi)               │                               │
  │                               │                               │
  │── Rejoint la visio ──────────▶│◀──────── Visio en direct ─────│
  │                               │                               │
  │                               │                               │── Ajouter réponse
  │                               │ BadWordService.verifier()     │
  │                               │ → validation réponse          │
  │                               │ → Enregistrement en BD        │
  │                               │                               │
  │                               │                               │── Changer statut
  │                               │    statut → "resolu"          │
  │                               │                               │
  │                               │                               │
  │         [Si 5h sans réponse]  │                               │
  │                               │ ReclamationWarningService     │
  │                               │ (thread daemon, toutes 30min) │
  │                               │──── Email alerte ────────────▶│
  │                               │     tableau réclamations      │
  │                               │     en attente                │
```

---

## 9. Choix techniques

### Pourquoi Jitsi Meet ?
- Aucune infrastructure serveur à maintenir
- Gratuit, open source, RGPD-friendly
- Fonctionne dans n'importe quel navigateur moderne
- URL unique générée côté Java avec UUID → pas de collision possible

### Pourquoi un résumé heuristique local ?
- Pas de dépendance à une API externe (Hugging Face, OpenAI)
- Fonctionne hors ligne
- Résultats déterministes et prévisibles
- Latence nulle (calcul instantané)
- Pas de coût d'API

### Pourquoi un thread daemon pour la surveillance ?
- Ne bloque pas la fermeture de l'application
- S'arrête automatiquement avec la JVM
- `ScheduledExecutorService` est plus fiable que `Timer` pour les tâches périodiques
- Première vérification après 1 minute (pas immédiatement au démarrage)

### Pourquoi Jakarta Mail (Angus) ?
- Implémentation de référence de Jakarta Mail
- Support STARTTLS natif
- Timeouts configurables (connexion, lecture, écriture)
- Fallback console si SMTP non configuré → développement sans configuration

### Stratégie de modération Bad Words
- Normalisation Unicode (suppression accents) → détecte "idiot" et "ïdïot"
- Regex avec word boundaries `(?<!\p{L})MOT(?!\p{L})` → évite les faux positifs dans les mots composés
- 3 stratégies configurables (BLOQUER / CENSURER / SIGNALER) → adaptable selon le contexte
- Table BD extensible → les admins peuvent ajouter/supprimer des mots sans recompiler

### Architecture MVC
- Les contrôleurs ne contiennent aucune logique SQL
- Les services ne connaissent pas JavaFX
- Les modèles sont de simples POJOs
- Facilite les tests unitaires et la maintenance
