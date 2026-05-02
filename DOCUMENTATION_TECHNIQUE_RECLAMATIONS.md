# Documentation Technique — Module Gestion des Réclamations (AfkArt)

> Projet : projetpijava — Application JavaFX / MySQL  
> Date : Avril 2026

---

## Table des matières

1. [Architecture générale](#1-architecture-générale)
2. [Modèle de données](#2-modèle-de-données)
3. [Fonctionnalité 1 — Filtre Bad Words (Mots interdits)](#3-fonctionnalité-1--filtre-bad-words)
4. [Fonctionnalité 2 — Résumé intelligent (IA heuristique)](#4-fonctionnalité-2--résumé-intelligent)
5. [Fonctionnalité 3 — Visioconférence Jitsi Meet](#5-fonctionnalité-3--visioconférence-jitsi-meet)
6. [Fonctionnalité 4 — Email d'alerte pour l'admin](#6-fonctionnalité-4--email-dalerte-pour-ladmin)
7. [EmailService — Couche transversale d'envoi d'email](#7-emailservice--couche-transversale-denvoi-demail)
8. [Flux d'interaction complet](#8-flux-dinteraction-complet)
9. [Choix techniques et justifications](#9-choix-techniques-et-justifications)

---

## 1. Architecture générale

L'application suit un pattern **MVC classique** :

```
┌─────────────────────────────────────────────────────────┐
│                    COUCHE VUE (FXML)                    │
│  ReclamationsAdmin.fxml  /  ReclamationAdminCard.fxml   │
└─────────────────────┬───────────────────────────────────┘
                      │ JavaFX Events
┌─────────────────────▼───────────────────────────────────┐
│                 COUCHE CONTROLLER                        │
│  ReclamationsAdminController                            │
│  ReclamationAdminCardController  ◄── point d'entrée UI  │
│  ReclamationsClientController                           │
│  AjouterReclamationController                           │
└──────┬──────────┬───────────┬────────────┬──────────────┘
       │          │           │            │
┌──────▼──┐ ┌────▼───┐ ┌─────▼──────┐ ┌──▼────────────┐
│Service  │ │BadWord │ │Reclamation │ │Reclamation    │
│Reclama- │ │Service │ │Summary     │ │Warning        │
│tion     │ │        │ │Service     │ │Service        │
└──────┬──┘ └────┬───┘ └────────────┘ └──────┬────────┘
       │         │                            │
┌──────▼─────────▼────────────────────────────▼────────┐
│                  COUCHE DONNÉES                       │
│  BadWordRepository  /  MyDatabase (Singleton JDBC)    │
│  Tables MySQL : reclamation, reponse_reclamation,     │
│                 bad_words, commentaire_moderation_flag│
└───────────────────────────────────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │       EmailService          │
              │ (Jakarta Mail / SMTP Gmail) │
              └─────────────────────────────┘
```

**Packages impliqués :**

| Package | Rôle |
|---|---|
| `org.example.models` | Entités métier (Reclamation, ReponseReclamation, User…) |
| `org.example.services` | Logique métier, accès données, envoi email |
| `org.example.controllers` | Gestion des événements UI JavaFX |
| `org.example.utils` | MyDatabase (connexion), SessionManager |

---

## 2. Modèle de données

### Classe `Reclamation` (modèle principal)

```
Reclamation
├── id              : int
├── titre           : String  (3–255 caractères, obligatoire)
├── description     : String  (10–2000 caractères, optionnel)
├── imageUrl        : String  (extension image valide, optionnel)
├── statut          : String  [en_attente | en_cours | resolu | rejete]
├── dateCreation    : Timestamp
├── clientId        : int     (FK → user.id)
└── client          : User    (chargé en JOIN pour l'admin)
```

**Enum interne `StatutReclamation` :**  
`EN_ATTENTE` → `EN_COURS` → `RESOLU` / `REJETE`

La validation des champs est intégrée dans les setters du modèle (Fail-Fast), **et** redoublée dans `ServiceReclamation.validerReclamation()`.

### Tables SQL utilisées

| Table | Colonnes clés |
|---|---|
| `reclamation` | id, titre, description, image, statut, date_creation, client_id |
| `reponse_reclamation` | id, contenu, date_reponse, reclamation_id, admin_id |
| `bad_words` | id, mot, langue, gravite, ajoute_par, date_ajout |
| `commentaire_moderation_flag` | id, commentaire_id, user_id, mots_detectes, date_flag |

> **Note :** `BadWordRepository.ensureSchema()` crée automatiquement les tables `bad_words` et `commentaire_moderation_flag` au premier démarrage via `CREATE TABLE IF NOT EXISTS`.

---

## 3. Fonctionnalité 1 — Filtre Bad Words

### Objectif
Empêcher les clients de soumettre des réclamations ou des commentaires contenant des mots offensants ou inappropriés.

### Classes impliquées

#### `BadWordRepository` — Accès base de données
- **`ensureSchema()`** : Crée les tables `bad_words` et `commentaire_moderation_flag` si elles n'existent pas. Insère 5 mots interdits par défaut (`idiot`, `nul`, `stupide`, `horrible`, `mauvais`).
- **`findAllWords()`** : Retourne la liste de tous les mots interdits (`SELECT mot FROM bad_words ORDER BY mot ASC`).
- **`addWord(word, langue, gravite, addedBy)`** : Insère un nouveau mot interdit (retourne `false` si doublon grâce à la contrainte `UNIQUE`).
- **`deleteWord(word)`** : Supprime un mot interdit.
- **`saveFlag(commentaireId, userId, detectedWordsCsv)`** : Enregistre un signalement dans `commentaire_moderation_flag`.

#### `BadWordService` — Logique de modération
- **Enum `ModerationStrategy`** : Définit 3 comportements possibles :
  - `BLOQUER` : Rejette le texte → exception levée côté service
  - `CENSURER` : Remplace les mots interdits par des `***`
  - `SIGNALER` : Laisse passer le texte mais pose un drapeau (`flagged = true`)
- **`verifierCommentaire(texte)`** : Méthode principale.
  1. Charge la liste des mots interdits depuis la BDD.
  2. Appelle `detectForbiddenWords()`.
  3. Selon la stratégie, retourne un `ModerationResult`.
- **`ModerationResult`** : Record contenant :
  - `allowed` (bool) : le texte est-il autorisé ?
  - `finalText` (String) : texte final (censuré ou original)
  - `detectedWords` (List) : liste des mots détectés
  - `flagged` (bool) : signalé pour review admin ?

#### Algorithme de détection (`detectForbiddenWords`)
```
Pour chaque mot interdit :
  1. Normaliser le texte et le mot (NFD → suppression accents → minuscules)
  2. Construire un pattern regex : (?<!\p{L})MOT(?!\p{L})
     → détecte le mot uniquement en tant que mot entier (pas dans un mot plus long)
  3. Rechercher le pattern dans le texte normalisé
  4. Si trouvé → ajouter à la liste des mots détectés
```

La **normalisation Unicode (NFD)** est cruciale : elle permet de détecter `idiot` même si l'utilisateur écrit `idiôt` ou `IDIOT`.

#### Censure (`censurerTexte`)
Remplace chaque mot détecté par `***` (minimum 3 étoiles, ou autant que la longueur du mot).  
Utilise `Pattern.compile("(?i)(?<!\p{L})MOT(?!\p{L})")` pour être insensible à la casse.

#### Intégration dans `ServiceReclamation`
```java
// Appelé avant chaque INSERT ou UPDATE de réclamation
private void appliquerModerationDescription(Reclamation r) throws SQLException {
    r.setDescription(modererTexteOuLever(r.getDescription(), "description de réclamation"));
}

private String modererTexteOuLever(String texte, String contexte) throws SQLException {
    BadWordService.ModerationResult result = badWordService.verifierCommentaire(texte);
    if (!result.isAllowed()) {
        throw new IllegalArgumentException("Le " + contexte + " contient des mots interdits: " + ...);
    }
    return result.getFinalText(); // texte potentiellement censuré
}
```

**Stratégie par défaut :** `BLOQUER` → si un mot interdit est détecté, une `IllegalArgumentException` est levée et l'UI affiche un message d'erreur à l'utilisateur.

#### Gestion admin des mots interdits
- `ajouterBadWord(word, currentUser)` : Vérifie que `currentUser` est ADMIN, puis insère le mot.
- `supprimerBadWord(word, currentUser)` : Idem.
- `listerBadWords()` : Liste tous les mots.
- `requireAdmin()` : Garde-fou → lève une exception si l'utilisateur n'est pas `Role.ADMIN`.

---

## 4. Fonctionnalité 2 — Résumé intelligent

### Objectif
Aider l'admin à traiter rapidement les réclamations en générant automatiquement :
- Un **résumé court** de la description
- Un **niveau d'urgence** (BASSE → CRITIQUE)
- Une **catégorie probable** (LIVRAISON, PAIEMENT, QUALITE, COMPTE…)
- Des **points clés** détectés
- Une **action recommandée**

### Classe `ReclamationSummaryService`

**Sans IA externe** — 100% heuristique locale, pas de dépendance à une API OpenAI ou autre.

#### Enums
- `Urgence` : BASSE, MOYENNE, ELEVEE, CRITIQUE
- `Categorie` : LIVRAISON, PAIEMENT, QUALITE, COMPTE, SERVICE_CLIENT, AUTRE

#### Record `SummaryInsights`
Contient toutes les informations calculées :
```java
record SummaryInsights(
    String shortSummary,    // Résumé court (2-3 phrases max)
    Urgence urgence,        // Niveau d'urgence calculé
    Categorie categorie,    // Catégorie détectée
    List<String> pointsCles, // Points importants
    String prochaineAction, // Action recommandée pour l'admin
    String delaiTraitement  // Temps écoulé depuis la création
)
```

#### Méthode `generateAISummary(description)` — Extracteur de résumé
1. **Découpe en phrases** via `splitSentences()` (séparateur `.!?`)
2. **Notation de chaque phrase** (`scoreSentence`) :
   - +2.0 si c'est la 1ère phrase (position 0)
   - +1.0 si c'est la 2ème phrase
   - +2.0 par mot-clé métier trouvé (probleme, erreur, livraison, remboursement…)
   - +1.2 si contient un nombre
   - +0.8 si longueur entre 40 et 220 caractères
3. **Sélectionne** les 1-2 meilleures phrases (triées par score décroissant, puis remises dans l'ordre original)
4. **Anti-redondance** : si le résumé est trop proche de l'original (>85% de mots en commun ou >85% de la longueur), retourne les 2 premières phrases

#### Méthode `detectCategory(description)` — Catégorisation
Chaque catégorie a une liste de mots-clés associés :
- `LIVRAISON` → livraison, retard, colis, transport, manquant
- `PAIEMENT` → paiement, remboursement, carte, facture, transaction
- `QUALITE` → defaut, casse, qualite, abime, endommag
- `COMPTE` → compte, connexion, mot de passe, profil, bloque
- `SERVICE_CLIENT` → support, sav, reponse, service, assistance

La catégorie avec le plus de mots-clés trouvés gagne. Si aucun mot-clé → `AUTRE`.

#### Méthode `detectUrgence(description, reclamation, reponses)` — Score d'urgence
Système de **score cumulatif** :
| Condition | Points |
|---|---|
| Mots "urgent", "bloque", "impossible" | +3 |
| Mots "remboursement", "paiement" | +2 |
| Mots "retard", "defaut", "casse" | +1 |
| Présence de `!` dans le texte | +1 |
| Statut `en_attente` | +1 |
| Sans réponse depuis 24h–72h | +2 |
| Sans réponse depuis >72h | +3 |

Résultat :
- Score ≥ 7 → `CRITIQUE`
- Score ≥ 5 → `ELEVEE`
- Score ≥ 3 → `MOYENNE`
- Sinon → `BASSE`

#### Méthode `buildRecommendedAction(categorie, urgence, nbReponses)` — Action recommandée
Génère un texte d'action selon la catégorie, enrichi selon l'urgence :
- Si urgence CRITIQUE ou ELEVEE → ajoute "Priorité haute: traiter immédiatement"
- Si aucune réponse encore → ajoute "Commencer par une première réponse empathique"

#### Point d'entrée UI — `ReclamationAdminCardController.handleVoirResume()`
1. Appelle `summaryService.analyzeReclamationOnly(reclamation)` → `SummaryInsights`
2. Appelle `summaryService.buildReclamationOnlyDigest(reclamation)` → texte complet formaté
3. Affiche une `Alert` JavaFX riche avec :
   - Chips colorés (urgence, catégorie, source)
   - Résumé court
   - Points clés en liste à puces
   - Action recommandée
   - Zone `TextArea` avec le digest complet
   - Boutons "Copier résumé court" / "Copier digest complet" (presse-papiers)

---

## 5. Fonctionnalité 3 — Visioconférence Jitsi Meet

### Objectif
Permettre à l'admin de démarrer une visioconférence avec le client directement depuis la réclamation, et envoyer automatiquement un lien d'invitation par email au client.

### Classe concernée : `ReclamationAdminCardController.handleDemarrerMeet()`

#### Flux complet
```
Admin clique [Démarrer Meet]
       │
       ▼
1. Générer un roomId unique :
   "afkart-reclamation-{reclamationId}-{UUID[0..8]}"
   Exemple : "afkart-reclamation-42-f3a91c08"
       │
       ▼
2. Construire l'URL Jitsi :
   "https://meet.jit.si/{roomId}"
       │
       ▼
3. Ouvrir le navigateur via java.awt.Desktop.getDesktop().browse(uri)
   → L'admin rejoint la salle instantanément
       │
       ▼
4. Récupérer l'email du client depuis reclamation.getClient().getEmail()
       │
       ▼
5. Appeler emailService.envoyerInvitationMeetReclamation(
       emailClient, nomClient, titreReclamation, meetUrl)
       │
       ▼
6. Afficher confirmation à l'admin :
   - Succès  → "Visio ouverte et invitation email envoyée au client."
   - Échec email → "Visio ouverte, mais email non envoyé. Vérifiez email.properties"
```

#### Template email d'invitation (HTML)
Construit par `EmailService.buildMeetHtml()` :
- En-tête vert (#0f9d58) "Invitation visioconférence"
- Corps : message personnalisé avec le nom du client et le titre de la réclamation
- Bouton CTA `[Rejoindre la visioconférence]` avec le lien Jitsi
- Lien brut affiché en bas (fallback)

#### Choix technique : Jitsi Meet
- **Aucune installation** côté serveur requise
- **Gratuit et open source**
- Chaque salle est identifiée par son nom dans l'URL
- L'unicité de la salle est garantie par l'UUID généré → pas de collision entre réclamations

---

## 6. Fonctionnalité 4 — Email d'alerte pour l'admin

### Objectif
Surveiller en arrière-plan les réclamations non traitées et envoyer un email d'alerte à **tous les admins** si une réclamation reste sans réponse pendant plus de **5 heures**.

### Classe `ReclamationWarningService`

#### Constantes
```java
private static final int SEUIL_HEURES = 5;                    // délai avant alerte
private static final int INTERVALLE_VERIFICATION_MINUTES = 30; // fréquence de vérification
```

#### Démarrage du service
```java
warningService.demarrer();
```
- Crée un `ScheduledExecutorService` avec un **thread daemon** unique nommé `reclamation-warning-thread`
- Planifie `verifierEtAlerter()` :
  - Première exécution : après 1 minute
  - Ensuite : toutes les 30 minutes
- Thread daemon = ne bloque pas la fermeture de l'application

#### Méthode `verifierEtAlerter()` — Logique principale
```
1. Interroger la BDD → obtenirReclamationsNonTraitees()
   ┌─ Requête SQL :
   │  SELECT r.*, CONCAT(u.prenom, ' ', u.nom) AS nomClient, u.email,
   │         TIMESTAMPDIFF(HOUR, r.date_creation, NOW()) AS heures_ecoulees
   │  FROM reclamation r
   │  LEFT JOIN user u ON r.client_id = u.id
   │  LEFT JOIN reponse_reclamation rep ON r.id = rep.reclamation_id
   │  WHERE r.statut IN ('en_attente', 'en_cours')
   │    AND rep.id IS NULL                    ← AUCUNE réponse
   │    AND TIMESTAMPDIFF(HOUR, ...) >= 5     ← depuis au moins 5h
   │  ORDER BY r.date_creation ASC
   └─

2. Si liste vide → log console, pas d'email

3. Récupérer les emails de tous les admins :
   SELECT email FROM user WHERE role = 'ADMIN'

4. Pour chaque admin :
   → Générer le sujet : "⚠️ Alerte - N réclamation(s) non traitée(s) depuis +5h"
   → Construire le HTML (buildWarningHtml)
   → emailService.envoyerEmailWarning(emailAdmin, subject, html)
```

#### Template HTML d'alerte (`buildWarningHtml`)
- En-tête orange vif (#ff6b35) avec icône ⚠️
- Tableau HTML des réclamations : ID | Titre | Client | Statut (badge coloré) | Date création
- Encart rouge en bas "Action requise : Connectez-vous à l'application AfkArt"
- Footer "Cet email est envoyé automatiquement par le système AfkArt"

#### Particularités
- Les heures écoulées sont affichées dans le titre de chaque réclamation temporairement (ex: "Mon titre [7h sans réponse]")
- Statuts en attente = badge orange, en cours = badge bleu
- Escape HTML (`esc()`) pour éviter les injections dans l'email

#### Arrêt du service
```java
warningService.arreter(); // shutdown immédiat du scheduler
```

---

## 7. EmailService — Couche transversale d'envoi d'email

### Configuration
Chargée depuis `src/main/resources/email.properties` :
```properties
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.username=votre@gmail.com
email.smtp.password=xxxx xxxx xxxx xxxx   # mot de passe d'application Google
email.from=votre@gmail.com
email.from.name=AfkArt
```

### Logique d'activation
Le SMTP est activé **uniquement si** `username` ET `password` sont renseignés et non vides.  
Sinon → **mode preview console** : l'email est affiché dans la console avec un guide de configuration.

### Méthodes publiques

| Méthode | Utilisée par | Description |
|---|---|---|
| `envoyerConfirmationCommande(commande, email)` | Module commandes | Email de confirmation de commande |
| `envoyerInvitationMeetReclamation(email, nom, titre, url)` | ReclamationAdminCardController | Invitation visioconférence |
| `envoyerEmailWarning(emailAdmin, subject, html)` | ReclamationWarningService | Alerte réclamations non traitées |

### Envoi SMTP (`sendViaSmtp`)
- Protocole : SMTP avec STARTTLS (port 587)
- Authentification : `PasswordAuthentication` Jakarta Mail
- Timeout : 10 secondes (connexion + lecture + écriture)
- Encodage : UTF-8 sur tous les champs (sujet, corps, expéditeur)
- Content-Type : `text/html; charset=UTF-8`

### Sécurité
- Utilise un **mot de passe d'application Google** (pas le mot de passe principal)
- Le mot de passe `VOTRE_MOT_DE_PASSE_APPLICATION` est explicitement reconnu comme non configuré

---

## 8. Flux d'interaction complet

### Scénario : Client soumet une réclamation avec un mot interdit

```
Client saisit description → AjouterReclamationController
  → ServiceReclamation.ajouterReclamation(r)
    → validerReclamation(r) ✓
    → appliquerModerationDescription(r)
      → BadWordService.verifierCommentaire(description)
        → BadWordRepository.findAllWords() [SQL]
        → detectForbiddenWords() → ["idiot"] trouvé
        → Stratégie BLOQUER → ModerationResult(allowed=false)
      → throw IllegalArgumentException("Le description contient des mots interdits: idiot")
  ← UI affiche : "Le description contient des mots interdits: idiot"
```

### Scénario : Admin consulte le résumé d'une réclamation

```
Admin clique [Voir résumé] → ReclamationAdminCardController.handleVoirResume()
  → summaryService.analyzeReclamationOnly(reclamation)
    → generateAISummary(description) → "Ma livraison est en retard depuis 5 jours."
    → detectCategory(description) → LIVRAISON
    → detectUrgence(description, reclamation, null) → ELEVEE (score=5)
    → extractKeyPoints(...) → ["Valeurs numériques: 5", "Mot-clé: retard", ...]
    → buildRecommendedAction(LIVRAISON, ELEVEE, 0) → "Vérifier le suivi transport... Priorité haute."
  → buildReclamationOnlyDigest(reclamation) → texte formaté complet
  → Affichage Alert JavaFX avec chips, résumé, points, action, digest
```

### Scénario : Admin démarre une visioconférence

```
Admin clique [Démarrer Meet] → handleDemarrerMeet()
  → Génère roomId : "afkart-reclamation-42-f3a91c08"
  → Ouvre https://meet.jit.si/afkart-reclamation-42-f3a91c08 dans le navigateur
  → emailService.envoyerInvitationMeetReclamation(
        "client@email.com", "Jean Dupont",
        "Problème livraison",
        "https://meet.jit.si/afkart-reclamation-42-f3a91c08")
    → [Si SMTP configuré] Envoie email HTML au client
    → [Sinon] Affiche preview dans la console
  → Admin voit : "Visio ouverte et invitation email envoyée au client."
```

### Scénario : Surveillance automatique des réclamations (background)

```
[Démarrage app] → ReclamationWarningService.demarrer()
  → ScheduledExecutorService toutes les 30 min

  [Après 1 min] → verifierEtAlerter()
    → obtenirReclamationsNonTraitees()
      [SQL] SELECT réclamations statut IN (en_attente, en_cours)
            sans aucune réponse et âgées de >= 5h
    → [2 réclamations trouvées]
    → obtenirEmailsAdmins() → ["admin@afkart.com"]
    → Pour admin@afkart.com :
        Subject : "⚠️ Alerte - 2 réclamation(s) non traitée(s) depuis +5h"
        HTML    : tableau avec les 2 réclamations
      → emailService.envoyerEmailWarning(...)
```

---

## 9. Choix techniques et justifications

| Choix | Justification |
|---|---|
| **Heuristique locale pour le résumé** | Pas de coût API, pas de dépendance réseau, fonctionne hors ligne. Suffisant pour des textes courts de réclamations. |
| **Normalisation Unicode NFD** | Détecte les mots interdits même avec des accents ou majuscules (é → e, IDIOT → idiot). |
| **Regex `(?<!\p{L})MOT(?!\p{L})`** | Évite les faux positifs (ex: "mauvais" dans "maltraitement"). Détecte uniquement des mots entiers. |
| **3 stratégies de modération** | Flexibilité selon le contexte : bloquer pour les réclamations (texte officiel), censurer pour les commentaires informels. |
| **Jitsi Meet (gratuit)** | Aucune installation serveur, gratuit, open source. L'URL suffit pour rejoindre une salle. UUID garantit l'unicité. |
| **ScheduledExecutorService** | Thread daemon léger, non bloquant. Parfait pour une surveillance périodique en arrière-plan. |
| **`ensureSchema()` dans les repositories** | Réduit les erreurs d'installation : les tables sont créées automatiquement si absentes. |
| **Compatibilité multi-schémas (`ensureSchemaCompatibility`)** | Gère les différents nommages de colonnes des bases legacy (datecreation, date_creation, etc.) sans migration manuelle. |
| **Mode preview console** | Permet de développer/tester sans configurer SMTP. Les emails s'affichent dans la console. |
| **Jakarta Mail** | Bibliothèque standard Java pour SMTP. Compatible Java 17+, supporte STARTTLS/TLS. |
| **Pattern MVC JavaFX** | Séparation claire UI/logique. Les controllers délèguent aux services sans logique SQL directe. |

---

## Résumé visuel des 4 fonctionnalités

```
┌──────────────────────────────────────────────────────────────────────┐
│                   MODULE GESTION RÉCLAMATIONS                        │
│                                                                      │
│  ┌─────────────────────┐    ┌──────────────────────────────────────┐ │
│  │  1. BAD WORDS        │    │  2. RÉSUMÉ INTELLIGENT               │ │
│  │  ─────────────────  │    │  ──────────────────────────────────  │ │
│  │  BadWordRepository  │    │  ReclamationSummaryService           │ │
│  │  BadWordService     │    │  → generateAISummary() heuristique   │ │
│  │  Stratégies :        │    │  → detectCategory() par mots-clés   │ │
│  │  BLOQUER/CENSURER/  │    │  → detectUrgence() par score         │ │
│  │  SIGNALER           │    │  → Affichage Alert JavaFX riche      │ │
│  │  Intégré dans       │    │  → Boutons copie presse-papiers      │ │
│  │  ServiceReclamation │    └──────────────────────────────────────┘ │
│  └─────────────────────┘                                             │
│                                                                      │
│  ┌─────────────────────┐    ┌──────────────────────────────────────┐ │
│  │  3. VISIOCONFÉRENCE  │    │  4. EMAIL D'ALERTE ADMIN             │ │
│  │  ─────────────────  │    │  ──────────────────────────────────  │ │
│  │  Jitsi Meet         │    │  ReclamationWarningService           │ │
│  │  URL unique via UUID│    │  → Thread daemon background          │ │
│  │  Ouvre navigateur   │    │  → Vérification toutes les 30 min   │ │
│  │  + Email invitation │    │  → Seuil : 5h sans réponse           │ │
│  │  au client          │    │  → Email HTML à tous les admins      │ │
│  │  EmailService       │    │  EmailService                        │ │
│  └─────────────────────┘    └──────────────────────────────────────┘ │
│                                                                      │
│  ══════════════════════════════════════════════════════════════════  │
│                     EmailService (transversal)                       │
│  SMTP Gmail / Jakarta Mail / email.properties / mode preview console │
└──────────────────────────────────────────────────────────────────────┘
```

