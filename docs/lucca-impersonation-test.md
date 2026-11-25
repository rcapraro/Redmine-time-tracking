# Test : absences Lucca, jours fériés et impersonation

Quand un admin Redmine agit pour un autre utilisateur, la fenêtre Absences doit afficher les absences Lucca **de
cette personne**, lues avec la session Lucca de l'admin. Ce document explique comment le vérifier dans l'app, puis
comment diagnostiquer dans le navigateur si un cas ne marche pas. Tout est **en lecture seule**, sauf l'import
lui-même.

## Comment l'app procède

1. Elle lit deux listes de personnes, en même temps :
   - l'annuaire Lucca, soit tous les salariés actuels : `/api/v3/users` ;
   - ton planning Lucca, c'est-à-dire les personnes que tu vois par défaut sur la page Planning :
     `/timmi-absences/api/planning/v1.0/users?population.relativeUsers=0`.

   Si Lucca refuse l'une des deux, l'autre suffit pour trouver quelqu'un.
2. Elle y cherche l'utilisateur Redmine impersonné par prénom et nom, en ne comparant que les lettres : la casse,
   les accents, les tirets, les apostrophes et les espaces ne comptent pas. « Hélène DUPONT-MARTIN » correspond à
   « Helene Dupont Martin », « N'Diaye » à « Ndiaye », « Lebœuf » à « Leboeuf ». Une lettre différente fait un
   autre nom : « Dupont » n'est pas « Dupond ». Le login ne sert pas.

   En cas d'homonymes dans l'entreprise, même distingués par un seul accent, l'app ne choisit pas au hasard : elle
   ne retient personne. Figurer dans l'annuaire ne donne pas le droit de lire les absences : c'est l'étape suivante
   qui le dit.
3. Elle lit les absences de la personne trouvée : `/api/v3/leaves?leavePeriod.ownerId=<id>`. Les jours de télétravail
   (`isRealLeave: false`) sont écartés.

   Pour quelqu'un hors de ton planning, une réponse vide ne prouve rien : Lucca peut ne rien renvoyer faute de
   droits. L'app l'indique alors, et ne propose à la suppression aucune absence déjà importée.
4. Elle lit ses jours fériés : `/timmi-absences/api/planning/v1.0/userDates`. Elle ne garde que les jours non
   travaillés qui portent un nom (« Armistice ») et qui tombent en semaine. Ils apparaissent sous le type « Jour
   férié », à associer à un ticket comme les autres types.

## Test dans l'app

Prendre 2 ou 3 collègues, si possible dans des équipes différentes, dont tu connais au moins une absence en novembre
ou décembre 2026.

1. Impersonner le collègue, puis ouvrir Absences.
2. Le bandeau doit indiquer « Absences Lucca de <Prénom Nom> », et ses absences connues doivent apparaître.
3. Les jours de télétravail ne doivent pas apparaître.
4. Les jours fériés de la période doivent apparaître sous le type « Jour férié », mais pas ceux qui tombent un
   week-end.
5. Pour un collègue **hors de ton planning par défaut** mais dont tu vois les absences dans Lucca, ses absences
   doivent apparaître de la même façon. Si Lucca ne te laisse pas les lire, la fenêtre doit afficher « Lucca ne vous
   autorise pas à voir les absences de <Prénom Nom> », ou bien aucune absence avec le bandeau « Lucca ne renvoie
   aucune absence pour <Prénom Nom>, qui n'est pas dans votre planning… ».
6. Optionnel : importer une absence, puis vérifier qu'elle apparaît dans **son** Redmine et pas dans le tien.

## Si ça ne marche pas : diagnostic dans le navigateur

Se connecter à https://packsolutions.ilucca.net, puis ouvrir ces adresses. Firefox affiche le JSON de façon lisible.

**A. Comment s'appelle-t-il dans Lucca ?**

L'annuaire :

```
https://packsolutions.ilucca.net/api/v3/users?fields=id,firstName,lastName&orderBy=id,asc&paging=0,1000
```

Ton planning :

```
https://packsolutions.ilucca.net/timmi-absences/api/planning/v1.0/users?page=1&limit=500&fields.root=count&population.relativeUsers=0
```

Noter son `id`, son `firstName` et son `lastName` tels qu'écrits dans Lucca, et dans laquelle des deux listes il
figure. Les comparer au prénom et au nom de sa fiche Redmine : Administration > Utilisateurs.

**B. Ses absences**

```
https://packsolutions.ilucca.net/api/v3/leaves?leavePeriod.ownerId=<id>&date=between,2026-11-01,2026-12-31&paging=0,1000&fields=id,date,isAM,isActive,isRealLeave,leaveAccount[id,name],leavePeriod[id,isConfirmed]
```

**C. Ses jours fériés**

```
https://packsolutions.ilucca.net/timmi-absences/api/planning/v1.0/userDates?owner.id=<id>&start=2026-11-01&end=2026-12-31&amOrPmIsOff=true
```

Les jours fériés doivent avoir un `name`. Si l'app affiche « Impossible de lire les jours fériés dans Lucca », noter
le code HTTP de cette adresse.

| Ce qu'on observe                                                    | Cause probable                                                                  |
|---------------------------------------------------------------------|---------------------------------------------------------------------------------|
| Le collègue n'est dans aucune des deux listes A                     | Absent de Lucca : l'app affiche « … est introuvable dans Lucca… »               |
| Il est dans une liste A, mais l'app dit « introuvable »             | Prénom ou nom écrits avec d'autres lettres des deux côtés, ou homonyme : corriger la fiche Redmine |
| Une adresse A renvoie une erreur et l'app dit « Lucca n'a pas laissé lire la liste des collaborateurs » | Noter le code HTTP et l'adresse, et me les envoyer |
| B renvoie 403                                                       | Lucca refuse l'accès : l'app affiche « Lucca ne vous autorise pas… »            |
| B renvoie une liste vide alors qu'il a des absences                 | Droits Lucca filtrés sans erreur : hors de ton planning, l'app le signale par un bandeau |
| B contient ses absences mais l'app ne les montre pas                | Bug de l'app : à me signaler                                                    |

## À me renvoyer en cas de problème

Pas besoin des dates d'absence. Pour chaque cas qui coince :
- la ligne du tableau qui correspond ;
- l'écriture du prénom et du nom côté Redmine et côté Lucca, si la correspondance est en cause.
