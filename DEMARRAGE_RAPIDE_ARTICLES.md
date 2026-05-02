# 🎯 DÉMARRAGE RAPIDE - Système de Gestion des Articles

Bienvenue! Voici comment démarrer en 5 minutes.

## 📖 1. LIRE (2 minutes)

Ouvrez ce fichier d'abord:
```
📄 RESUME_EXECUTIF_ARTICLES.md
```

C'est une synthèse rapide avec:
- ✅ Ce qui est inclus
- ✅ Comment ça marche
- ✅ Installation rapide

## 🔧 2. INSTALLER (20 minutes)

Suivez l'ordre recommandé:
1. Vérifier la table `article`
2. Vérifier les fichiers Java du module
3. Vérifier les fichiers FXML
4. Vérifier les CSS utilisés
5. Compiler et tester

## 💻 3. INTÉGRER (10 minutes)

Points d'intégration principaux:
- Ajouter/valider le bouton d'accès Articles dans les dashboards
- Vérifier la navigation vers `AfficherArticles.fxml`
- Vérifier les droits (ARTISANT/ADMIN)

## 🧪 4. TESTER (5 minutes)

Exécuter le test rapide:
```bash
java -cp target/classes org.example.app.TestCRUDArticle
```

Puis test manuel:
1. Ouvrir la vue Articles
2. Publier un article valide
3. Rechercher un article
4. Trier A→Z puis Z→A
5. Tester modification/suppression

## ✅ 5. VALIDER (10 minutes)

Checklist rapide:
- [ ] Le formulaire refuse titre < 3
- [ ] Le formulaire refuse contenu < 10
- [ ] Le prix négatif est bloqué
- [ ] La liste se recharge après publication
- [ ] La recherche filtre correctement
- [ ] Le tri fonctionne correctement
- [ ] Les droits d'action sont respectés

---

## 📁 FICHIERS PAR USAGE

### 👨‍💼 Je suis Manager/Chef de Projet
→ Lire: `RESUME_EXECUTIF_ARTICLES.md`

### 👨‍💻 Je suis Développeur
→ Lire: `RESUME_SYSTEM_ARTICLES.md`
→ Puis vérifier: `src/main/java/org/example/services/ServiceArticle.java`

### 🏗️ Je suis Architecte
→ Lire: `RESUME_SYSTEM_ARTICLES.md`

### 🚀 Je dois l'installer rapidement
→ Suivre: ce fichier `DEMARRAGE_RAPIDE_ARTICLES.md`

### 🧪 Je dois tester/valider
→ Utiliser: le scénario de test ci-dessus + `TestCRUDArticle`

---

## 🎁 QU'EST-CE QUE J'AI?

### Code Source
✅ Modèle `Article`  
✅ Service métier `ServiceArticle` (CRUD + validations)  
✅ Contrôleurs d'affichage/édition/cartes  
✅ Test CRUD inclus  

### Interfaces
✅ Vue principale Articles  
✅ Carte article backoffice/client  
✅ Formulaire modification  
✅ Vue détail commentaires  

### Design
✅ CSS global cohérent  
✅ Cartes lisibles  
✅ Formulaire intégré  
✅ Tri/recherche ergonomiques  

### Base de Données
✅ Table `article` existante  
✅ Requêtes SQL opérationnelles  
✅ Lecture/écriture validées via service  

### Documentation
✅ Résumé exécutif  
✅ Résumé système  
✅ Démarrage rapide  

---

## 🚀 5 ÉTAPES POUR DÉMARRER

### Étape 1: Vérifier SQL (2 min)
```bash
# vérifier la table article dans MySQL
```

### Étape 2: Vérifier Java Source
Contrôler `Article.java`, `ServiceArticle.java`, `AfficherArticlesController.java`

### Étape 3: Vérifier FXML
Contrôler `AfficherArticles.fxml` et `ArticleCard.fxml`

### Étape 4: Vérifier CSS
Contrôler `style.css` et `blog-style.css`

### Étape 5: Compiler
```bash
mvn clean compile
```

✅ Prêt à utiliser!

---

## 🎯 QUICK FACTS

| Item | Valeur |
|------|--------|
| Module | Articles |
| CRUD | Oui |
| Recherche/Tri | Oui |
| Validation métier | Oui |
| Test rapide | `TestCRUDArticle` |
| Temps de prise en main | 20-30 min |

---

## ❓ FAQ RAPIDE

**Q: Combien de temps pour installer?**  
R: 20-30 minutes pour vérifier et valider le module.

**Q: Y a-t-il des dépendances externes?**  
R: Non, seulement JavaFX/MySQL déjà utilisés dans le projet.

**Q: Le module est-il intégré au backoffice?**  
R: Oui, via `AfficherArticles.fxml` et la navigation dashboard.

**Q: Puis-je personnaliser l'affichage?**  
R: Oui, via `style.css` / `blog-style.css`.

---

## 🎉 C'EST TOUT!

Vous avez maintenant:
- ✅ Un module Articles complet
- ✅ Code opérationnel
- ✅ Documentation claire
- ✅ Test rapide CRUD

Commencez par: **RESUME_EXECUTIF_ARTICLES.md**

---

**Bon courage avec votre module Articles! 🚀**

