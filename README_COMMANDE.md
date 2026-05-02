# Module Commande

## Fichiers ajoutés

- `src/main/java/org/example/models/Commande.java`
- `src/main/java/org/example/services/ServiceCommande.java`
- `src/main/java/org/example/controllers/AjouterCommandeController.java`
- `src/main/java/org/example/controllers/AfficherCommandesController.java`
- `src/main/java/org/example/controllers/ModifierCommandeController.java`
- `src/main/java/org/example/controllers/CommandeCardController.java`
- `src/main/java/org/example/MainCommandeAPP.java`
- `src/main/java/org/example/app/TestCRUDCommande.java`
- `src/main/resources/fxml/AjouterCommande.fxml`
- `src/main/resources/fxml/AfficherCommandes.fxml`
- `src/main/resources/fxml/ModifierCommande.fxml`
- `src/main/resources/fxml/CommandeCard.fxml`
- `src/main/resources/sql/commande.sql`

## Préparer la base de données

Exécuter le script SQL suivant :

- `src/main/resources/sql/commande.sql`

Il crée :

- la table `commande`
- la table de liaison `commande_article`

## Lancer l'interface Commande

Depuis l'IDE, exécuter :

- `org.example.MainCommandeAPP`

## Lancer le test CRUD simple

Depuis l'IDE, exécuter :

- `org.example.app.TestCRUDCommande`

## Règles métier implémentées

- numéro auto généré : `CMD-YYYYMMDD-XXXXXX`
- total strictement supérieur à 0
- adresse de livraison d'au moins 10 caractères
- téléphone tunisien valide si renseigné
- statuts autorisés : `en_attente`, `confirmee`, `livree`
- modes de paiement autorisés : `carte`, `espèces`, `virement`

## Flux UI

1. `AjouterCommande.fxml` : saisie et enregistrement
2. `AfficherCommandes.fxml` : liste des commandes
3. `ModifierCommande.fxml` : modification d'une commande existante
4. `CommandeCard.fxml` : carte avec actions modifier / supprimer
