# Module AI Personalized Messages - AfkArt

Ce module génère automatiquement des messages personnalisés quand une commande est validée.

## Ce qui est implémenté

- **PersonalizedMessageService** : Appelle Hugging Face API (Mistral-7B) pour générer des messages
- **Fallback automatique** : 3 templates prédéfinis si l'API Hugging Face est indisponible
- **Intégration dans ServiceCommande** : Génération lors de `ajouter(Commande)`
- **Persistance** : Message sauvegardé en base avec flag `ai_generated`
- **Support multi-article** : Construit le prompt avec la liste des articles

## Architecture

```
Client Valide Commande
    ↓
ServiceCommande.ajouter(commande)
    ↓
PersonalizedMessageService.genererMessagePersonnalise()
    ↓
Appel Hugging Face API (Mistral-7B)
    ├── ✅ Succès → Message unique
    └── ❌ Échoue → Template fallback
    ↓
Message sauvegardé en DB + flag ai_generated
    ↓
Email avec message personnalisé
```

## Tables modifiées

### Colonne ajoutée à `commande`

- `message_personnalise` (LONGTEXT, utf8mb4)
- `ai_generated` (BOOLEAN)

## Mapping API

Le projet JavaFX + JDBC ne possède pas d'API REST native, mais voici le mapping :

- `POST /api/commandes` ← `ServiceCommande.ajouter(commande)`
  - Génère automatiquement `message_personnalise`
  - Définit `ai_generated = true|false`

## Configuration Hugging Face

Pour activer l'appel à l'API Hugging Face, définir la variable d'environnement :

```bash
export HUGGING_FACE_API_KEY="hf_xxxxxxxxxxxxx"
```

Si la clé est absente → Fallback automatique

## Fallback Messages

3 templates prédéfinis (rotation) :

```
1. Chère {nom}, merci pour votre belle commande ! Nous préparons vos {nb} article(s) avec soin. Votre total : {total} DT. Livraison à {adresse}.

2. Bonjour {nom}, votre commande de {nb} article(s) ({total} DT) est confirmée ! Nos artisans travaillent déjà sur vos pièces uniques.

3. Merci {nom} ! Votre commande ({total} DT) pour {nb} article(s) est en cours de traitement. À très bientôt à {adresse} !
```

## Smoke Test

Classe de test : `org.example.tools.PersonalizedMessageSmokeTest`

```powershell
Set-Location "C:\Users\issra\IdeaProjects\projetpijava"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" -DskipTests compile
java -cp target/classes org.example.tools.PersonalizedMessageSmokeTest
```

## Exemple de réponse attendue

```json
{
  "id": 42,
  "numero": "CMD-20260424-ABC123",
  "client": "benghrib issra",
  "total": 500.00,
  "message_personnalise": "Chère benghrib issra, merci pour votre confiance ! Votre vase en céramique artisanale vous attend avec impatience...",
  "ai_generated": true,
  "date": "2026-04-24"
}
```

## Modèle AI utilisé

- **API** : Hugging Face Inference API
- **Modèle** : `mistralai/Mistral-7B-Instruct-v0.1`
- **Timeout** : 10 secondes
- **Max tokens** : 200

## Notes

- Le message est généré **une fois** à la création de la commande
- Si l'API Hugging Face échoue ou timeout → Fallback automatique
- Aucune interruption du processus de commande
- Le message est inclu dans l'email de confirmation (à implémenter dans le service email)

