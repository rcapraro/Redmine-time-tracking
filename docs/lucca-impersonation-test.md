# Test : absences Lucca d'un utilisateur impersonné

Quand un admin Redmine agit pour un autre utilisateur, la fenêtre Absences doit afficher les absences Lucca **de
cette personne**, lues avec la session Lucca de l'admin. Ce document explique comment le vérifier dans l'app, puis
comment diagnostiquer dans le navigateur si un cas ne marche pas. Tout est **en lecture seule**, sauf l'import
lui-même.

## Comment l'app procède

1. Elle lit les personnes de ton planning Lucca, c'est-à-dire celles que tu vois par défaut sur la page Planning :
   `/timmi-absences/api/planning/v1.0/users?population.relativeUsers=0`.
2. Elle y cherche l'utilisateur Redmine impersonné :
   - d'abord par prénom et nom, sans tenir compte de la casse, des accents, des espaces ni des tirets ;
   - sinon avec la règle des e-mails Pack Solutions appliquée au login Redmine : initiale du prénom + nom
     (`jdupont`).

   En cas d'homonymes, l'app ne choisit pas au hasard : elle ne retient personne.
3. Elle lit les absences de la personne trouvée : `/api/v3/leaves?leavePeriod.ownerId=<id>`. Les jours de télétravail
   (`isRealLeave: false`) sont écartés.

## Test dans l'app

Prendre 2 ou 3 collègues, si possible dans des équipes différentes, dont tu connais au moins une absence en novembre
ou décembre 2026.

1. Impersonner le collègue, puis ouvrir Absences.
2. Le bandeau doit indiquer « Absences Lucca de <Prénom Nom> », et ses absences connues doivent apparaître.
3. Les jours de télétravail ne doivent pas apparaître.
4. Pour un collègue **hors de ton planning**, la fenêtre doit afficher « <Prénom Nom> n'apparaît pas dans votre
   planning Lucca… ».
5. Optionnel : importer une absence, puis vérifier qu'elle apparaît dans **son** Redmine et pas dans le tien.

## Si ça ne marche pas : diagnostic dans le navigateur

Se connecter à https://packsolutions.ilucca.net, puis ouvrir ces adresses. Firefox affiche le JSON de façon lisible.

**A. Qui est dans ton planning ?**

```
https://packsolutions.ilucca.net/timmi-absences/api/planning/v1.0/users?page=1&limit=500&fields.root=count&population.relativeUsers=0
```

Chercher le collègue, puis noter son `id`, son `firstName` et son `lastName` tels qu'écrits dans Lucca. Les comparer
au prénom, au nom et au login de sa fiche Redmine : Administration > Utilisateurs.

**B. Ses absences**

```
https://packsolutions.ilucca.net/api/v3/leaves?leavePeriod.ownerId=<id>&date=between,2026-11-01,2026-12-31&paging=0,1000&fields=id,date,isAM,isActive,isRealLeave,leaveAccount[id,name],leavePeriod[id,isConfirmed]
```

| Ce qu'on observe                                                    | Cause probable                                                                  |
|---------------------------------------------------------------------|---------------------------------------------------------------------------------|
| Le collègue n'est pas dans la liste A                               | Hors de ton planning Lucca : l'app ne peut pas le voir                          |
| Il est dans A, mais l'app dit qu'il n'apparaît pas                  | Nom et login différents des deux côtés : noter les deux écritures et me les envoyer |
| B renvoie 403                                                       | Lucca refuse l'accès : l'app affiche « Lucca ne vous autorise pas… »            |
| B renvoie une liste vide alors qu'il a des absences                 | Droits Lucca filtrés sans erreur : à me signaler                                |
| B contient ses absences mais l'app ne les montre pas                | Bug de l'app : à me signaler                                                    |

## À me renvoyer en cas de problème

Pas besoin des dates d'absence. Pour chaque cas qui coince :
- la ligne du tableau qui correspond ;
- l'écriture du prénom, du nom et du login côté Redmine, et du prénom et du nom côté Lucca, si la correspondance est
  en cause.
