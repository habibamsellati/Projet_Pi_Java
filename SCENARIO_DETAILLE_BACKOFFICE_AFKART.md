# Scenario detaille - Backoffice afk'art

## Objectif
Definir le cycle de vie complet du backoffice afk'art pour les modules admin:
- Dashboard
- Utilisateurs
- Artisans
- Articles
- Commandes
- Livraisons
- Reclamations
- Categories
- Produits recyclables

## Vue globale
La plateforme comporte 4 roles principaux:
- `CLIENT`
- `ARTISAN`
- `LIVREUR`
- `ADMIN`

Le role `ADMIN` supervise tous les modules et applique les validations metier.

---

## Scenario 1 - Inscription & gestion des utilisateurs
### Flux
1. Un visiteur cree un compte.
2. Le compte apparait dans le module `Utilisateurs` du backoffice.
3. L'admin peut consulter, bloquer, supprimer.

### Regles metier
- Un utilisateur peut avoir le role `CLIENT`, `ARTISAN` ou `LIVREUR`.
- Un admin ne peut pas se supprimer lui-meme.
- Un compte bloque ne peut plus se connecter ni passer commande.

---

## Scenario 2 - Publication d'article par un artisan
### Flux
1. L'artisan publie un article.
2. L'article apparait dans la liste admin.
3. L'admin peut valider ou supprimer.

### Regles metier
- Artisan: modification/suppression uniquement de ses propres articles.
- Admin: modification/suppression sur tous les articles.

---

## Scenario 3 - Cycle de vie commande
### Flux
1. Le client passe une commande.
2. L'admin la voit dans le backoffice.
3. L'admin confirme la commande.
4. L'admin assigne un livreur.
5. La commande passe en livraison puis livree.

### Regles metier
- Transition recommandee: `en_attente -> confirmee -> livree`.
- Annulation possible avant `livree`.
- Les actions de statut global sont reservees a l'admin.

---

## Scenario 4 - Gestion des reclamations
### Flux
1. Le client depose une reclamation.
2. L'admin la traite (analyse, reponse, decision).
3. L'admin cloture (resolue/rejetee).

### Statuts
- `ouverte`
- `en_cours`
- `resolue`
- `rejetee`

---

## Scenario 5 - Validation artisan
### Flux
1. Un artisan cree son profil.
2. Le profil passe en attente.
3. L'admin valide ou rejette.
4. Si valide, l'artisan peut publier et recevoir commandes.

### Statuts
- `en_attente`
- `actif`
- `rejete`

---

## Tableau recapitulatif
| Module | Acteur principal | Actions admin | Statuts |
|---|---|---|---|
| Dashboard | Admin | Consulter KPI et graphiques | - |
| Utilisateurs | Admin | Voir, bloquer, supprimer | actif / bloque |
| Artisans | Admin | Valider profil, desactiver | en_attente / actif / rejete |
| Articles | Admin | Valider, supprimer, moderer commentaires | en_attente / publie / supprime |
| Commandes | Admin | Confirmer, annuler, changer statut, exporter | en_attente / confirmee / annulee / en_livraison / livree |
| Livraisons | Admin + Livreur | Assigner livreur, suivre | assignee / en_route / livree |
| Reclamations | Admin | Traiter, repondre, cloturer | ouverte / en_cours / resolue / rejetee |
| Categories | Admin | CRUD categories | actif / inactif |
| Produits recyclables | Admin | Approuver propositions | en_attente / approuve |

---

## RBAC cible
| Action | CLIENT | ARTISAN | ADMIN |
|---|---|---|---|
| Voir articles | Oui | Oui | Oui |
| Creer article | Non | Oui | Oui |
| Modifier article | Non | Oui (sien) | Oui (tous) |
| Supprimer article | Non | Oui (sien) | Oui (tous) |
| Liker article | Oui | Non | Non |
| Commenter article | Oui | Non | Non |
| Passer commande | Oui | Non | Non |
| Voir ses commandes | Oui | Non | Oui (toutes) |
| Changer statut commande | Non | Non | Oui |
| Annuler commande | Oui (si en_attente) | Non | Oui (si non livree) |

---

## Enums Java recommandes
```java
public enum StatutCommande {
    EN_ATTENTE, CONFIRMEE, EN_COURS_LIVRAISON, LIVREE, ANNULEE
}

public enum StatutUtilisateur {
    ACTIF, BLOQUE, EN_ATTENTE
}

public enum Role {
    ROLE_CLIENT, ROLE_ARTISAN, ROLE_LIVREUR, ROLE_ADMIN
}

public enum StatutReclamation {
    OUVERTE, EN_COURS, RESOLUE, REJETEE
}
```

---

## Notes d'integration (ce repo)
Ce repo principal est en Java/JavaFX. Ce scenario sert de reference pour:
1. continuer l'implementation JavaFX actuelle (controllers/services),
2. ou monter un module web Spring Boot backoffice en parallele,
3. ou faire une migration progressive vers une architecture web complete.

