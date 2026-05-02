# Système de Gestion des Réclamations - AfkArt

## Vue d'ensemble

Ce module permet aux clients de soumettre des réclamations (plaintes) et aux administrateurs de gérer ces réclamations avec des réponses.

## Architecture

### Modèles

#### 1. `Reclamation.java`
Représente une réclamation/plainte du client.

**Attributs:**
- `id`: Identifiant unique
- `titre`: Titre de la réclamation (3-255 caractères, obligatoire)
- `description`: Description détaillée (10-2000 caractères, optionnel)
- `imageUrl`: URL d'une image en pièce jointe (optionnel, doit être une image valide)
- `statut`: État de la réclamation (en_attente, en_cours, resolu, rejete)
- `dateCreation`: Date de création (automatique)
- `clientId`: ID du client qui a créé la réclamation
- `client`: Objet User associé

**Énumération:**
```java
StatutReclamation {
    EN_ATTENTE("en_attente"),
    EN_COURS("en_cours"),
    RESOLU("resolu"),
    REJETE("rejete")
}
```

#### 2. `ReponseReclamation.java`
Représente la réponse d'un administrateur à une réclamation.

**Attributs:**
- `id`: Identifiant unique
- `contenu`: Contenu de la réponse (5-2000 caractères)
- `dateReponse`: Date de la réponse (automatique)
- `reclamationId`: ID de la réclamation
- `reclamation`: Objet Reclamation associé
- `adminId`: ID de l'administrateur
- `admin`: Objet User associé

### Service

#### `ServiceReclamation.java`
Gère toutes les opérations CRUD et métier.

**Méthodes principales:**

**Réclamations:**
- `ajouterReclamation(Reclamation r)`: Crée une nouvelle réclamation
- `modifierReclamation(Reclamation r)`: Modifie une réclamation (titre, description, image)
- `modifierStatut(int id, String statut)`: Change le statut (admin uniquement)
- `supprimerReclamation(int id)`: Supprime une réclamation et ses réponses
- `afficherReclamationsClient(int clientId)`: Récupère les réclamations d'un client
- `afficherToutesReclamations()`: Récupère toutes les réclamations (admin)
- `obtenirReclamation(int id)`: Récupère une réclamation spécifique

**Réponses:**
- `ajouterReponse(ReponseReclamation rep)`: Ajoute une réponse
- `obtenirReponses(int reclamationId)`: Récupère toutes les réponses d'une réclamation
- `supprimerReponse(int reponseId)`: Supprime une réponse

**Statistiques:**
- `compterReclamationsParStatut(String statut)`: Compte les réclamations par statut
- `compterReclamationsSansReponse()`: Compte les réclamations sans réponse

### Contrôleurs

#### Pour les Clients

**`ReclamationsClientController`**
- Affiche toutes les réclamations du client connecté
- Permet la recherche et le filtrage
- Charge les cartes de réclamations de manière dynamique

**`AjouterReclamationController`**
- Formulaire pour créer une nouvelle réclamation
- Validation en temps réel des champs
- Règles de validation:
  - Titre: obligatoire, 3-255 caractères
  - Description: optionnel, 10-2000 caractères si fourni
  - Image: optionnel, doit être une URL d'image valide

**`ReclamationCardController`**
- Affiche une réclamation sous forme de carte
- Boutons: Voir, Modifier, Supprimer
- Les modifications ne sont possibles que si le statut est "en_attente"

#### Pour les Administrateurs

**`ReclamationsAdminController`**
- Affiche toutes les réclamations de tous les clients
- Filtre par statut et recherche textuelle
- Charge les cartes admin de manière dynamique

**`ReclamationAdminCardController`**
- Carte admin pour gérer une réclamation
- Changement de statut
- Ajout de réponses
- Visualisation des réponses précédentes

### Vues FXML

#### Client
- **`ReclamationsClient.fxml`**: Liste des réclamations du client
- **`ReclamationCard.fxml`**: Carte d'une réclamation client
- **`AjouterReclamation.fxml`**: Formulaire de création

#### Admin
- **`ReclamationsAdmin.fxml`**: Gestion des réclamations (admin)
- **`ReclamationAdminCard.fxml`**: Carte de gestion admin

### Styles CSS

**`reclamations-style.css`** contient tous les styles:
- Cartes blanches avec bordure gauche colorée
- Badges de statut avec couleurs différentes:
  - En attente: jaune (#f59f00)
  - En cours: vert clair (#2d7a58)
  - Résolu: vert foncé (#22863a)
  - Rejeté: rouge (#c0392b)
- Boutons stylisés avec hover effects
- Champs de texte avec validation visuelle

### Base de données

**Tables SQL (fichier `reclamation.sql`):**

```sql
CREATE TABLE reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NULL,
    image VARCHAR(500) NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id INT NOT NULL,
    CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE reponse_reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT NOT NULL,
    date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reclamation_id INT NOT NULL,
    admin_id INT NOT NULL,
    CONSTRAINT fk_reponse_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE,
    CONSTRAINT fk_reponse_admin FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE RESTRICT
);
```

## Flux d'utilisation

### Pour un Client

1. **Soumettre une réclamation:**
   - Cliquer sur "Nouvelle Réclamation"
   - Remplir le formulaire (titre obligatoire, description et image optionnels)
   - Le système valide les données en temps réel
   - La réclamation est créée avec le statut "en_attente"

2. **Voir ses réclamations:**
   - Accéder à "Mes Réclamations"
   - Voir toutes les réclamations avec leur statut actuel
   - Utiliser la barre de recherche pour filtrer

3. **Modifier une réclamation:**
   - Seules les réclamations avec le statut "en_attente" peuvent être modifiées
   - Cliquer sur "Modifier" pour accéder au formulaire

4. **Supprimer une réclamation:**
   - Confirmation demandée avant suppression

### Pour un Administrateur

1. **Consulter les réclamations:**
   - Accéder à "Gestion des Réclamations"
   - Voir toutes les réclamations de tous les clients
   - Filtrer par statut ou rechercher par texte

2. **Changer le statut:**
   - Sélectionner un nouveau statut dans le combo box
   - Cliquer sur "Enregistrer"

3. **Répondre à une réclamation:**
   - Remplir le champ "Ajouter une réponse"
   - Cliquer sur "Envoyer réponse"
   - Minimum 5 caractères requis

4. **Voir l'historique des réponses:**
   - Cliquer sur "Voir les réponses"
   - Affiche toutes les réponses précédentes

## Règles de Validation

| Champ | Obligatoire | Règle | Message |
|-------|-------------|-------|---------|
| titre | Oui | 3-255 caractères | "Ce champ doit être rempli" |
| description | Non | 10-2000 si fourni | "Au moins 10 caractères" |
| image | Non | Image valide | Upload optionnel |
| statut | Auto | en_attente/en_cours/resolu/rejete | Fixe |
| dateCreation | Auto | Date du jour | Automatique |

## Installation

1. **Créer les tables:**
   ```bash
   mysql -u utilisateur -p base_de_donnees < src/main/resources/sql/reclamation.sql
   ```

2. **Compiler le projet:**
   ```bash
   mvn clean compile
   ```

3. **Intégrer dans le menu principal:**
   - Ajouter un bouton dans le dashboard client vers `/fxml/ReclamationsClient.fxml`
   - Ajouter un bouton dans le dashboard admin vers `/fxml/ReclamationsAdmin.fxml`

## Exemple d'intégration dans ClientDashboardController

```java
@FXML
void handleMesReclamations(ActionEvent event) {
    navigate(event, "/fxml/ReclamationsClient.fxml", "Mes Réclamations", 1000, 700);
}
```

## Exemple d'intégration dans ArtisanDashboardController (admin)

```java
@FXML
void handleGestionReclamations(ActionEvent event) {
    navigate(event, "/fxml/ReclamationsAdmin.fxml", "Gestion des Réclamations", 1200, 800);
}
```

## Améliorations futures possibles

- Pièces jointes multiples
- Évaluation des réponses par le client
- Assignation des réclamations à un admin spécifique
- Notifications email
- Export des statistiques
- Escalade automatique si pas de réponse
- Système de priorité (haute/normale/basse)

