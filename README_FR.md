# RedmineTime | Gestion du temps Redmine

Une application de bureau pour gérer les saisies de temps dans Redmine avec une interface utilisateur moderne construite
avec Compose for Desktop.

![Capture d'écran de l'application RedmineTime (Thème clair)](docs/images/redmine-time-screenshot_fr.png)

*La capture d'écran ci-dessus montre l'application en thème clair. L'application prend également en charge un thème
sombre qui peut être activé dans les paramètres.*

## Fonctionnalités

- **Vue d'ensemble mensuelle des saisies** avec navigation intuitive
- **Barres de progression hebdomadaires et mensuelle** avec pourcentage de completion et infobulles par semaine ISO
- **Temps de travail configurable** — heures par jour (6 / 6,5 / 7 / 7,5 h) et jusqu'à 4 jours non travaillés en semaine
- **Ajout, modification et suppression de saisies** avec validation complète
- **Heures exactes** — saisie à deux décimales (3,75), totaux sans erreur d'arrondi, et avertissement quand Redmine
  plafonnera la journée
- **Import des absences Lucca** — congés et jours fériés reportés dans Redmine en un clic, y compris pour un collègue en
  impersonation
- **Duplication de saisies** sur le même jour, sur le jour ouvré suivant, ou sur une plage de dates
- **Modification et suppression groupées** — sélectionnez plusieurs saisies pour modifier projet, activité, demande,
  heures ou commentaire en une seule opération
- **Sélecteurs de projet, ticket et activité** avec menus déroulants recherchables
- **Navigation facile entre les mois** avec boutons fléchés, raccourci « Aujourd'hui » et raccourcis clavier
- **Aide intégrée** (bouton Aide dans le bandeau supérieur) résumant les actions, icônes et raccourcis
- **Mises à jour intégrées** — un bouton « Téléchargement » apparaît dans le bandeau supérieur lorsqu'une nouvelle
  version stable est disponible
- **Impersonation** — les administrateurs peuvent saisir le temps pour le compte d'un autre utilisateur Redmine depuis
  le bandeau supérieur
- **Bandeau de statut en temps réel** affichant la date du jour, l'heure courante et le numéro de la semaine ISO
- **Support SSL** (y compris les certificats auto-signés avec confiance automatique et vérification du nom d'hôte
  désactivée)
- **Apparence native** qui s'intègre avec votre système d'exploitation
- **Thèmes clair et sombre**
- **Raccourcis clavier** pour une productivité améliorée
- **Support multilingue** (français et anglais) avec système de secours intelligent
- **Gestion d'erreurs robuste** avec messages d'erreur conviviaux
- **Validation en temps réel** pour tous les champs de formulaire

## Support linguistique

L'application prend en charge plusieurs langues avec un système de secours intelligent :

- Français (langue par défaut)
    - Langue principale pour tous les utilisateurs
    - Utilise l'anglais si une traduction est manquante

- Anglais (langue de secours)
    - Langue alternative
    - Utilise le français si une traduction est manquante

### Configuration de la langue

L'application utilise le français par défaut. Vous pouvez changer la langue dans le panneau de configuration :

1. Cliquez sur le bouton **Paramètres** (icône d'engrenage) dans le bandeau supérieur
2. Sélectionnez votre langue préférée (français ou anglais) dans le menu déroulant
3. Cliquez sur Enregistrer

L'application se rechargera avec la langue sélectionnée, et toutes les dates seront formatées selon la langue
sélectionnée.

Note : L'application gère automatiquement les traductions manquantes en utilisant la langue alternative.

## Prérequis

- JetBrains Runtime 21 avec JCEF (version épinglée dans `mise.toml` et le workflow CI), requis pour compiler le navigateur de connexion Lucca intégré
- Instance serveur Redmine (avec accès API activé)

## Configuration

L'application peut être configurée de deux manières :

### Configuration graphique

Cliquez sur le bouton **Paramètres** (icône d'engrenage) dans le bandeau supérieur pour ouvrir la boîte de dialogue de
configuration. Vous pouvez définir :

- **URL Redmine**
- **Clé d'API** — la clé d'API Redmine (un bouton afficher/masquer et un lien « Comment obtenir votre clé d'API ? » sont
  disponibles)
- **Thème sombre** — bascule entre les thèmes clair et sombre Material 3
- **Langue** — français ou anglais
- **Heures par jour** — 6, 6,5, 7 ou 7,5 (sert de cible quotidienne, hebdomadaire et mensuelle)
- **Jours non travaillés** — jusqu'à 4 jours de la semaine (lundi–vendredi) qui ne comptent pas comme jours ouvrés

La configuration est automatiquement sauvegardée et stockée de manière sécurisée via l'API Java Preferences dans les
préférences de votre système :

- Windows: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`
- macOS: `~/Library/Preferences/com.ps.redmine.plist` (Key: `/com/ps/redmine`)
- Linux: `~/.java/.userPrefs/com/ps/redmine/prefs.xml`

Les valeurs de configuration sont stockées sous le nœud `/com/ps/redmine` dans ces emplacements spécifiques au système.
L'association des types d'absence Lucca aux tickets Redmine y est aussi enregistrée (clé `lucca.absenceTargets`). La
session Lucca, elle, est conservée dans le trousseau du système (Trousseau macOS, Gestionnaire d'identification
Windows).

### Variables d'environnement

Alternativement, vous pouvez utiliser des variables d'environnement (elles ont la priorité sur la configuration
sauvegardée) :

- `REDMINE_URL`: L'URL de votre serveur Redmine
- `REDMINE_API_KEY`: Votre clé d'API Redmine
- `REDMINE_DARK_THEME`: Définir à "true" pour activer le thème sombre, sinon le thème clair est utilisé. Défaut : "
  false".

Note : Les paramètres de langue ne peuvent être modifiés que via le panneau de configuration.

## Installation

### Depuis les sources

1. Cloner le dépôt
2. Construire l'application :
   ```bash
   ./gradlew build
   ```
3. Lancer l'application :
   ```bash
   ./gradlew run
   ```

### Lancer depuis IntelliJ IDEA

La fenêtre de connexion Lucca embarque Chromium via JCEF, qui n'est fourni qu'avec JetBrains Runtime 21 avec JCEF. La
compilation comme l'exécution en ont besoin : avec un autre JDK, le build échoue sur les imports `org.cef` ou l'application
indique que le navigateur intégré n'est pas disponible.

1. Installer le JDK déclaré dans `mise.toml` :
   ```bash
   mise install
   ```
2. Faire pointer la JVM Gradle d'IntelliJ dessus, sans déclarer de SDK global :
   ```bash
   echo "java.home=$(mise where java)" > .gradle/config.properties
   ```
   Puis choisir `GRADLE_LOCAL_JAVA_HOME` dans *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle
   JVM* et recharger le projet Gradle. Relancer la commande après une mise à jour du JDK avec mise.
3. Lancer ou déboguer la configuration partagée **RedmineTime** (`.run/RedmineTime.run.xml`, tâche Gradle `run`).

L'icône ▶ dans la gouttière à côté de `main()` utilise le SDK du projet. Pour l'utiliser, et pour que l'éditeur résolve
`org.cef`, régler *Project Structure → SDK* sur le même JDK (*Add JDK from disk…* → le chemin donné par `mise where java`).

### Installateurs natifs

L'application peut être empaquetée comme un installateur natif pour différentes plateformes :

- macOS (DMG)
- Windows (MSI and portable ZIP)
- Linux (DEB) : la fonctionnalité des absences Lucca n'y est pas encore prise en charge. Le build n'embarque pas les
  fichiers Chromium dont le navigateur de connexion intégré a besoin sous Linux : la connexion à Lucca indique que le
  navigateur intégré n'est pas disponible.

Pour créer les installateurs natifs :

```bash
./gradlew packageReleaseDmg    # Pour macOS
./gradlew packageReleaseMsi    # Pour Windows installateur MSI
./gradlew createReleaseDistributable    # Pour Windows fichiers distribuables
# Puis compresser les fichiers
# Windows: Compress-Archive -Path build/compose/binaries/main-release/app/* -DestinationPath RedmineTime-portable.zip
# Linux/macOS: zip -r RedmineTime-portable.zip build/compose/binaries/main-release/app/*
./gradlew packageReleaseDeb    # Pour Linux
```

### Intégration Continue

Le projet utilise GitHub Actions pour l'intégration continue et les builds automatisés. À chaque push sur la branche
principale ou pull request :

1. L'application est construite et testée sur Windows et macOS
2. Des installateurs natifs sont créés automatiquement :
    - Installateur Windows MSI
    - Application portable Windows (ZIP)
    - macOS DMG

Ces artefacts sont disponibles au téléchargement depuis l'exécution du workflow GitHub Actions.

### Versions

Lorsqu'une nouvelle version est prête pour la publication :

1. Créez et poussez un tag avec le numéro de version préfixé par 'v' (par exemple, `v1.0.0`, `v2.1.3`)
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub Actions va automatiquement :
    - Construire l'application pour toutes les plateformes supportées
    - Créer une nouvelle version GitHub avec le nom du tag
    - Joindre tous les installateurs construits à la version

Un tag contenant un tiret (par exemple `v2.4.0-beta`) est publié comme pré-version : la mise à jour intégrée ne la
propose pas.

Les installateurs publiés seront disponibles sur la page des versions GitHub.

## Utilisation

### Configuration initiale

1. **Configurer la connexion Redmine** :
    - Option 1 : Cliquez sur le bouton **Paramètres** (icône d'engrenage) dans le bandeau supérieur pour configurer
      votre connexion Redmine
    - Option 2 : Définir les variables d'environnement comme décrit dans la section Configuration
2. **Lancer l'application**

### Bandeau supérieur

Le bandeau supérieur regroupe les informations de statut et les actions globales :

- **Pastille utilisateur** — affiche votre compte ; les administrateurs peuvent cliquer dessus pour saisir le temps pour
  un autre utilisateur Redmine (un badge coloré rappelle alors pour qui vous saisissez). Choisissez « Moi-même » pour
  revenir sur votre compte. L'impersonation n'est jamais persistée.
- **Pastilles d'information** — date du jour, heure courante et numéro de la semaine ISO.
- **Mise à jour** (icône de téléchargement, avec un point rouge) — n'apparaît que lorsqu'une nouvelle version stable est
  disponible ; ouvre la boîte de dialogue de mise à jour avec les notes de version et un téléchargement direct de
  l'installateur correspondant à votre OS.
- **Absences** (icône de parasol) — ouvre la fenêtre d'import des absences Lucca (voir plus bas).
- **Paramètres** (icône d'engrenage) — ouvre la boîte de dialogue de configuration.
- **Aide** (icône point d'interrogation) — ouvre un guide intégré listant chaque action, icône et raccourci.

### Gestion des saisies de temps

1. **Naviguer entre les mois** avec les flèches au-dessus de la liste, le raccourci « Aujourd'hui (Alt+T) » ou les
   raccourcis clavier (Alt+← / Alt+→).
2. **Voir la progression mensuelle et hebdomadaire** : une colonne de barres de progression hebdomadaires se trouve à
   l'extrême gauche, à côté du total mensuel. L'application affiche :
    - Total des heures saisies pour le mois
    - Progression mensuelle avec pourcentage de completion et célébration lorsque le mois est terminé
    - Jours ouvrables du mois courant (hors week-ends et hors jours non travaillés configurés)
    - Heures attendues (jours ouvrables × heures par jour configurées)
    - Heures restantes pour compléter le mois
    - Barres de progression par semaine, avec infobulle indiquant la plage de dates et heures / objectif. Cliquez sur
      une barre hebdomadaire pour vous positionner sur le premier jour ouvré de cette semaine.
3. **Ajouter une nouvelle saisie de temps** : le panneau de droite est le formulaire de création/édition. Désélectionnez
   la saisie courante (ou changez de jour) pour afficher le formulaire de création, puis remplissez :
    - **Date** — choisir une date avec l'icône calendrier, ou utiliser les boutons `-1 / +1 jour` (jours ouvrés)
    - **Heures** — nombre d'heures travaillées, avec deux décimales au plus (`3.75` ou `3,75`). Le bouton « Journée
      complète » le remplit avec vos heures du jour. Redmine plafonne chaque journée à ces heures : si la saisie les
      dépasse, le formulaire indique combien d'heures seront réellement gardées.
    - **Projet** — menu déroulant recherchable
    - **Ticket** — menu déroulant recherchable filtré par le projet sélectionné
    - **Activité** — menu déroulant recherchable
    - **Commentaires** — obligatoires, 255 caractères max
4. **Modifier une saisie existante** : cliquez sur n'importe quelle saisie de la liste pour la charger dans le
   formulaire de droite.
5. **Dupliquer une saisie** : chaque ligne possède une icône de copie qui ouvre un menu — dupliquer pour le même jour,
   pour le jour ouvré suivant, ou sur une plage de dates (seuls les jours ouvrés de la plage sont dupliqués).
6. **Supprimer une saisie** : cliquez sur l'icône poubelle rouge de la ligne ; une boîte de confirmation s'affiche.
7. **Actions groupées** : cochez la case de plusieurs saisies pour faire apparaître la barre d'actions groupées
   au-dessus de la liste :
    - **Modifier** — ouvre la boîte de dialogue de modification groupée et permet de choisir quels champs (projet,
      activité, ticket, heures, commentaire) appliquer à toutes les saisies sélectionnées.
    - **Supprimer** — supprime toutes les saisies sélectionnées après confirmation.
    - **Fermer** (X) — vide la sélection courante.
8. **Sauvegarder les modifications** : utilisez Ctrl/Cmd+S ou cliquez sur le bouton Enregistrer. Le formulaire passe
   ensuite au premier jour incomplet du mois, en gardant ticket, activité et commentaire pour enchaîner.
9. **Annuler les modifications** : appuyez sur Échap ou cliquez sur Annuler.

### Absences Lucca

Le bouton **Absences** du bandeau supérieur reporte dans Redmine les absences posées dans Lucca, pour le mois affiché
et le suivant.

1. **Se connecter à Lucca** : à la première ouverture, une fenêtre de connexion Lucca s'ouvre dans l'application (SSO
   compris). La session est conservée dans le trousseau du système ; « Se déconnecter » l'efface.
2. **Associer les types d'absence** : chaque type (congés payés, RTT, jour férié…) est associé une fois pour toutes à un
   ticket et à une activité Redmine. Les jours fériés Lucca qui tombent en semaine apparaissent sous le type « Jour
   férié ». Le télétravail n'est pas compté comme une absence.
3. **Choisir les absences à reporter** : chaque jour d'absence reçoit un statut (à ajouter, déjà saisie, heures
   différentes, dépassement, annulée…). Une demi-journée compte 3,75 h. Seules les absences validées dans Lucca et
   absentes de Redmine sont cochées d'office.
4. **Enregistrer dans Redmine** : rien de ce qui existe déjà n'est modifié sans votre accord. Une absence déjà saisie
   avec d'autres heures, ou annulée depuis dans Lucca, n'est jamais cochée d'office. Avant l'enregistrement, une
   confirmation récapitule les heures remplacées, les saisies supprimées et les journées que Redmine plafonnera.

En impersonation, la fenêtre affiche les absences Lucca du collègue, lues avec votre propre session Lucca, et les
importe dans son Redmine. Le collègue est retrouvé dans Lucca par son prénom et son nom, quels que soient la casse, les
accents, les tirets et les apostrophes ; en cas d'homonyme, personne n'est choisi. Si Lucca ne vous autorise pas à lire
ses absences, la fenêtre l'indique. Le document [docs/lucca-impersonation-test.md](docs/lucca-impersonation-test.md)
détaille comment le vérifier et le diagnostiquer.

L'instance Lucca est fixée dans `LuccaClient.kt` (`LUCCA_BASE_URL`). Sous Linux, la connexion à Lucca n'est pas encore
prise en charge (voir Installateurs natifs).

### Suivi de progression mensuelle

L'application calcule et affiche automatiquement :

- **Jours ouvrables** dans le mois courant (lundi–vendredi moins les jours non travaillés configurés)
- **Heures attendues** (jours ouvrables × heures par jour configurées)
- **Pourcentage de completion** avec indicateur de progression visuel
- **Détail par semaine** pour repérer en un clin d'œil la semaine où il manque des heures
- **Statut codé par couleur** : vert lorsque le jour ou le mois est complet, ambre/rouge lorsqu'il manque ou qu'il y a
  trop d'heures
- **Heures restantes** pour atteindre l'objectif mensuel

Note : Vous pouvez mettre à jour vos paramètres de connexion Redmine à tout moment depuis le bouton **Paramètres** dans
le bandeau supérieur. L'application prend en compte les nouveaux identifiants et recharge les données sans
redémarrer.

## Raccourcis clavier

- `Ctrl/Cmd + S`: Sauvegarder la saisie de temps
- `Escape`: Annuler l'opération en cours
- `Alt + Left Arrow`: Naviguer vers le mois précédent
- `Alt + Right Arrow`: Naviguer vers le mois suivant
- `Alt + T`: Aller au mois courant

## Détails techniques

Construit avec :

- **Kotlin** 2.3.21
- **Compose for Desktop** 1.10.3 (Material 3 1.9.0)
- **Ktor Client** 3.4.3 (client HTTP pour les API Redmine et Lucca)
- **Kotlin Coroutines** 1.11.0
- **Kotlinx DateTime** 0.8.0
- **Kotlinx Serialization** 1.11.0
- **Koin** 4.2.1 (Injection de dépendances)
- **JCEF** (fourni par JetBrains Runtime 21) pour la fenêtre de connexion Lucca
- **java-keyring** 1.0.4 (session Lucca dans le trousseau du système)
- **SLF4J** 2.0.17 (Journalisation)
- **JUnit** 6.0.3 + **MockK** 1.14.9 (Tests)

### Architecture

L'application suit les pratiques modernes de développement Kotlin :

- **Interface utilisateur Compose** : Framework d'interface utilisateur déclarative pour applications de bureau
- **Coroutines** : Programmation asynchrone pour des opérations non-bloquantes
- **Injection de dépendances** : Architecture propre avec Koin
- **Client HTTP** : Ktor pour une communication API robuste avec support SSL
- **Sérialisation** : Kotlinx Serialization pour l'analyse JSON
- **Date/Heure** : Kotlinx DateTime pour la gestion des dates multi-plateforme

## Crédits

- Icône de l'application créée par Fabrice Perez
    - LinkedIn: [https://www.linkedin.com/in/perezfabrice/](https://www.linkedin.com/in/perezfabrice/)
