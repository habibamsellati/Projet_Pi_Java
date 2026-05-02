# Résumé du Système de Gestion des Articles

## 🎯 Vue d'Ensemble

Un système complet de gestion des articles pour AfkArt, permettant la publication artisanale, la consultation structurée, la recherche/tri et la gestion des actions d'édition/suppression selon le rôle utilisateur.

## 📦 Architecture

```
Système Articles
│
├── COUCHE PRÉSENTATION (Vues FXML + CSS)
│   ├── Backoffice:
│   │   ├── AfficherArticles.fxml (Liste + Formulaire)
│   │   ├── ArticleCard.fxml (Carte article)
│   │   ├── ModifierArticle.fxml (Édition)
│   │   └── ArticleCommentairesView.fxml (Détail)
│   │
│   └── Client:
│       ├── ArticleClientCard.fxml
│       └── BlogView.fxml (intégration selon navigation)
│
├── COUCHE CONTRÔLE (Controllers)
│   ├── AfficherArticlesController
│   ├── ArticleCardController
│   ├── ModifierArticleController
│   ├── AjouterArticleController
│   └── ArticleCommentairesController
│
├── COUCHE MÉTIER (Service)
│   └── ServiceArticle
│       ├── CRUD Article
│       ├── Validation métier
│       └── Contrôle de cohérence SQL
│
├── COUCHE MODÈLE (Models)
│   └── Article
│       ├── id, titre, contenu
│       ├── datePublication
│       ├── categorie, prix, imageUrl
│       └── artisanId
│
├── BASE DE DONNÉES
│   └── article (table principale)
│
└── TESTS / SUPPORT
	└── TestCRUDArticle.java
```

## 🔄 Flux Fonctionnel

### Publication / Gestion Backoffice

```
Utilisateur connecté (ARTISANT ou ADMIN)
	↓
Ouvre "AfficherArticles.fxml"
	↓
Voit la liste des articles existants
	├── Recherche par titre/contenu
	├── Tri (Par défaut / A→Z / Z→A)
	└── Rafraîchir
	↓
Remplit le formulaire "Ajouter un article"
	├── Titre* (>= 3)
	├── Contenu* (>= 10)
	├── Catégorie
	├── Prix (optionnel, >= 0)
	└── Image (optionnelle, format valide)
	↓
Publication en base via ServiceArticle
```

### Actions sur une carte article

```
Carte chargée
	├── Lire suite → Détail + commentaires
	├── Modifier → Formulaire d'édition (si autorisé)
	└── Supprimer → Confirmation puis suppression (si autorisé)
```

## 📊 Règles Métier

| Entité | Champ | Règle |
|--------|-------|-------|
| **Article** | titre | 3-255 caractères (obligatoire) |
| | contenu | minimum 10 caractères (obligatoire) |
| | categorie | valeurs autorisées (Artisanat, Décoration, Textile, Céramique, Autres) |
| | prix | null ou valeur >= 0 |
| | imageUrl | extension image valide si renseignée |

## 🗄️ Base de Données (vue logique)

### Table article
```sql
id (PK, Auto)
titre (VARCHAR, NOT NULL)
contenu (TEXT, NOT NULL)
date (DATETIME/TIMESTAMP)
categorie (VARCHAR)
prix (DECIMAL, NULL)
image (VARCHAR, NULL)
artisan_id (FK logique user)
```

## 🎨 Interface Utilisateur

### Vue principale Articles
- Bloc haut: retour + titre + rafraîchir
- Bloc liste: recherche + tri + cartes
- Bloc formulaire: création article

### Carte article
- Date, auteur, titre, contenu court, prix
- Actions: Lire suite / Modifier / Supprimer

## 🚀 Fonctionnalités Implémentées

✅ Création d'article  
✅ Affichage liste d'articles  
✅ Recherche en temps réel  
✅ Tri alphabétique  
✅ Modification d'article  
✅ Suppression d'article  
✅ Validation métier côté service  
✅ Contrôle de droits côté contrôleur  

## 🔐 Sécurité et Robustesse

- Validation entrée utilisateur côté service
- Requêtes paramétrées (`PreparedStatement`)
- Gestion d'exceptions affichée en UI (`Alert`)
- Cloisonnement clair Service/Controller/View

## 🔗 Intégration

### Navigation Admin
```java
@FXML void handleVoirArticles(ActionEvent event) {
	navigate(event, "/fxml/AfficherArticles.fxml", "Backoffice - Articles");
}
```

### Navigation Artisan
```java
@FXML void handleVoirArticles(ActionEvent event) {
	navigate(event, "/fxml/AfficherArticles.fxml", "Articles", 1200, 780);
}
```

## 📈 Évolutions Recommandées

- Pagination serveur ou client pour gros volume
- Filtre avancé par catégorie/prix/date
- Workflow publication (brouillon/validé)
- Upload image local/cloud au lieu d'URL brute
- Dashboard statistique articles/catégories

---

**Module Articles cohérent, maintenable et prêt pour les évolutions backoffice. 🚀**

