# ⚡ CONFIGURATION URGENTE : Recevoir les Emails de Confirmation

## 🔴 PROBLÈME ACTUEL

Vous **ne recevez pas d'emails** quand vous passez une commande parce que le SMTP n'est **pas configuré correctement**.

Le fichier `email.properties` contient des valeurs fictives :
```
email.smtp.username=tonadresse@gmail.com
email.smtp.password=ton_app_password
```

## ✅ SOLUTION RAPIDE (10 minutes)

### Option 1 : Gmail (Recommandé) 🟢

#### Étape 1 : Générer un mot de passe d'application Gmail

1. **Ouvrez** : https://myaccount.google.com/security

2. **Activez la validation en 2 étapes** (si ce n'est pas déjà fait)
   - Cliquez sur "Validation en 2 étapes"
   - Suivez les instructions pour activer

3. **Générez un mot de passe d'application** :
   - Retournez sur https://myaccount.google.com/security
   - Recherchez "Mots de passe des applications"
   - Cliquez dessus
   - Sélectionnez :
     - **Application** : Courrier
     - **Appareil** : Autre → Tapez "AfkArt"
   - Cliquez sur **Générer**
   - **Copiez le mot de passe** (16 caractères) : `xxxx xxxx xxxx xxxx`
   - ⚠️ **Enlevez les espaces** : `xxxxxxxxxxxxxxxx`

#### Étape 2 : Modifier email.properties

Ouvrez le fichier : `src/main/resources/email.properties`

Remplacez le contenu par :

```properties
# Configuration SMTP Gmail
email.smtp.host=smtp.gmail.com
email.smtp.port=587
email.smtp.username=VOTRE_VRAIE_ADRESSE@gmail.com
email.smtp.password=VOTRE_MOT_DE_PASSE_APPLICATION

# Expéditeur
email.from=VOTRE_VRAIE_ADRESSE@gmail.com
email.from.name=AfkArt
```

**Important** :
- Remplacez `VOTRE_VRAIE_ADRESSE@gmail.com` par votre vraie adresse Gmail
- Remplacez `VOTRE_MOT_DE_PASSE_APPLICATION` par le mot de passe généré (sans espaces !)
- **Ne mettez PAS** votre mot de passe Gmail normal !

#### Étape 3 : Redémarrer l'application

1. **Fermez** complètement votre application JavaFX
2. **Relancez-la**
3. **Vérifiez les logs** au démarrage :
   ```
   EmailService: mode SMTP actif (host=smtp.gmail.com, from=votre.email@gmail.com)
   ```
   ✅ Si vous voyez ce message, SMTP est activé !

#### Étape 4 : Tester

1. **Connectez-vous** comme client avec un compte ayant un **vrai email**
2. **Passez une commande**
3. **Vérifiez** :
   - Les logs : "Email envoyé à client@example.com"
   - Votre boîte email : vous devriez recevoir l'email !

---

### Option 2 : Outlook / Hotmail 🔵

Si vous préférez Outlook :

```properties
# Configuration SMTP Outlook
email.smtp.host=smtp-mail.outlook.com
email.smtp.port=587
email.smtp.username=VOTRE_ADRESSE@outlook.com
email.smtp.password=VOTRE_MOT_DE_PASSE

# Expéditeur
email.from=VOTRE_ADRESSE@outlook.com
email.from.name=AfkArt
```

**Note** : Outlook utilise votre mot de passe normal (pas de mot de passe d'application)

---

## 🧪 VÉRIFICATION

### Vérification 1 : Logs au démarrage

Quand vous lancez l'application, cherchez cette ligne :

✅ **SMTP configuré** (bon signe) :
```
EmailService: mode SMTP actif (host=smtp.gmail.com, from=votre.email@gmail.com)
```

❌ **SMTP non configuré** (problème) :
```
EmailService: SMTP incomplet, mode preview console actif
```

### Vérification 2 : Logs lors de la commande

Quand vous passez une commande :

✅ **Email envoyé** :
```
Email envoyé à client@example.com
```

❌ **Email non envoyé** :
```
================ EMAIL AFKART ================
Mode: SMTP non configuré
TO: client@example.com
...
```

### Vérification 3 : Boîte email

- Ouvrez votre boîte email
- Cherchez un email de "AfkArt"
- Sujet : "Confirmation de votre commande CMD-..."
- Si vous ne le voyez pas, vérifiez le **dossier Spam/Courrier indésirable**

---

## 🐛 DÉPANNAGE

### Problème : "Authentication failed"

**Cause** : Mot de passe incorrect

**Solution** :
1. Vérifiez que vous utilisez le **mot de passe d'application** (Gmail)
2. Pas votre mot de passe Gmail normal !
3. Vérifiez qu'il n'y a **pas d'espaces** dans le mot de passe

### Problème : "Validation en 2 étapes requise"

**Solution** :
1. Allez sur https://myaccount.google.com/security
2. Activez "Validation en 2 étapes"
3. Générez ensuite le mot de passe d'application

### Problème : Email dans les spams

**Solution** :
1. Ajoutez votre email à la liste blanche
2. Marquez l'email comme "Pas un spam"

### Problème : Aucun email reçu même après config

**Vérifiez** :
1. Que l'email du client dans la BD est correct
2. Les logs pour voir si l'email a été envoyé
3. Que le port 587 n'est pas bloqué par votre pare-feu

---

## 📋 CHECKLIST

- [ ] J'ai généré un mot de passe d'application Gmail
- [ ] J'ai modifié `email.properties` avec mes vraies données
- [ ] J'ai enlevé tous les espaces du mot de passe
- [ ] J'ai redémarré l'application
- [ ] Les logs montrent "mode SMTP actif"
- [ ] J'ai testé en passant une commande
- [ ] J'ai vérifié ma boîte email
- [ ] J'ai vérifié le dossier spam

---

## ⚡ SI VOUS ÊTES PRESSÉ

Vous pouvez aussi utiliser **Mailtrap** ou **Ethereal** pour tester rapidement sans configuration Gmail :

### Ethereal (Email de test gratuit)

1. Allez sur : https://ethereal.email/create
2. Copiez les identifiants générés
3. Mettez-les dans `email.properties`

Ça ne vous enverra pas de vrais emails, mais vous pourrez les voir sur le site Ethereal.

---

## 🎯 RÉSUMÉ

**Pourquoi vous ne recevez pas d'emails** : Le SMTP n'est pas configuré

**Solution** :
1. Générez un mot de passe d'application Gmail
2. Modifiez `email.properties`
3. Redémarrez l'application
4. Testez !

**Temps estimé** : 10 minutes

---

✅ **Après configuration, vous recevrez automatiquement un email personnalisé à chaque commande !**

