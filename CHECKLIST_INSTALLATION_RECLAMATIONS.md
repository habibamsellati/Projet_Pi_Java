# 🚀 Checklist Complète d'Installation

## Phase 1: Préparation

### Base de Données
- [ ] Créer un backup de la base de données
- [ ] Exécuter le script SQL: `reclamation.sql`
- [ ] Vérifier que les 2 tables sont créées:
  ```sql
  SHOW TABLES LIKE 'reclamation%';
  ```
- [ ] Vérifier les colonnes:
  ```sql
  DESC reclamation;
  DESC reponse_reclamation;
  ```

### Fichiers Java
- [ ] `Reclamation.java` copié dans `models/`
- [ ] `ReponseReclamation.java` copié dans `models/`
- [ ] `ServiceReclamation.java` copié dans `services/`
- [ ] `ReclamationsClientController.java` copié dans `controllers/`
- [ ] `ReclamationCardController.java` copié dans `controllers/`
- [ ] `AjouterReclamationController.java` copié dans `controllers/`
- [ ] `ReclamationsAdminController.java` copié dans `controllers/`
- [ ] `ReclamationAdminCardController.java` copié dans `controllers/`
- [ ] `TestReclamation.java` copié dans `app/`

### Fichiers FXML
- [ ] `ReclamationsClient.fxml` copié dans `resources/fxml/`
- [ ] `ReclamationCard.fxml` copié dans `resources/fxml/`
- [ ] `AjouterReclamation.fxml` copié dans `resources/fxml/`
- [ ] `ReclamationsAdmin.fxml` copié dans `resources/fxml/`
- [ ] `ReclamationAdminCard.fxml` copié dans `resources/fxml/`

### Fichiers CSS
- [ ] `reclamations-style.css` copié dans `resources/css/`

### Documentation
- [ ] `README_RECLAMATIONS.md` présent
- [ ] `GUIDE_INTEGRATION_RECLAMATIONS.md` présent
- [ ] `RESUME_SYSTEM_RECLAMATIONS.md` présent
- [ ] `EXEMPLES_INTEGRATION_RECLAMATIONS.md` présent
- [ ] `CHECKLIST_INSTALLATION.md` présent

## Phase 2: Compilation

### Maven
- [ ] Ouvrir le projet dans IntelliJ
- [ ] Lancer: `mvn clean compile`
- [ ] ✓ Pas d'erreurs de compilation

### Fichiers compilés
- [ ] Tous les .java compilés dans `target/classes/`
- [ ] Tous les .fxml présents dans `target/classes/fxml/`
- [ ] Le CSS présent dans `target/classes/css/`

## Phase 3: Tests Unitaires

### Test des Modèles
- [ ] Exécuter: `java -cp target/classes org.example.app.TestReclamation`
- [ ] Résultats:
  - [ ] Test 1: Création valide ✓
  - [ ] Test 2: Validation titre ✓
  - [ ] Test 3: Validation description ✓
  - [ ] Test 4: Validation image ✓
  - [ ] Test 5: Réponse valide ✓
  - [ ] Test 6: Validation réponse ✓
  - [ ] Test 7: Statuts ✓

### Test Base de Données
- [ ] La connexion SQL fonctionne
- [ ] Les tables sont accessibles
- [ ] Insérer une réclamation de test:
  ```sql
  INSERT INTO reclamation (titre, description, statut, client_id) 
  VALUES ('Test', 'Description de test', 'en_attente', 1);
  ```
- [ ] Vérifier l'insertion:
  ```sql
  SELECT * FROM reclamation;
  ```

## Phase 4: Intégration Client

### Dashboard Client
- [ ] Ouvrir `ClientDashboard.fxml`
- [ ] Ajouter le bouton "Mes Réclamations"
- [ ] Ouvrir `ClientDashboardController.java`
- [ ] Ajouter la méthode:
  ```java
  @FXML
  void handleMesReclamations(ActionEvent event) {
      navigate(event, "/fxml/ReclamationsClient.fxml", 
              "Mes Réclamations", 1000, 700);
  }
  ```
- [ ] Compiler et tester
- [ ] ✓ Le bouton fonctionne et navigue vers la page

### Test du Formulaire
- [ ] Cliquer sur "Nouvelle Réclamation"
- [ ] Tester la validation en temps réel:
  - [ ] Titre vide: erreur affichée
  - [ ] Titre < 3 caractères: erreur
  - [ ] Titre valide: OK
  - [ ] Description < 10 caractères: erreur
  - [ ] Description valide (ou vide): OK
  - [ ] Image invalide: erreur
  - [ ] Image valide (ou vide): OK
- [ ] Soumettre une réclamation valide
- [ ] ✓ Réclamation créée en base de données
- [ ] ✓ Retour à la liste

### Test de la Liste Client
- [ ] Affichage des réclamations
- [ ] Recherche par titre
- [ ] Recherche par description
- [ ] Recherche par statut
- [ ] Bouton "Voir"
- [ ] Bouton "Modifier" (sauf si pas en_attente)
- [ ] Bouton "Supprimer" (avec confirmation)
- [ ] Suppression cascade des réponses

## Phase 5: Intégration Admin

### Dashboard Admin
- [ ] Ouvrir le dashboard admin (ArtisanDashboard.fxml ou autre)
- [ ] Ajouter le bouton "Gestion des Réclamations"
- [ ] Ouvrir le controller correspondant
- [ ] Ajouter la méthode:
  ```java
  @FXML
  void handleGestionReclamations(ActionEvent event) {
      navigate(event, "/fxml/ReclamationsAdmin.fxml", 
              "Gestion des Réclamations", 1200, 800);
  }
  ```
- [ ] ✓ Vérifier que seuls les ADMIN peuvent accéder

### Test de Gestion Admin
- [ ] Affichage de toutes les réclamations
- [ ] Filtre par statut:
  - [ ] "Tous les statuts"
  - [ ] "en_attente"
  - [ ] "en_cours"
  - [ ] "resolu"
  - [ ] "rejete"
- [ ] Recherche textuelle:
  - [ ] Par titre
  - [ ] Par description
  - [ ] Par nom du client
- [ ] Changement de statut
- [ ] Ajout de réponse:
  - [ ] Validation (min 5 caractères)
  - [ ] Base de données mise à jour
- [ ] Visualisation des réponses précédentes

## Phase 6: Test Complet du Flux

### Scénario Complet
1. [ ] Client se connecte
2. [ ] Client crée une réclamation avec:
   - [ ] Titre valide
   - [ ] Description complète
   - [ ] Image (optionnel)
3. [ ] Réclamation apparaît dans la liste avec statut "en_attente"
4. [ ] Admin se connecte
5. [ ] Admin voit la réclamation dans la liste
6. [ ] Admin change le statut à "en_cours"
7. [ ] Admin ajoute une réponse
8. [ ] Client revient et voit:
   - [ ] Statut mis à jour
   - [ ] Sa réclamation modifiable devient non-modifiable
9. [ ] Admin change le statut à "resolu"
10. [ ] Client voit:
    - [ ] Badge de statut "Resolu" en vert
    - [ ] Les réponses (optionnel)

## Phase 7: Tests Avancés

### Gestion des Erreurs
- [ ] Essayer d'accéder sans être connecté
  - [ ] Message "Accès refusé"
- [ ] Essayer d'accéder en tant que CLIENT à l'admin
  - [ ] Message "Accès refusé"
- [ ] Connexion SQL cassée
  - [ ] Message d'erreur approprié
- [ ] Base de données vide
  - [ ] Message "Aucune réclamation"

### Performances
- [ ] Charger 100 réclamations
  - [ ] Pas de lag notable
- [ ] Recherche rapide
  - [ ] Résultats instantanés
- [ ] Paginer les résultats (optionnel)

### Sécurité
- [ ] SQL Injection:
  - [ ] Entrer `'; DROP TABLE reclamation;` dans titre
  - [ ] ✓ Pas d'exécution dangereuse (PreparedStatement)
- [ ] Client ne peut pas voir les réclamations d'autres
  - [ ] ✓ Vérifier le filtre client_id
- [ ] Admin ne peut accéder qu'avec le bon rôle
  - [ ] ✓ Vérifier le role

## Phase 8: Interface Utilisateur

### Design Client
- [ ] Cartes blanches visibles
- [ ] Bordures latérales colorées
- [ ] Badges de statut clairs
- [ ] Boutons accessibles
- [ ] Responsive sur différentes résolutions
- [ ] Texte lisible
- [ ] Animations smooth

### Design Admin
- [ ] Filtres visibles et accessibles
- [ ] Recherche intuitive
- [ ] Zone de réponse claire
- [ ] Historique des réponses visible
- [ ] Changement de statut facile

## Phase 9: Performance et Optimisation

### Base de Données
- [ ] Index créés sur:
  - [ ] client_id
  - [ ] statut
  - [ ] date_creation
- [ ] Les requêtes sont optimisées
- [ ] Pas de N+1 queries

### Mémoire
- [ ] Pas de fuites mémoire
- [ ] Pas d'objets non libérés
- [ ] Garbage collection efficace

## Phase 10: Documentation

### Code
- [ ] Tous les fichiers Java ont des commentaires
- [ ] Les méthodes publiques sont documentées
- [ ] Les validations sont expliquées

### Utilisateur
- [ ] README clair et complet
- [ ] Guide d'intégration détaillé
- [ ] Exemples pratiques fournis
- [ ] FAQ disponible (optionnel)

## Phase 11: Maintenance

### Logs
- [ ] Les erreurs sont loggées
- [ ] Les actions importantes sont tracées
- [ ] Format de logs standardisé

### Backup
- [ ] Plan de backup de la base de données
- [ ] Procédure de restauration documentée

### Version Control
- [ ] Tous les fichiers commitées
- [ ] Messages de commit clairs
- [ ] Branches propres

## Phase 12: Déploiement

### Pré-Production
- [ ] Tous les tests passent
- [ ] ✓ Aucun warning Maven
- [ ] ✓ Code revu
- [ ] ✓ Documentation complète

### Production
- [ ] Base de données sauvegardée
- [ ] Application testée en environnement similaire
- [ ] Rollback plan préparé
- [ ] Support utilisateur prêt

## Checklist de Vérification Finale

```
FICHIERS
├── Java (8 fichiers)
│   ├── [✓] Reclamation.java
│   ├── [✓] ReponseReclamation.java
│   ├── [✓] ServiceReclamation.java
│   ├── [✓] 5 Controllers
│   └── [✓] TestReclamation.java
│
├── FXML (5 fichiers)
│   ├── [✓] ReclamationsClient.fxml
│   ├── [✓] ReclamationCard.fxml
│   ├── [✓] AjouterReclamation.fxml
│   ├── [✓] ReclamationsAdmin.fxml
│   └── [✓] ReclamationAdminCard.fxml
│
├── CSS (1 fichier)
│   └── [✓] reclamations-style.css
│
├── SQL (1 fichier)
│   └── [✓] reclamation.sql
│
└── Documentation (5 fichiers)
    ├── [✓] README_RECLAMATIONS.md
    ├── [✓] GUIDE_INTEGRATION_RECLAMATIONS.md
    ├── [✓] RESUME_SYSTEM_RECLAMATIONS.md
    ├── [✓] EXEMPLES_INTEGRATION_RECLAMATIONS.md
    └── [✓] CHECKLIST_INSTALLATION.md

FONCTIONNALITÉS
├── [✓] Création de réclamations
├── [✓] Modification de réclamations
├── [✓] Suppression de réclamations
├── [✓] Visualisation des réclamations
├── [✓] Filtrage et recherche
├── [✓] Gestion des statuts
├── [✓] Ajout de réponses
├── [✓] Visualisation des réponses
├── [✓] Validation en temps réel
├── [✓] Design responsive
└── [✓] Gestion des erreurs

TEST
├── [✓] Tests unitaires passent
├── [✓] Tests d'intégration passent
├── [✓] Pas d'erreurs de compilation
└── [✓] Pas de fuites mémoire

SÉCURITÉ
├── [✓] Validation des données
├── [✓] Vérification des rôles
├── [✓] PreparedStatement utilisé
└── [✓] Pas d'injection SQL

PERFORMANCE
├── [✓] Recherche rapide
├── [✓] Chargement optimisé
├── [✓] Index de base de données
└── [✓] Pas de lag

DOCUMENTATION
├── [✓] Code commenté
├── [✓] API documentée
├── [✓] Exemples fournis
└── [✓] Guide clair
```

## Statut Final

- **Total Éléments:** 80+
- **À Faire:** [ ] Marquez les cases au fur et à mesure
- **Date de Démarrage:** _______________
- **Date de Fin Prévue:** _______________
- **Date de Fin Réelle:** _______________

---

**Bienvenue dans AfkArt! Le système de réclamations est maintenant intégré! 🎉**

