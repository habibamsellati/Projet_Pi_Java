# Login simple par roles (ARTISANT / CLIENT / ADMIN)

## But

Implémenter une connexion JavaFX basée sur la table `user` de la base `piprojet1`, puis rediriger l'utilisateur vers une interface selon son rôle.

## Fichiers ajoutés

- `src/main/java/org/example/models/Role.java`
- `src/main/java/org/example/models/User.java`
- `src/main/java/org/example/services/AuthService.java`
- `src/main/java/org/example/utils/SessionManager.java`
- `src/main/java/org/example/utils/PasswordVerifier.java`
- `src/main/java/org/example/utils/SceneNavigator.java`
- `src/main/java/org/example/controllers/LoginController.java`
- `src/main/java/org/example/controllers/ArtisanDashboardController.java`
- `src/main/java/org/example/controllers/ClientDashboardController.java`
- `src/main/resources/fxml/Login.fxml`
- `src/main/resources/fxml/ArtisanDashboard.fxml`
- `src/main/resources/fxml/ClientDashboard.fxml`

## Fichier modifie

- `src/main/java/org/example/MainAPP.java` (démarrage sur `Login.fxml`)

## Flux

1. L'application démarre sur `Login.fxml`.
2. L'utilisateur saisit email + mot de passe.
3. `AuthService` lit la table `user` (`email`, `motdepasse`, `role`).
4. Si le rôle est `ARTISANT` -> `ArtisanDashboard.fxml`.
5. Si le rôle est `CLIENT` -> `ClientDashboard.fxml`.
6. Si le rôle est `ADMIN` -> backoffice (dashboard backoffice).
7. Sinon, message d'erreur (rôle non géré).

## Vérification mot de passe

- Le projet tente une vérification simple compatible hash PHP via `PasswordVerifier`.
- Par défaut, il appelle `php` (CLI) avec `password_verify(...)`.
- Vous pouvez forcer le binaire PHP avec la variable d'environnement `PHP_BINARY`.

Exemple PowerShell :

```powershell
$env:PHP_BINARY = "C:\\xamppp\\php\\php.exe"
```

## Navigation role-based

- Dashboard artisan : accès à la publication/gestion d'articles.
- Dashboard client : accès au catalogue et commandes.
- Bouton Déconnexion : nettoie `SessionManager` et revient au login.

