# 🔧 Guide de Configuration : Envoi d'Emails Réels

## 📋 Vue d'ensemble

Ce guide vous aide à configurer l'envoi **réel** d'emails personnalisés lorsqu'un client passe une commande.

**État actuel :** ✅ Le code est déjà implémenté et fonctionne !
- Mode **preview console** : actif par défaut (affiche l'email dans les logs)
- Mode **SMTP réel** : nécessite configuration (envoie de vrais emails)

---

## ⚡ Configuration Rapide (5 minutes)

### Étape 1 : Choisissez votre fournisseur d'email

| Fournisseur | Avantages | Configuration |
|-------------|-----------|---------------|
| **Gmail** ⭐ | Gratuit, fiable, 500 emails/jour | Recommandé |
| **Outlook** | Gratuit, Microsoft | Alternative |
| **SMTP personnalisé** | Contrôle total | Avancé |

---

## 🟢 Option 1 : Gmail (Recommandé)

### 📝 Prérequis
- Un compte Gmail (ex: `votre.email@gmail.com`)
- Validation en 2 étapes activée

### 🔐 Générer un mot de passe d'application

1. **Allez sur** : https://myaccount.google.com/security

2. **Activez la validation en 2 étapes** (si ce n'est pas déjà fait)
   - Cliquez sur "Validation en 2 étapes"
   - Suivez les instructions

3. **Générez un mot de passe d'application** :
   - Retournez sur https://myaccount.google.com/security
   - Cliquez sur "Mots de passe des applications"
   - Sélectionnez :
     - App : **Mail**
     - Appareil : **Autre** (tapez "AfkArt")
   - Cliquez sur **Générer**
   - Notez le mot de passe (16 caractères) : `xxxx xxxx xxxx xxxx`

### ⚙️ Configurez email.properties

Éditez le fichier : `src/main/resources/email.properties`

```properties
# Configuration SMTP Gmail
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.username=votre.email@gmail.com
email.smtp.password=xxxxyyyyzzzzaaaa

# Expéditeur (même adresse)
email.from=votre.email@gmail.com
email.from.name=AfkArt
```

**Important** :
- Remplacez `votre.email@gmail.com` par votre vraie adresse Gmail
- Remplacez `xxxxyyyyzzzzaaaa` par le mot de passe d'application (sans espaces)
- Ne mettez PAS votre mot de passe Gmail normal !

### ✅ Test

1. Relancez votre application
2. Connectez-vous comme client
3. Passez une commande
4. Vérifiez les logs :
   ```
   EmailService: mode SMTP actif (host=smtp.gmail.com, from=votre.email@gmail.com)
   Email envoyé à client@example.com
   ```
5. Vérifiez la boîte email du client !

---

## 🔵 Option 2 : Outlook / Hotmail

### ⚙️ Configuration

Éditez `src/main/resources/email.properties` :

```properties
# Configuration SMTP Outlook
email.smtp.host=smtp-mail.outlook.com
email.smtp.port=587
email.smtp.username=votre.email@outlook.com
email.smtp.password=votre_mot_de_passe

# Expéditeur
email.from=votre.email@outlook.com
email.from.name=AfkArt
```

**Note** : Outlook utilise votre mot de passe normal (pas de mot de passe d'application)

---

## 🟣 Option 3 : SMTP Personnalisé

Pour un serveur SMTP personnalisé (ex: hébergeur web) :

```properties
email.smtp.host=mail.votre-domaine.com
email.smtp.port=587
email.smtp.username=noreply@votre-domaine.com
email.smtp.password=votre_mot_de_passe

email.from=noreply@votre-domaine.com
email.from.name=AfkArt
```

---

## 🔐 Sécurité : Variables d'Environnement (Recommandé)

Au lieu de mettre les mots de passe dans le fichier, utilisez des variables d'environnement :

### Windows (PowerShell)
```powershell
$env:EMAIL_SMTP_HOST = "smtp.gmail.com"
$env:EMAIL_SMTP_PORT = "587"
$env:EMAIL_SMTP_USERNAME = "votre.email@gmail.com"
$env:EMAIL_SMTP_PASSWORD = "xxxxyyyyzzzzaaaa"
$env:EMAIL_FROM = "votre.email@gmail.com"
$env:EMAIL_FROM_NAME = "AfkArt"
```

### Linux / macOS
```bash
export EMAIL_SMTP_HOST="smtp.gmail.com"
export EMAIL_SMTP_PORT="587"
export EMAIL_SMTP_USERNAME="votre.email@gmail.com"
export EMAIL_SMTP_PASSWORD="xxxxyyyyzzzzaaaa"
export EMAIL_FROM="votre.email@gmail.com"
export EMAIL_FROM_NAME="AfkArt"
```

Le système les détectera automatiquement !

---

## 🧪 Vérification de la Configuration

### Test 1 : Vérifier les logs au démarrage

Quand vous lancez l'application, cherchez cette ligne :

✅ **SMTP configuré** :
```
EmailService: mode SMTP actif (host=smtp.gmail.com, from=votre.email@gmail.com)
```

❌ **SMTP non configuré** :
```
EmailService: SMTP incomplet, mode preview console actif. Configurez email.smtp.* et email.from.
```

### Test 2 : Passer une commande de test

1. Connectez-vous avec un compte client ayant un **vrai email**
2. Ajoutez des articles au panier
3. Validez la commande
4. Vérifiez les logs :

✅ **Email envoyé** :
```
Email envoyé à client@example.com
```

❌ **Erreur SMTP** :
```
Échec d'envoi email SMTP: Authentication failed
================ EMAIL AFKART ================
Mode: fallback console après erreur SMTP
...
```

### Test 3 : Vérifier la réception

- Ouvrez la boîte email du client
- Cherchez un email de "AfkArt"
- Sujet : "Confirmation de votre commande CMD-..."

---

## 🐛 Dépannage

### Problème : "Authentication failed"

**Cause** : Mot de passe incorrect ou validation en 2 étapes non activée

**Solution** :
1. Vérifiez que vous utilisez un **mot de passe d'application** (Gmail)
2. Pas le mot de passe normal de votre compte !
3. Activez la validation en 2 étapes sur Gmail

### Problème : "Connection timeout"

**Cause** : Pare-feu ou port bloqué

**Solution** :
1. Vérifiez que le port **587** est ouvert
2. Désactivez temporairement votre pare-feu/antivirus
3. Essayez le port **465** (SSL) :
   ```properties
   email.smtp.port=465
   ```

### Problème : "Email non envoyé"

**Vérifiez** :
1. Le fichier `email.properties` existe bien dans `src/main/resources/`
2. Les valeurs ne contiennent pas d'espaces en trop
3. L'email du client existe dans la base de données
4. Les logs console pour voir l'erreur exacte

### Problème : "Email va dans spam"

**Solutions** :
1. Ajoutez votre email à la liste blanche du client
2. Utilisez un domaine personnalisé (pas @gmail.com)
3. Configurez SPF/DKIM pour votre domaine

---

## 📊 Limites d'Envoi

| Fournisseur | Limite quotidienne | Note |
|-------------|-------------------|------|
| Gmail | 500 emails/jour | Compte gratuit |
| Gmail Workspace | 2000 emails/jour | Compte payant |
| Outlook | 300 emails/jour | Compte gratuit |
| SMTP personnalisé | Selon hébergeur | Variable |

---

## 🎨 Personnalisation Avancée

### Modifier l'expéditeur

Dans `email.properties` :
```properties
email.from.name=Boutique AfkArt
```

L'email apparaîtra comme : **"Boutique AfkArt <votre.email@gmail.com>"**

### Ajouter un email de réponse différent

Modifiez `EmailService.java` ligne 136 :
```java
message.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
message.setReplyTo(InternetAddress.parse("support@afkart.com")); // ← Ajoutez cette ligne
```

---

## 📈 Monitoring des Emails

### Logs automatiques

Le système log automatiquement :
- ✅ Emails envoyés avec succès
- ❌ Erreurs d'envoi
- 📧 Adresse du destinataire
- 📋 Numéro de commande

Exemple :
```
Email envoyé à ahmed.benali@example.com
Commande CMD-20240424-1234 confirmée
```

### Statistiques

Pour suivre les emails envoyés, ajoutez une table en BD :

```sql
CREATE TABLE email_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    commande_id INT,
    email_destinataire VARCHAR(255),
    sujet VARCHAR(500),
    statut VARCHAR(50), -- 'envoye', 'echec'
    date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (commande_id) REFERENCES commande(id)
);
```

---

## ✅ Checklist de Configuration

- [ ] Compte email créé (Gmail/Outlook)
- [ ] Validation en 2 étapes activée (Gmail)
- [ ] Mot de passe d'application généré (Gmail)
- [ ] Fichier `email.properties` configuré
- [ ] Application relancée
- [ ] Test de commande effectué
- [ ] Logs vérifiés (mode SMTP actif)
- [ ] Email reçu dans la boîte du client

---

## 🎯 Résumé

### Sans configuration SMTP
```
Client passe commande
        ↓
Email affiché dans la console (mode preview)
        ↓
Aucun email réel envoyé
```

### Avec configuration SMTP
```
Client passe commande
        ↓
Email construit automatiquement
        ↓
Envoi via SMTP (Gmail/Outlook)
        ↓
Email reçu par le client ✉️
```

---

## 📚 Ressources

- [Configuration Gmail SMTP](https://support.google.com/a/answer/176600)
- [Configuration Outlook SMTP](https://support.microsoft.com/en-us/office/pop-imap-and-smtp-settings-8361e398-8af4-4e97-b147-6c6c4ac95353)
- [Mots de passe d'application Gmail](https://support.google.com/accounts/answer/185833)

---

## 💡 Astuce Finale

**Pour tester sans configurer SMTP** :
Le mode preview console affiche l'email complet dans les logs. C'est parfait pour :
- Développement
- Debug
- Validation du contenu
- Tests sans envoyer de vrais emails

**Pour la production** :
Configurez SMTP avec un compte dédié (ex: `noreply@afkart.com`)

---

✅ **Votre système d'email personnalisé est prêt à fonctionner !**

