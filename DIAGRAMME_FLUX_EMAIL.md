# 🎨 Diagramme : Flux d'Email Personnalisé lors d'une Commande

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                     FLUX D'EMAIL PERSONNALISÉ - COMMANDE                  ║
╚═══════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────┐
│                        1️⃣  AUTHENTIFICATION                              │
└─────────────────────────────────────────────────────────────────────────┘

          👤 Client saisit email et mot de passe
                         │
                         ▼
          ┌──────────────────────────┐
          │   LoginController        │
          │   handleConnexion()      │
          └──────────┬───────────────┘
                     │
                     ├─► AuthService.login()
                     │   SELECT * FROM user WHERE email = ?
                     │
                     ▼
          ┌──────────────────────────┐
          │   User (modèle)          │
          │   • id: 1                │
          │   • nom: "Ben Ali"       │
          │   • prenom: "Ahmed"      │
          │   • email: "ahmed.       │
          │     benali@example.com"  │  ◄── EMAIL RÉCUPÉRÉ ICI
          │   • role: CLIENT         │
          └──────────┬───────────────┘
                     │
                     ▼
          ┌──────────────────────────┐
          │  SessionManager.login()  │
          │  currentUser = user      │  ◄── STOCKÉ EN SESSION
          └──────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                    2️⃣  AJOUT D'ARTICLES AU PANIER                        │
└─────────────────────────────────────────────────────────────────────────┘

          🛒 Client parcourt le catalogue
                         │
                         ▼
          ┌──────────────────────────┐
          │  ClientCatalogue         │
          │  Controller              │
          └──────────┬───────────────┘
                     │
                     ▼
          ┌──────────────────────────┐
          │  PanierService           │
          │  .ajouterArticle()       │
          │                          │
          │  Articles en mémoire:    │
          │  - Article 1 (120 DT)    │
          │  - Article 2 (250 DT)    │
          │  - Article 3 (75.50 DT)  │
          └──────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                      3️⃣  VALIDATION DE LA COMMANDE                       │
└─────────────────────────────────────────────────────────────────────────┘

          📦 Client valide sa commande
                         │
                         ▼
          ┌──────────────────────────┐
          │ ValiderCommande          │
          │ Controller               │
          │ handleValider()          │
          └──────────┬───────────────┘
                     │
                     ├─► User user = SessionManager.getCurrentUser()
                     │   • id: 1
                     │   • email: ahmed.benali@example.com
                     │
                     ├─► Création Commande:
                     │   commande.setClientId(user.getId())  ◄── ID = 1
                     │   commande.setNomClient("Ahmed Ben Ali")
                     │   commande.setAdresseLivraison(...)
                     │   commande.addArticle(article1)
                     │   commande.addArticle(article2)
                     │   commande.setTotal(445.50)
                     │
                     ▼
          ┌──────────────────────────┐
          │  Commande (modèle)       │
          │  • clientId: 1           │  ◄── LIEN VERS L'EMAIL
          │  • nomClient: "Ahmed..." │
          │  • total: 445.50 DT      │
          │  • articles: [3]         │
          └──────────┬───────────────┘
                     │
                     ▼
                serviceCommande.ajouter(commande)


┌─────────────────────────────────────────────────────────────────────────┐
│              4️⃣  ENREGISTREMENT ET ENVOI AUTOMATIQUE D'EMAIL            │
└─────────────────────────────────────────────────────────────────────────┘

          ┌──────────────────────────────────────────────────┐
          │  ServiceCommande                                 │
          │  .ajouter(commande)                              │
          └──────────┬───────────────────────────────────────┘
                     │
                     ├─► preparerCommande(commande)
                     │   • Génère numéro: CMD-20240424-1234
                     │   • Normalise téléphone
                     │
                     ├─► validerCommande(commande)
                     │   • Vérifie total > 0
                     │   • Vérifie adresse (min 10 chars)
                     │
                     ├─► generarMessagePersonnalise(commande)
                     │   │
                     │   ├─► PersonalizedMessageService
                     │   │   .generateOrderConfirmation()
                     │   │   
                     │   │   Génère message IA:
                     │   │   "Bonjour Ahmed Ben Ali,
                     │   │    Nous sommes ravis de vous
                     │   │    compter parmi nos clients..."
                     │   │
                     │   └─► commande.setMessagePersonnalise(message)
                     │       commande.setAiGenerated(true)
                     │
                     ├─► INSERT INTO commande (...)
                     │   VALUES (...)
                     │   • Enregistrement en base de données
                     │   • Récupère ID généré: 42
                     │
                     └─► envoyerEmailConfirmation(commande)  ◄── AUTOMATIQUE !
                         │
                         ▼
          ┌──────────────────────────────────────────────────┐
          │  envoyerEmailCommande(commande)                  │
          └──────────┬───────────────────────────────────────┘
                     │
                     ├─► obtenirEmailDuClient(clientId=1)
                     │   │
                     │   │  SELECT email FROM user WHERE id = 1
                     │   │
                     │   └─► email = "ahmed.benali@example.com"
                     │       ▲
                     │       │
                     │       └─── EMAIL RÉCUPÉRÉ DEPUIS LA BD
                     │
                     ▼
          ┌──────────────────────────────────────────────────┐
          │  EmailService                                    │
          │  .envoyerConfirmationCommande()                  │
          └──────────┬───────────────────────────────────────┘
                     │
                     ├─► buildOrderConfirmationHtml(commande)
                     │   │
                     │   │  Construction du HTML:
                     │   │  • En-tête AfkArt
                     │   │  • Message personnalisé IA
                     │   │  • Détails commande
                     │   │  • Liste articles avec prix
                     │   │  • Total: 445.50 DT
                     │   │  • Pied de page
                     │   │
                     │   └─► HTML complet généré
                     │
                     └─► sendEmail(
                           to: "ahmed.benali@example.com",  ◄── EMAIL CIBLE
                           subject: "Confirmation CMD-...",
                           html: "..."
                         )


┌─────────────────────────────────────────────────────────────────────────┐
│                    5️⃣  ENVOI SMTP OU PREVIEW CONSOLE                     │
└─────────────────────────────────────────────────────────────────────────┘

          ┌──────────────────────────┐
          │  sendEmail()             │
          └──────────┬───────────────┘
                     │
                     ├─► smtpEnabled ?
                     │
         ┌───────────┴────────────┐
         │                        │
         ▼                        ▼
    ✅ OUI                   ❌ NON
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌──────────────────────┐
│ sendViaSmtp()   │    │ printPreview()       │
└────────┬────────┘    └──────────┬───────────┘
         │                        │
         ├─► Configuration        ├─► Affichage console:
         │   • host: smtp.        │   
         │     gmail.com          │   ===============
         │   • port: 587          │   EMAIL AFKART
         │   • auth: username/    │   ===============
         │     password           │   TO: ahmed.benali
         │                        │       @example.com
         ├─► MimeMessage          │   SUBJECT: Confirm...
         │   • FROM: AfkArt       │   -------------
         │   • TO: ahmed.benali   │   <html>...
         │     @example.com       │   
         │   • Subject: "Conf..." │   
         │   • Content: HTML      │   
         │                        │
         ├─► Transport.send()     │
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌──────────────────────┐
│ ✉️  EMAIL ENVOYÉ │    │ 🖥️  PREVIEW AFFICHÉ  │
└─────────────────┘    └──────────────────────┘
         │
         ▼
    SMTP Server
    (Gmail/Outlook)
         │
         ▼
    📧 Boîte email
       du client


┌─────────────────────────────────────────────────────────────────────────┐
│                   6️⃣  RÉCEPTION PAR LE CLIENT                            │
└─────────────────────────────────────────────────────────────────────────┘

         📱 ahmed.benali@example.com
                    │
                    ▼
         ┌────────────────────────┐
         │  📧 Nouvel email reçu  │
         │                        │
         │  DE : AfkArt           │
         │  SUJET : Confirmation  │
         │          de votre      │
         │          commande...   │
         └────────┬───────────────┘
                  │
                  ▼ (Ouvre l'email)
         ┌────────────────────────┐
         │  🎨 Email HTML         │
         │  ═══════════════════   │
         │  AfkArt — Confirmation │
         │                        │
         │  ✨ Bonjour Ahmed      │
         │     Ben Ali,           │
         │                        │
         │  Nous sommes ravis...  │
         │                        │
         │  📦 Détails:           │
         │  • N° CMD-20240424...  │
         │  • Total: 445.50 DT    │
         │  • Adresse: ...        │
         │                        │
         │  Articles:             │
         │  • Poterie — 120 DT    │
         │  • Tapis — 250 DT      │
         │  • Lampe — 75.50 DT    │
         └────────────────────────┘


╔═══════════════════════════════════════════════════════════════════════════╗
║                              RÉSUMÉ                                       ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                           ║
║  1️⃣  EMAIL récupéré lors de la connexion (AuthService → User.email)      ║
║  2️⃣  EMAIL stocké en session (SessionManager.currentUser)                ║
║  3️⃣  CLIENT_ID lié à l'email dans la BD (table user)                     ║
║  4️⃣  COMMANDE créée avec clientId de l'utilisateur connecté              ║
║  5️⃣  EMAIL récupéré depuis la BD via clientId                            ║
║  6️⃣  EMAIL PERSONNALISÉ envoyé automatiquement                           ║
║                                                                           ║
║  ✅ Le client reçoit un email à l'adresse de son compte connecté         ║
║                                                                           ║
╚═══════════════════════════════════════════════════════════════════════════╝
```

## 📊 Tables de la Base de Données

```sql
┌─────────────────────────────────────────────────────────────┐
│  TABLE: user                                                │
├────────────┬──────────────────────────────────────────────┤
│  id (PK)   │  1                                           │
│  nom       │  "Ben Ali"                                   │
│  prenom    │  "Ahmed"                                     │
│  email     │  "ahmed.benali@example.com"  ◄── IMPORTANT  │
│  role      │  "CLIENT"                                    │
└────────────┴──────────────────────────────────────────────┘
              ▲
              │
              │ FK: client_id
              │
┌─────────────────────────────────────────────────────────────┐
│  TABLE: commande                                            │
├────────────┬──────────────────────────────────────────────┤
│  id (PK)   │  42                                          │
│  numero    │  "CMD-20240424-1234"                         │
│  client_id │  1  ◄── LIEN VERS user.id                   │
│  total     │  445.50                                      │
│  statut    │  "en_attente"                                │
│  message_  │  "Bonjour Ahmed Ben Ali,..."                 │
│  personnali│                                              │
│  ai_genera │  true                                        │
│  ted       │                                              │
└────────────┴──────────────────────────────────────────────┘
```

## 🎯 Points Clés

1. **L'EMAIL EST TOUJOURS CELUI DU COMPTE CONNECTÉ**
   - Récupéré lors de la connexion
   - Stocké dans `SessionManager.currentUser`
   - Lié à `clientId` dans la commande
   - Récupéré depuis la BD via `obtenirEmailDuClient(clientId)`

2. **ENVOI AUTOMATIQUE**
   - Pas besoin d'action manuelle
   - Déclenché par `serviceCommande.ajouter()`

3. **PERSONNALISATION**
   - Message IA avec le nom du client
   - Liste des articles commandés
   - Total et détails de livraison

4. **FIABILITÉ**
   - Mode SMTP pour envoi réel
   - Mode console pour debug/preview
   - Gestion d'erreurs robuste

