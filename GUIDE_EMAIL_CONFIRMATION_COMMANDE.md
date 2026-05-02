# 📧 Guide : Email Personnalisé de Confirmation de Commande

## 📋 Vue d'ensemble

Le système envoie automatiquement un **email personnalisé** à l'utilisateur connecté lorsqu'il passe une commande.

## ✨ Fonctionnement

### 1️⃣ Passage de Commande
Lorsqu'un client passe une commande via `ValiderCommandeController` :
```java
// L'ID du client connecté est automatiquement récupéré
User user = SessionManager.getCurrentUser();
commande.setClientId(user.getId());

// La commande est enregistrée
serviceCommande.ajouter(commande);
```

### 2️⃣ Récupération de l'Email
Dans `ServiceCommande.java`, la méthode `ajouter()` :
```java
public void ajouter(Commande commande) throws SQLException {
    // ... enregistrement de la commande dans la BD ...
    
    // Envoi automatique de l'email de confirmation
    envoyerEmailConfirmation(commande);
}
```

La méthode `envoyerEmailConfirmation()` récupère l'email du client :
```java
private void envoyerEmailCommande(Commande commande) {
    // Récupération de l'email depuis la base de données
    String emailClient = obtenirEmailDuClient(commande.getClientId());
    
    // Envoi de l'email personnalisé
    emailService.envoyerConfirmationCommande(commande, emailClient);
}

private String obtenirEmailDuClient(int clientId) throws SQLException {
    String sql = "SELECT email FROM `user` WHERE id = ?";
    // ... exécution de la requête ...
    return email;
}
```

### 3️⃣ Personnalisation du Message
Le message inclut :
- **Nom complet du client** récupéré depuis la session ou la base de données
- **Message personnalisé généré par IA** ou message automatique
- **Articles commandés** avec leurs prix
- **Total de la commande**
- **Adresse de livraison**
- **Numéro de commande**

### 4️⃣ Format de l'Email
L'email est envoyé au format HTML avec :
- **Design personnalisé** aux couleurs AfkArt
- **Message de bienvenue personnalisé** avec le nom du client
- **Détails complets de la commande**
- **Indicateur IA** si le message a été généré par intelligence artificielle

## 🎨 Exemple d'Email

```
┌─────────────────────────────────────────┐
│   AfkArt — Confirmation                 │
│                                         │
│ ✨ Message personnalisé                 │
│ ┌─────────────────────────────────────┐ │
│ │ Bonjour Ahmed Ben Ali,              │ │
│ │                                     │ │
│ │ Nous sommes ravis de vous compter  │ │
│ │ parmi nos clients ! Votre commande │ │
│ │ CMD-20240424-XXXX est confirmée.   │ │
│ │                                     │ │
│ │ Total : 250.00 DT                  │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ 📦 Détails de la commande               │
│ • Numéro : CMD-20240424-XXXX           │
│ • Total : 250.00 DT                    │
│ • Adresse : 12 Rue de Carthage, Tunis  │
│                                         │
│ Articles :                              │
│ • Poterie artisanale — 120.00 DT       │
│ • Tapis berbère — 130.00 DT            │
│                                         │
│ Message généré par IA.                  │
└─────────────────────────────────────────┘
```

## ⚙️ Configuration SMTP

Pour activer l'envoi réel d'emails, configurez le fichier `src/main/resources/email.properties` :

```properties
# Configuration SMTP
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.username=votre.email@gmail.com
email.smtp.password=votre_mot_de_passe_application

# Expéditeur
email.from=votre.email@gmail.com
email.from.name=AfkArt
```

### Configuration Gmail
1. Activez la **validation en 2 étapes** sur votre compte Gmail
2. Générez un **mot de passe d'application** :
   - Allez sur https://myaccount.google.com/security
   - Cliquez sur "Mots de passe des applications"
   - Sélectionnez "Mail" et générez un mot de passe
   - Utilisez ce mot de passe dans `email.smtp.password`

### Mode Preview (sans SMTP)
Si le SMTP n'est pas configuré, l'email s'affiche dans la console :
```
================ EMAIL AFKART ================
Mode: SMTP non configuré
TO: client@example.com
SUBJECT: Confirmation de votre commande CMD-XXX
---------------------------------------------
[Contenu HTML de l'email]
=============================================
```

## 🔍 Flux Complet

```
┌─────────────────┐
│  Client passe   │
│  une commande   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ SessionManager  │──────► Récupération de l'utilisateur connecté
│ .getCurrentUser()│       (ID, nom, prénom, email, rôle)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ServiceCommande │
│   .ajouter()    │
└────────┬────────┘
         │
         ├──► 1. Génération du message personnalisé (IA)
         │
         ├──► 2. Enregistrement dans la base de données
         │
         ├──► 3. Récupération de l'email du client via clientId
         │
         └──► 4. Envoi de l'email via EmailService
                  │
                  ├──► Si SMTP configuré : envoi réel
                  │
                  └──► Sinon : affichage console
```

## ✅ Points Clés

1. **Automatique** : L'email est envoyé automatiquement à chaque nouvelle commande
2. **Personnalisé** : Utilise le nom et l'email du compte connecté
3. **Intelligent** : Message généré par IA si disponible
4. **Sécurisé** : L'email est récupéré depuis la base de données via l'ID client
5. **Robuste** : Mode fallback console si SMTP non configuré

## 🧪 Test Manuel

Pour tester l'envoi d'email :

1. **Connectez-vous** avec un compte client
2. **Ajoutez des articles** au panier
3. **Validez la commande**
4. **Vérifiez** :
   - Dans les logs console : confirmation d'envoi
   - Dans votre boîte email (si SMTP configuré)
   - Ou dans la console (mode preview)

## 📝 Code Source Pertinent

- **Contrôleur** : `ValiderCommandeController.java`
- **Service** : `ServiceCommande.java`
- **Email** : `EmailService.java`
- **Session** : `SessionManager.java`
- **Modèles** : `User.java`, `Commande.java`

## 🚀 Améliorations Futures

- ✅ Email de confirmation déjà implémenté
- 📧 Email de mise à jour de statut (livraison, etc.)
- 📄 Génération de facture PDF en pièce jointe
- 🔔 Notifications push mobile
- 📊 Statistiques d'ouverture d'emails
- 🌍 Support multilingue des emails

---

**Note** : Le système utilise l'email du compte connecté via `SessionManager`, garantissant que chaque client reçoit une confirmation personnalisée à sa propre adresse email.

