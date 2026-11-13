# Test : lire les absences Lucca d'un autre utilisateur

But : savoir si l'app peut importer les absences de l'utilisateur Redmine impersonné, avec la session Lucca de la
personne connectée (admin ou manager). Le test est **en lecture seule** : il n'écrit rien, ni dans Lucca, ni dans
Redmine.

L'app identifie aujourd'hui l'utilisateur avec `/api/v3/users/me`. Elle lit ensuite ses absences avec
`/api/v3/leaves?leavePeriod.ownerId=<son id>`. La question est de savoir si on peut remplacer cet id par celui d'un
collègue.

## Avant de commencer

- Être connecté à Lucca dans le navigateur : https://packsolutions.ilucca.net. Les URL ci-dessous réutilisent cette
  session.
- Choisir **2 ou 3 collègues**, si possible dans des équipes différentes : les droits Lucca dépendent souvent de
  l'équipe ou de l'établissement.
- Pour chacun, connaître **au moins une absence** sur novembre ou décembre 2026. Sans ça, une réponse vide ne permet
  pas de distinguer « aucune absence » de « pas le droit de voir ».
- Firefox affiche le JSON de façon lisible. Dans Chrome, cocher « Impression élégante ».

## Étape 1 — Ton propre identifiant Lucca (contrôle)

```
https://packsolutions.ilucca.net/api/v3/users/me?fields=id,firstName,lastName,mail
```

Attendu : ton `id`. C'est l'appel que fait l'app. S'il échoue, la session du navigateur n'est pas valide : se
reconnecter.

## Étape 2 — Trouver le collègue dans Lucca

L'app partirait de l'e-mail Redmine de l'utilisateur impersonné. Essayer dans l'ordre, et s'arrêter au premier qui
renvoie la bonne personne :

```
https://packsolutions.ilucca.net/api/v3/users?mail=<email>&fields=id,firstName,lastName,mail
https://packsolutions.ilucca.net/api/v3/users?lastName=<Nom>&fields=id,firstName,lastName,mail
https://packsolutions.ilucca.net/api/v3/users?name=like,<nom>&fields=id,firstName,lastName,mail
```

Noter :
- quelle variante fonctionne ;
- le code HTTP ;
- si `mail` dans Lucca est **exactement** l'e-mail du compte Redmine. Côté Redmine : Administration > Utilisateurs,
  puis la fiche de la personne.

## Étape 3 — Lire ses absences (la requête de l'app)

Remplacer `<id>` par l'id trouvé à l'étape 2 :

```
https://packsolutions.ilucca.net/api/v3/leaves?leavePeriod.ownerId=<id>&date=between,2026-11-01,2026-12-31&paging=0,1000&fields=id,date,isAM,isActive,leaveAccount[id,name],leavePeriod[id,isConfirmed]
```

Faire aussi l'appel avec **ton propre id** (étape 1), pour comparer avec ce que l'app affiche aujourd'hui.

## Comment lire le résultat

| Réponse à l'étape 3                                         | Conclusion                                                                  |
|-------------------------------------------------------------|-----------------------------------------------------------------------------|
| 200, et les absences connues du collègue sont là            | Faisable avec tes droits actuels                                            |
| 200, liste vide alors que le collègue a des absences        | Lucca filtre selon tes droits sans erreur : pas faisable pour ce collègue  |
| 401 ou 403                                                  | Pas le droit de lire ses absences                                           |
| Redirection vers la page de connexion                       | Session expirée : se reconnecter et recommencer                             |
| Tout fonctionne pour un collègue mais pas pour un autre     | Droits par équipe ou par établissement : à prévoir dans l'app               |

## À me renvoyer

Pas besoin de noms ni de dates : juste, pour chaque collègue testé :

1. la variante de l'étape 2 qui a fonctionné, et si l'e-mail Lucca = l'e-mail Redmine ;
2. le code HTTP et le **nombre** d'absences renvoyées à l'étape 3 ;
3. si ce nombre correspond à ce que tu sais de ses absences ;
4. ton rôle dans Lucca (manager, RH, admin…), si tu le connais.

Avec ça, on saura si l'import pour un autre utilisateur est possible, et pour qui. On saura aussi comment retrouver
la personne dans Lucca : par e-mail ou par nom.
