# Guide d'Intégration Complète - Système de Gestion des Réclamations

## 1. Fichiers Créés

### Modèles (Models)
- `Reclamation.java` - Entité représentant une réclamation
- `ReponseReclamation.java` - Entité représentant une réponse à une réclamation

### Services
- `ServiceReclamation.java` - Couche métier pour la gestion des réclamations

### Contrôleurs (Controllers)
- `ReclamationsClientController.java` - Gestion de la vue client
- `ReclamationCardController.java` - Composant carte pour un client
- `AjouterReclamationController.java` - Formulaire d'ajout pour client
- `ReclamationsAdminController.java` - Gestion de la vue admin
- `ReclamationAdminCardController.java` - Composant carte pour admin

### Vues (FXML)
- `ReclamationsClient.fxml` - Liste des réclamations du client
- `ReclamationCard.fxml` - Carte d'une réclamation client
- `AjouterReclamation.fxml` - Formulaire d'ajout
- `ReclamationsAdmin.fxml` - Gestion admin des réclamations
- `ReclamationAdminCard.fxml` - Carte de gestion admin

### Styles (CSS)
- `reclamations-style.css` - Feuille de styles pour les réclamations

### Base de Données (SQL)
- `reclamation.sql` - Script de création des tables

### Tests
- `TestReclamation.java` - Tests unitaires

### Documentation
- `README_RECLAMATIONS.md` - Documentation complète
- `GUIDE_INTEGRATION.md` - Ce fichier

## 2. Étapes d'Installation

### Étape 1: Créer les Tables en Base de Données

Exécutez le script SQL pour créer les tables:

```bash
mysql -u votre_utilisateur -p votre_base_de_donnees < src/main/resources/sql/reclamation.sql
```

Ou copiez-collez les commandes SQL directement dans votre client MySQL:

```sql
CREATE TABLE IF NOT EXISTS reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NULL,
    image VARCHAR(500) NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id INT NOT NULL,
    CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_client_id (client_id),
    INDEX idx_statut (statut),
    INDEX idx_date_creation (date_creation)
);

CREATE TABLE IF NOT EXISTS reponse_reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT NOT NULL,
    date_reponse DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reclamation_id INT NOT NULL,
    admin_id INT NOT NULL,
    CONSTRAINT fk_reponse_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE,
    CONSTRAINT fk_reponse_admin FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE RESTRICT,
    INDEX idx_reclamation_id (reclamation_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_date_reponse (date_reponse)
);
```

### Étape 2: Vérifier le Projet

Vérifiez que les fichiers sont bien placés:

```
src/main/java/org/example/
├── models/
│   ├── Reclamation.java
│   └── ReponseReclamation.java
├── services/
│   └── ServiceReclamation.java
├── controllers/
│   ├── ReclamationsClientController.java
│   ├── ReclamationCardController.java
│   ├── AjouterReclamationController.java
│   ├── ReclamationsAdminController.java
│   └── ReclamationAdminCardController.java
└── app/
    └── TestReclamation.java

src/main/resources/
├── fxml/
│   ├── ReclamationsClient.fxml
│   ├── ReclamationCard.fxml
│   ├── AjouterReclamation.fxml
│   ├── ReclamationsAdmin.fxml
│   └── ReclamationAdminCard.fxml
├── css/
│   └── reclamations-style.css
└── sql/
    └── reclamation.sql
```

### Étape 3: Ajouter les Boutons au Dashboard Client

Modifiez `ClientDashboardController.java`:

```java
@FXML
void handleMesReclamations(ActionEvent event) {
    navigate(event, "/fxml/ReclamationsClient.fxml", 
            "Mes Réclamations", 1000, 700);
}
```

Ajoutez le bouton dans `ClientDashboard.fxml`:

```xml
<Button text="Mes Réclamations" 
       onAction="#handleMesReclamations"
       styleClass="btn-primary"/>
```

### Étape 4: Ajouter les Boutons au Dashboard Admin

Modifiez `ArtisanDashboardController.java` (ou votre contrôleur admin):

```java
@FXML
void handleGestionReclamations(ActionEvent event) {
    navigate(event, "/fxml/ReclamationsAdmin.fxml", 
            "Gestion des Réclamations", 1200, 800);
}
```

Ajoutez le bouton dans le FXML correspondant.

## 3. Flux d'Utilisation

### Scénario Client

1. **Le client se connecte**
   - SessionManager mémorise l'utilisateur connecté

2. **Le client clique sur "Mes Réclamations"**
   - Navigue vers `/fxml/ReclamationsClient.fxml`
   - `ReclamationsClientController` charge toutes les réclamations du client
   - Chaque réclamation s'affiche sous forme de carte

3. **Le client clique sur "Nouvelle Réclamation"**
   - Navigue vers `/fxml/AjouterReclamation.fxml`
   - `AjouterReclamationController` affiche un formulaire
   - Validation en temps réel des champs
   - À la soumission:
     - ServiceReclamation valide les données
     - Crée la réclamation en base avec statut "en_attente"
     - Revient à la liste

4. **Le client voit sa réclamation**
   - Statut "En attente" en badge jaune
   - Peut modifier ou supprimer si statut "en_attente"
   - Peut voir les détails et les réponses

### Scénario Admin

1. **L'admin se connecte**
   - SessionManager mémorise l'utilisateur connecté

2. **L'admin clique sur "Gestion des Réclamations"**
   - Navigue vers `/fxml/ReclamationsAdmin.fxml`
   - `ReclamationsAdminController` charge toutes les réclamations
   - Affiche les réclamations avec filtre par statut

3. **L'admin filtre les réclamations**
   - Par statut: en_attente, en_cours, resolu, rejete
   - Par texte: titre, description, nom du client

4. **L'admin sélectionne une réclamation**
   - La carte admin affiche tous les détails
   - Peut changer le statut
   - Peut ajouter une réponse
   - Peut voir les réponses précédentes

5. **L'admin change le statut**
   - Sélectionne le nouveau statut
   - Clique sur "Enregistrer"
   - La base de données est mise à jour

6. **L'admin ajoute une réponse**
   - Remplir le champ de réponse (min 5 caractères)
   - Clique sur "Envoyer réponse"
   - La réponse est créée avec l'ID de l'admin
   - Le client sera notifié (optionnel, à implémenter)

## 4. Validations

### Réclamation

| Champ | Règle | Message d'erreur |
|-------|-------|------------------|
| titre | 3-255 caractères | Ce champ doit être rempli |
| description | Optionnel ou 10-2000 caractères | Au moins 10 caractères |
| image | Optionnel ou image valide | Format invalide |
| statut | en_attente, en_cours, resolu, rejete | Statut invalide |

### Réponse

| Champ | Règle | Message d'erreur |
|-------|-------|------------------|
| contenu | 5-2000 caractères | Le contenu doit contenir 5-2000 caractères |
| reclamationId | > 0 | ID invalide |
| adminId | > 0 | ID invalide |

## 5. Personnalisation des Styles

### Couleurs Principales

```css
/* Badge statuts */
.status-en-attente { background-color: #fff9db; color: #f59f00; }
.status-en-cours { background-color: #d3f9d8; color: #2d7a58; }
.status-resolu { background-color: #c6f6d5; color: #22863a; }
.status-rejete { background-color: #fce4e4; color: #c0392b; }

/* Boutons */
.btn-new { background-color: #2d7a58; color: white; }
.btn-view { background-color: #967d54; color: white; }
.btn-edit { background-color: #c4a47c; color: white; }
.btn-delete { background-color: #e74c3c; color: white; }
```

Pour changer ces couleurs, modifiez le fichier `reclamations-style.css`.

## 6. Tests

Pour exécuter les tests unitaires:

```bash
java -cp target/classes org.example.app.TestReclamation
```

Les tests couvrent:
- Création de réclamations valides
- Validation des titres
- Validation des descriptions
- Validation des images
- Création de réponses valides
- Validation des réponses
- Gestion des statuts

## 7. Améliorations Futures

### Phase 2
- [ ] Upload d'images (au lieu d'URL)
- [ ] Pièces jointes multiples
- [ ] Notifications email
- [ ] Priorités (haute/normale/basse)
- [ ] Assignation à un admin spécifique

### Phase 3
- [ ] Évaluation des réponses
- [ ] Escalade automatique
- [ ] Export des statistiques
- [ ] Rapports détaillés
- [ ] API REST

### Phase 4
- [ ] Système de chatbot
- [ ] Intégration avec système de tickets externes
- [ ] Analytics avancées
- [ ] Prédictions avec IA

## 8. Dépannage

### Problème: "Accès refusé" dans ReclamationsClient

**Cause:** L'utilisateur n'est pas un CLIENT ou la session n'est pas initialisée

**Solution:**
1. Vérifier que `SessionManager.getCurrentUser()` retourne un utilisateur
2. Vérifier que l'utilisateur a le rôle CLIENT

### Problème: Les réclamations ne s'affichent pas

**Cause:** La base de données n'a pas de données

**Solution:**
1. Vérifier que les tables existent: `SHOW TABLES LIKE 'reclamation%';`
2. Insérer des données de test:
```sql
INSERT INTO reclamation (titre, description, statut, client_id) 
VALUES ('Test', 'Description de test', 'en_attente', 1);
```

### Problème: Erreur "Column not found" au lancement

**Cause:** Les tables SQL n'ont pas été créées

**Solution:**
1. Exécuter le script `reclamation.sql`
2. Vérifier les noms des colonnes dans le service

## 9. Contacts et Support

Pour toute question ou problème:
1. Consultez le README_RECLAMATIONS.md
2. Vérifiez les logs de l'application
3. Exécutez les tests unitaires
4. Consultez le code des contrôleurs

---

**Version:** 1.0  
**Date:** 2026-04-11  
**Auteur:** GitHub Copilot

