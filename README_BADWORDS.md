# Module Bad Words - AfkArt

Ce module ajoute une moderation des commentaires avant publication.

## Ce qui est implemente

- Verification du texte avant publication.
- Liste des mots interdits en base MySQL (`bad_words`).
- Ajout/suppression de mots interdits (controle admin).
- Trois strategies de moderation:
  - `BLOQUER`
  - `CENSURER`
  - `SIGNALER`
- Journal des commentaires signales (`commentaire_moderation_flag`).

## Mapping avec la spec API

Le projet est JavaFX + JDBC (pas de REST natif), mais les methodes metier correspondent aux endpoints:

- `POST /api/commentaires/verifier`
  - `ServiceCommentaire.verifierAvantPublication(String texte)`
- `GET /api/badwords`
  - `ServiceCommentaire.listerBadWords()`
- `POST /api/badwords`
  - `ServiceCommentaire.ajouterBadWord(String mot, User admin)`
- `DELETE /api/badwords/{mot}`
  - `ServiceCommentaire.supprimerBadWord(String mot, User admin)`

## Tables creees

- `bad_words`
- `commentaire_moderation_flag`

## Smoke test rapide

Classe de test manuel: `org.example.tools.BadWordsSmokeTest`

Exemple (avec MySQL demarre):

```powershell
Set-Location "C:\Users\issra\IdeaProjects\projetpijava"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" -DskipTests compile
java -cp target/classes org.example.tools.BadWordsSmokeTest "Ce produit est idiot"
```

