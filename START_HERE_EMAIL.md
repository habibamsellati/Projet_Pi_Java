# ⚡ SOLUTION RAPIDE : Recevoir les Emails de Confirmation

## 🔴 PROBLÈME
Vous ne recevez PAS d'email quand vous passez une commande.

## ✅ SOLUTION (10 minutes)

### ÉTAPE 1 : Mot de passe Gmail (5 min)

1. **Ouvrez** : https://myaccount.google.com/security
2. **Activez** "Validation en 2 étapes" (si pas déjà fait)
3. **Cliquez** sur "Mots de passe des applications"
4. **Sélectionnez** : Courrier → Autre → "AfkArt"
5. **Copiez** le mot de passe généré (sans espaces)

**Exemple** : `abcd efgh ijkl mnop` → Copiez comme : `abcdefghijklmnop`

---

### ÉTAPE 2 : Modifier le fichier (2 min)

1. **Ouvrez** le fichier :
   ```
   src/main/resources/email.properties
   ```

2. **Trouvez** ces lignes :
   ```properties
   email.smtp.username=VOTRE_EMAIL@gmail.com
   email.smtp.password=VOTRE_MOT_DE_PASSE_APPLICATION
   email.from=VOTRE_EMAIL@gmail.com
   ```

3. **Remplacez** par vos vraies données :
   ```properties
   email.smtp.username=votre.vraie.adresse@gmail.com
   email.smtp.password=abcdefghijklmnop
   email.from=votre.vraie.adresse@gmail.com
   ```

4. **Enregistrez** le fichier

---

### ÉTAPE 3 : Test (3 min)

1. **Fermez** complètement votre application
2. **Relancez**-la
3. **Vérifiez** les logs (vous devriez voir) :
   ```
   EmailService: mode SMTP actif
   ```
4. **Connectez-vous** comme client
5. **Passez une commande**
6. **Vérifiez** votre boîte email (et le dossier spam)

---

## ✅ RÉSULTAT

Vous recevrez un email comme celui-ci :

```
╔═══════════════════════════════════════════════╗
║  DE : AfkArt                                  ║
║  SUJET : Confirmation de votre commande       ║
╠═══════════════════════════════════════════════╣
║                                               ║
║  Bonjour [Votre Nom],                         ║
║                                               ║
║  Votre commande a été enregistrée !           ║
║                                               ║
║  📦 Numéro : CMD-20240424-1234                ║
║  💰 Total : 445.50 DT                         ║
║                                               ║
║  Articles :                                   ║
║  • Article 1 — 120.00 DT                      ║
║  • Article 2 — 250.00 DT                      ║
║  • Article 3 — 75.50 DT                       ║
║                                               ║
║  Merci !                                      ║
║                                               ║
╚═══════════════════════════════════════════════╝
```

---

## ❓ PROBLÈMES ?

### "Je ne trouve pas Mots de passe des applications"
→ Activez d'abord la validation en 2 étapes

### "Authentication failed"
→ Vérifiez que vous avez copié le bon mot de passe (sans espaces)

### "Email dans les spams"
→ C'est normal ! Marquez comme "Pas un spam"

### "Toujours rien"
→ Consultez : **SOLUTION_PAS_DEMAIL.md** (guide détaillé)

---

## 📚 BESOIN D'AIDE ?

**Guides disponibles** :
- `SOLUTION_PAS_DEMAIL.md` → Solution complète
- `GUIDE_VISUEL_EMAIL.md` → Instructions visuelles
- `README_CONFIGURATION_EMAIL.md` → Vue d'ensemble

---

**⏱️ Temps : 10 minutes**
**🎯 Résultat : Emails automatiques !**

