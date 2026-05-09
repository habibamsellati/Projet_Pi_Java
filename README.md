# ☕ AfkArt — Backend Java

## 📖 Description

AfkArt est une plateforme intelligente dédiée au recyclage, à l’économie circulaire et à l’art durable.

Cette partie correspond au backend Java développé avec JavaFX.

---

# 🚀 Fonctionnalités Principales

# 👤 Gestion des utilisateurs

## Fonctionnalités
- Authentification sécurisée
- Inscription des utilisateurs
- Gestion des rôles :
  - Admin
  - Client
  - Artisan
- Validation des comptes par email
- Réinitialisation des mots de passe
- Connexion Google OAuth
- CAPTCHA anti-spam
- Génération d’avatars IA selon le sexe
- Historique des utilisateurs supprimés
- Restauration des comptes supprimés
- Suppression définitive des utilisateurs

## Fonctionnalités avancées
- Génération PDF des utilisateurs
- Statistiques dynamiques des rôles utilisateurs
- Dashboard administrateur

## APIs utilisées
- Gmail SMTP API
- Google OAuth API
- CAPTCHA API
- PDF Export API
- Avatar AI Generator

---

# ♻️ Gestion des produits recyclables

## Fonctionnalités
- CRUD des produits recyclables
- Gestion des catégories
- Upload des images
- Recherche avancée

## Fonctionnalités avancées
- Génération IA des images
- Estimation intelligente des prix
- Recommandation automatique des artisans
- Détection intelligente des formulaires
- Gestion du calendrier
- Mailing automatique

## APIs utilisées
- AI Image Generator
- NLP Detection API
- Mailing API

---

# 🚚 Gestion des livraisons

## Fonctionnalités
- Gestion des livraisons
- Suivi des livraisons
- Gestion des trajets

## Contraintes métiers
- Maximum 3 livraisons
- Livraison non modifiable
- Livraison non supprimable

## APIs utilisées
- Maps API
- Route Calculation API

---

# 🛒 Gestion des commandes

## Fonctionnalités
- Création des commandes
- Gestion du panier
- Historique des commandes

## Fonctionnalités avancées
- Like / Dislike
- Gestion des commentaires
- Réactions Emoji
- Traduction automatique
- Détection des mots interdits
- Messages personnalisés

## APIs utilisées
- Translation API
- Bad Words API
- Messaging API

---

# 🖼️ Gestion des articles

## Fonctionnalités
- Publication des articles
- Gestion des commentaires
- Modification et suppression

## Fonctionnalités avancées
- Like / Dislike
- Réactions Emoji
- Traduction multilingue
- Détection des contenus toxiques

---

# 📩 Gestion des réclamations

## Fonctionnalités
- Création des réclamations
- Traitement des réclamations
- Gestion des réponses administratives

## Fonctionnalités avancées
- Résumé automatique IA
- Mailing automatique d’avertissement
- Statistiques intelligentes
- Visioconférence

## APIs utilisées
- Bad Words API
- Mailing API
- AI Summary API
- Video Conference API

---

# 🎉 Gestion des événements et réservations

## Fonctionnalités
- Création des événements
- Réservation des places
- Gestion des participants
- Génération des tickets PDF
- Génération QR Code
- Paiement en ligne des réservations

## Fonctionnalités métiers avancées
- Contrôle dynamique des capacités en temps réel
- Auto-annulation des réservations expirées
- Calcul automatique des revenus

## APIs utilisées
- QRServer API
- Gemini API
- Wikipedia API
- PDF Export API
- Payment API

---

# 🛠️ Technologies utilisées

- Java 17
- JavaFX
- JDBC
- MySQL
- Maven
- Jakarta Mail
- Scene Builder

---

# 🔐 Sécurité

- CAPTCHA
- Authentification sécurisée
- Gestion des rôles
- Validation des formulaires
- Hashage des mots de passe
- Protection des données

---

# ⚙️ Installation du projet

## 1️⃣ Cloner le projet

```bash
git clone https://github.com/your-repository/afkart-java.git
```

---

## 2️⃣ Ouvrir le projet

Importer le projet dans :
- IntelliJ IDEA
- Eclipse
- NetBeans

---

## 3️⃣ Configurer la base de données

Créer une base MySQL :

```sql
CREATE DATABASE afkart;
```

Configurer les paramètres de connexion :

```java
DB_URL=jdbc:mysql://localhost:3306/pi_projet
DB_USER=root
DB_PASSWORD=password
```

---

## 4️⃣ Installer les dépendances Maven

```bash
mvn clean install
```

---

## 5️⃣ Lancer l’application

```bash
mvn javafx:run
```

---

# 📧 Services Email

Le projet utilise Gmail SMTP pour :
- Validation des comptes
- Réinitialisation des mots de passe
- Notifications automatiques
- Mailings métiers

---

# 📂 Structure du projet

```bash
src/
 ├── controllers/
 ├── entities/
 ├── services/
 ├── interfaces/
 ├── api/
 ├── utils/
 └── resources/
```

---

# 👨‍💻 Équipe

Projet développé dans le cadre du projet intégré AfkArt.
