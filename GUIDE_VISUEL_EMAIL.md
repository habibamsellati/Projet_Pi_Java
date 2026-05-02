# 📧 GUIDE VISUEL : Comment Recevoir les Emails de Confirmation

## 🔴 PROBLÈME

Quand vous passez une commande, vous **ne recevez PAS d'email** de confirmation.

**Pourquoi ?** Le fichier de configuration n'est pas rempli avec vos vraies données.

---

## ✅ SOLUTION EN 4 ÉTAPES (10 minutes)

```
┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 1 : GÉNÉRER UN MOT DE PASSE D'APPLICATION GMAIL     │
└─────────────────────────────────────────────────────────────┘

1. Ouvrez votre navigateur
2. Allez sur : https://myaccount.google.com/security
3. Cherchez "Validation en 2 étapes"
4. Si désactivé, activez-le (suivez les instructions)
5. Retournez sur : https://myaccount.google.com/security
6. Cherchez "Mots de passe des applications"
7. Cliquez dessus
8. Sélectionnez :
   - Application : Courrier
   - Appareil : Autre → Tapez "AfkArt"
9. Cliquez sur "Générer"
10. Un mot de passe apparaît : "xxxx xxxx xxxx xxxx"
11. COPIEZ-LE (vous en aurez besoin !)
12. Enlevez les espaces : "xxxxxxxxxxxxxxxx"

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 2 : MODIFIER LE FICHIER DE CONFIGURATION            │
└─────────────────────────────────────────────────────────────┘

1. Dans votre projet, ouvrez le fichier :
   📁 src/main/resources/email.properties

2. Trouvez ces lignes :
   email.smtp.username=VOTRE_EMAIL@gmail.com
   email.smtp.password=VOTRE_MOT_DE_PASSE_APPLICATION
   email.from=VOTRE_EMAIL@gmail.com

3. Remplacez :
   - VOTRE_EMAIL@gmail.com → Par votre vraie adresse Gmail
   - VOTRE_MOT_DE_PASSE_APPLICATION → Par le mot de passe copié

4. Exemple APRÈS modification :
   email.smtp.username=ahmed.benali@gmail.com
   email.smtp.password=abcdkqkljdpzemml
   email.from=ahmed.benali@gmail.com

5. ENREGISTREZ le fichier

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 3 : REDÉMARRER L'APPLICATION                         │
└─────────────────────────────────────────────────────────────┘

1. FERMEZ complètement votre application JavaFX
2. RELANCEZ-LA
3. Regardez les logs (console) au démarrage
4. Vous devriez voir :
   ✅ "EmailService: mode SMTP actif (host=smtp.gmail.com, from=votre.email@gmail.com)"

Si vous voyez :
   ❌ "EmailService: SMTP incomplet, mode preview console actif"
   → Retournez à l'étape 2 et vérifiez votre configuration

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 4 : TESTER                                           │
└─────────────────────────────────────────────────────────────┘

1. Connectez-vous comme CLIENT
   (Utilisez un compte avec une vraie adresse email dans la BD)

2. Ajoutez des articles au panier

3. Validez la commande

4. Regardez les logs :
   ✅ "Email envoyé à votre.email@example.com"

5. Ouvrez votre boîte email

6. Cherchez un email de "AfkArt"
   Sujet : "Confirmation de votre commande CMD-..."

7. Si vous ne le voyez pas, vérifiez le dossier SPAM

✅ Vous devriez recevoir l'email de confirmation !
```

---

## 🎥 CAPTURE D'ÉCRAN : À QUOI ÇA RESSEMBLE

### 1. Page Gmail - Mots de passe des applications

```
╔═══════════════════════════════════════════════════╗
║  Mots de passe des applications                   ║
╠═══════════════════════════════════════════════════╣
║                                                   ║
║  Sélectionner l'application : [Courrier ▼]       ║
║  Sélectionner l'appareil : [Autre ▼]             ║
║                                                   ║
║  Nom de l'appareil : [AfkArt____________]        ║
║                                                   ║
║  [         Générer         ]                      ║
║                                                   ║
╚═══════════════════════════════════════════════════╝
```

### 2. Mot de passe généré

```
╔═══════════════════════════════════════════════════╗
║  Votre mot de passe pour AfkArt                   ║
╠═══════════════════════════════════════════════════╣
║                                                   ║
║  Votre mot de passe d'application :               ║
║                                                   ║
║  ┌─────────────────────────────────────────────┐ ║
║  │  abcd efgh ijkl mnop                        │ ║
║  └─────────────────────────────────────────────┘ ║
║                                                   ║
║  Copiez-le et collez-le dans l'application        ║
║  (sans les espaces)                               ║
║                                                   ║
║  [    OK    ]                                     ║
║                                                   ║
╚═══════════════════════════════════════════════════╝
```

### 3. Fichier email.properties AVANT

```properties
email.smtp.username=VOTRE_EMAIL@gmail.com
email.smtp.password=VOTRE_MOT_DE_PASSE_APPLICATION
```

### 4. Fichier email.properties APRÈS

```properties
email.smtp.username=ahmed.benali@gmail.com
email.smtp.password=abcdefghijklmnop
```

### 5. Logs de l'application (succès)

```
[INFO] EmailService: mode SMTP actif (host=smtp.gmail.com, from=ahmed.benali@gmail.com)
[INFO] Email envoyé à client@example.com
```

### 6. Email reçu

```
┌─────────────────────────────────────────────────┐
│ 📧 Gmail - Boîte de réception                   │
├─────────────────────────────────────────────────┤
│                                                 │
│ ✉️ AfkArt                                       │
│    Confirmation de votre commande CMD-20240...  │
│    Il y a 2 minutes                             │
│                                                 │
│    Bonjour Ahmed Ben Ali,                       │
│    Nous sommes ravis de vous compter parmi...   │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## ❓ QUESTIONS FRÉQUENTES

**Q : Je n'arrive pas à trouver "Mots de passe des applications"**
R : Vous devez d'abord activer la validation en 2 étapes sur votre compte Gmail

**Q : Puis-je utiliser mon mot de passe Gmail normal ?**
R : NON ! Vous devez utiliser un mot de passe d'application

**Q : Ça fonctionne avec Outlook ?**
R : Oui, voir les instructions alternatives dans email.properties

**Q : L'email va dans les spams**
R : C'est normal au début. Marquez-le comme "Pas un spam"

**Q : J'ai une erreur "Authentication failed"**
R : Vérifiez que vous avez copié le bon mot de passe sans espaces

**Q : Combien d'emails puis-je envoyer ?**
R : Gmail gratuit : 500 emails/jour (largement suffisant)

---

## 🆘 BESOIN D'AIDE ?

### Vérifications rapides

✅ **Check 1** : Mot de passe d'application généré ?
✅ **Check 2** : Fichier email.properties modifié et enregistré ?
✅ **Check 3** : Application redémarrée ?
✅ **Check 4** : Logs montrent "mode SMTP actif" ?
✅ **Check 5** : Email du client dans la BD est correct ?

### Si ça ne marche toujours pas

1. Vérifiez les logs pour voir l'erreur exacte
2. Vérifiez que le port 587 n'est pas bloqué
3. Essayez avec un autre compte Gmail
4. Consultez : CONFIGURATION_EMAIL_URGENTE.md

---

## ✅ SUCCÈS !

Une fois configuré, **CHAQUE commande** enverra automatiquement un email personnalisé :

- ✉️ À l'adresse du compte client connecté
- 🎨 Avec un design professionnel
- 📦 Avec les détails de la commande
- 💰 Avec le total et les articles
- 🤖 Avec un message personnalisé par IA

**Aucune action manuelle nécessaire !**

---

**⏱️ Temps total : 10 minutes**
**🎯 Résultat : Emails automatiques à chaque commande !**

