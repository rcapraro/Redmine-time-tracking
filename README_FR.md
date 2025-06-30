# RedmineTime | Gestion du temps Redmine

Une application de bureau pour gérer les saisies de temps dans Redmine avec une interface utilisateur moderne construite
avec Compose for Desktop.

![Capture d'écran de l'application RedmineTime (Thème clair)](docs/images/redmine-time-screenshot_fr.png)

*La capture d'écran ci-dessus montre l'application en thème clair. L'application prend également en charge un thème
sombre qui peut être activé dans les paramètres.*

## Fonctionnalités

- **Vue d'ensemble mensuelle des temps** avec navigation intuitive
- **Suivi de progression mensuelle** avec indicateurs visuels montrant le pourcentage de completion
- **Calcul des jours ouvrables** et suivi des heures attendues (jours ouvrables × 7,5 heures)
- **Ajout et modification des temps** avec validation complète
- **Sélection de projet et d'activité** avec menus déroulants recherchables
- **Intégration des tickets** - sélection des tickets associés aux projets choisis
- **Navigation facile entre les mois** avec raccourcis clavier
- **Création et modification rapide des temps** avec valeurs par défaut intelligentes
- **Support SSL** (y compris les certificats auto-signés avec confiance automatique et vérification du nom d'hôte
  désactivée)
- **Apparence native** qui s'intègre avec votre système d'exploitation
- **Support des thèmes clair et sombre** avec intégration système
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

1. Cliquez sur l'icône des paramètres dans la barre supérieure
2. Sélectionnez votre langue préférée (français ou anglais) dans le menu déroulant
3. Cliquez sur Enregistrer

L'application se rechargera avec la langue sélectionnée, et toutes les dates seront formatées selon la langue
sélectionnée.

Note : L'application gère automatiquement les traductions manquantes en utilisant la langue alternative.

## Prérequis

- Java Development Kit (JDK) 21 ou supérieur
- Instance serveur Redmine (avec accès API activé)

## Configuration

L'application peut être configurée de deux manières :

### Configuration graphique

Cliquez sur l'icône des paramètres dans la barre supérieure pour ouvrir la boîte de dialogue de configuration. Vous
pouvez définir :

- URL Redmine
- Clé d'API (la clé d'API Redmine)
- Thème sombre

La configuration est automatiquement sauvegardée et stockée de manière sécurisée via l'API Java Preferences dans les
préférences de votre système :

- Windows: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`
- macOS: `~/Library/Preferences/com.ps.redmine.plist` (Key: `/com/ps/redmine`)
- Linux: `~/.java/.userPrefs/com/ps/redmine/prefs.xml`

Les valeurs de configuration sont stockées sous le nœud `/com/ps/redmine` dans ces emplacements spécifiques au système.

### Variables d'environnement

Alternativement, vous pouvez utiliser des variables d'environnement (elles ont la priorité sur la configuration
sauvegardée) :

- `REDMINE_URL`: L'URL de votre serveur Redmine
- `REDMINE_API_KEY`: Votre clé d'API Redmine
- `REDMINE_DARK_THEME`: Définir à "true" pour activer le thème sombre (défaut: "false")

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

### Installateurs natifs

L'application peut être empaquetée comme un installateur natif pour différentes plateformes :

- macOS (DMG)
- Windows (MSI and portable ZIP)
- Linux (DEB)

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

Les installateurs publiés seront disponibles sur la page des versions GitHub.

## Utilisation

### Configuration initiale

1. **Configurer la connexion Redmine** :
    - Option 1 : Cliquez sur l'icône des paramètres dans la barre supérieure pour configurer votre connexion Redmine
    - Option 2 : Définir les variables d'environnement comme décrit dans la section Configuration
2. **Lancer l'application**

### Gestion des saisies de temps

1. **Naviguer entre les mois** en utilisant les boutons de navigation ou les raccourcis clavier (Alt+← / Alt+→)
2. **Voir la progression mensuelle** : L'application affiche :
    - Total des heures saisies pour le mois
    - Indicateur de progression mensuelle montrant le pourcentage de completion
    - Calcul des jours ouvrables et heures attendues (jours ouvrables × 7,5)
    - Heures restantes nécessaires pour compléter le mois
3. **Ajouter une nouvelle saisie de temps** :
    - Cliquez sur le bouton "+" ou sélectionnez "Nouvelle saisie"
    - Remplir les informations requises :
        - **Date** : Sélectionner la date pour la saisie de temps
        - **Heures** : Entrer le nombre d'heures travaillées
        - **Projet** : Choisir parmi les projets disponibles (menu déroulant recherchable)
        - **Ticket** : Sélectionner un ticket du projet choisi
        - **Activité** : Sélectionner le type d'activité effectuée
        - **Commentaires** : Ajouter des commentaires descriptifs (obligatoire)
4. **Modifier les saisies existantes** : Cliquez sur n'importe quelle saisie de temps dans la liste pour la modifier
5. **Sauvegarder les modifications** : Utilisez Ctrl/Cmd+S ou cliquez sur le bouton Sauvegarder
6. **Annuler les modifications** : Appuyez sur Échap ou cliquez sur Annuler

### Suivi de progression mensuelle

L'application calcule et affiche automatiquement :

- **Jours ouvrables** dans le mois actuel (excluant les week-ends)
- **Heures attendues** (jours ouvrables × 7,5 heures)
- **Pourcentage de completion** avec indicateur de progression visuel
- **Statut codé par couleur** : Vert quand le mois est complété
- **Heures restantes** nécessaires pour atteindre l'objectif mensuel

Note : Vous pouvez mettre à jour vos paramètres de connexion Redmine à tout moment en cliquant sur l'icône des
paramètres dans la barre supérieure. L'application redémarrera pour appliquer la nouvelle configuration.

## Raccourcis clavier

- `Ctrl/Cmd + S`: Sauvegarder la saisie de temps
- `Escape`: Annuler l'opération en cours
- `Alt + Left Arrow`: Naviguer vers le mois précédent
- `Alt + Right Arrow`: Naviguer vers le mois suivant
- `Alt + T`: Aller au mois courant

## Détails techniques

Construit avec :

- **Kotlin** 2.2.0
- **Compose for Desktop** 1.8.2
- **Ktor Client** 3.2.0 (client HTTP pour la communication API)
- **Kotlin Coroutines** 1.10.2
- **Kotlinx DateTime** 0.6.1
- **Kotlinx Serialization** 1.9.0
- **Koin** 4.1.0 (Injection de dépendances)
- **SLF4J** 2.0.16 + **Logback** 1.5.12 (Journalisation)
- **JUnit 5** 5.13.2 (Framework de test)

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
