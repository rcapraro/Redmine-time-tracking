# RedmineTime | Gestion du temps Redmine

A desktop application for managing time entries in Redmine with a modern user interface built using Compose for Desktop.

Une application de bureau pour gérer les saisies de temps dans Redmine avec une interface utilisateur moderne construite
avec Compose for Desktop.

![RedmineTime Application Screenshot (Light Theme) | Capture d'écran de l'application RedmineTime (Thème clair)](docs/images/redmine-time-screenshot.png)

*The screenshot above shows the application in light theme. The application also supports a dark theme that can be
enabled in the settings.*
*La capture d'écran ci-dessus montre l'application en thème clair. L'application prend également en charge un thème
sombre qui peut être activé dans les paramètres.*

## Features | Fonctionnalités

- Monthly time entry overview | Vue d'ensemble mensuelle des temps
- Add and edit time entries | Ajout et modification des temps
- Project and activity selection | Sélection de projet et d'activité
- Easy navigation between months | Navigation facile entre les mois
- Quick time entry creation and editing | Création et modification rapide des temps
- SSL support (including self-signed certificates with automatic trust and hostname verification disabled) | Support
  SSL (y compris les certificats auto-signés avec confiance automatique et vérification du nom d'hôte désactivée)
- Native look and feel | Apparence native
- Light and dark theme support | Support des thèmes clair et sombre
- Keyboard shortcuts for improved productivity | Raccourcis clavier pour une meilleure productivité
- French language support | Support de la langue française

## Language Support | Support linguistique

The application supports multiple languages with an intelligent fallback system:
L'application prend en charge plusieurs langues avec un système de secours intelligent :

- French (default language) | Français (langue par défaut)
    - Primary language for all users | Langue principale pour tous les utilisateurs
    - Falls back to English if a translation is missing | Utilise l'anglais si une traduction est manquante

- English (fallback language) | Anglais (langue de secours)
    - Alternative language | Langue alternative
    - Falls back to French if a translation is missing | Utilise le français si une traduction est manquante

### Language Configuration | Configuration de la langue

The application uses French by default. You can change the language in the configuration panel:
L'application utilise le français par défaut. Vous pouvez changer la langue dans le panneau de configuration :

1. Click the settings icon in the top bar | Cliquez sur l'icône des paramètres dans la barre supérieure
2. Select your preferred language (French or English) from the dropdown | Sélectionnez votre langue préférée (français
   ou anglais) dans le menu déroulant
3. Click Save | Cliquez sur Enregistrer

The application will reload with the selected language, and all dates will be formatted according to the selected
language.
L'application se rechargera avec la langue sélectionnée, et toutes les dates seront formatées selon la langue
sélectionnée.

Note: The application will automatically handle missing translations by falling back to the alternative language.
Note : L'application gère automatiquement les traductions manquantes en utilisant la langue alternative.

## Prerequisites | Prérequis

- Java Development Kit (JDK) 17 or later | Java Development Kit (JDK) 17 ou supérieur
- Redmine server instance (with API access) | Instance serveur Redmine (avec accès API)

## Configuration | Configuration

The application can be configured in two ways | L'application peut être configurée de deux manières :

### GUI Configuration | Configuration graphique

Click the settings icon in the top bar to open the configuration dialog. You can set:
Cliquez sur l'icône des paramètres dans la barre supérieure pour ouvrir la boîte de dialogue de configuration. Vous
pouvez définir :

- Redmine URL | URL Redmine
- Username | Nom d'utilisateur
- Password | Mot de passe
- Dark Theme | Thème sombre

The configuration is automatically saved and stored securely using Java Preferences API in your system's preferences:
La configuration est automatiquement sauvegardée et stockée de manière sécurisée via l'API Java Preferences dans les
préférences de votre système :

- Windows: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`
- macOS: `~/Library/Preferences/com.ps.redmine.plist` (Key: `/com/ps/redmine`)
- Linux: `~/.java/.userPrefs/com/ps/redmine/prefs.xml`

The configuration values are stored under the node `/com/ps/redmine` in these system-specific locations.
Les valeurs de configuration sont stockées sous le nœud `/com/ps/redmine` dans ces emplacements spécifiques au système.

### Environment Variables | Variables d'environnement

Alternatively, you can use environment variables (they take precedence over saved configuration):
Alternativement, vous pouvez utiliser des variables d'environnement (elles ont la priorité sur la configuration
sauvegardée) :

- `REDMINE_URL`: The URL of your Redmine server | L'URL de votre serveur Redmine (
  default: "https://redmine-restreint.packsolutions.local")
- `REDMINE_USERNAME`: Your Redmine username | Votre nom d'utilisateur Redmine
- `REDMINE_PASSWORD`: Your Redmine password | Votre mot de passe Redmine
- `REDMINE_DARK_THEME`: Set to "true" to enable dark theme | Définir à "true" pour activer le thème sombre (default: "
  false")

Note: Language settings can only be changed through the configuration panel.
Note : Les paramètres de langue ne peuvent être modifiés que via le panneau de configuration.

## Installation | Installation

### From Source | Depuis les sources

1. Clone the repository | Cloner le dépôt
2. Build the application | Construire l'application :
   ```bash
   ./gradlew build
   ```
3. Run the application | Lancer l'application :
   ```bash
   ./gradlew run
   ```

### Native Installers | Installateurs natifs

The application can be packaged as a native installer for different platforms:
L'application peut être empaquetée comme un installateur natif pour différentes plateformes :

- macOS (DMG)
- Windows (MSI and portable ZIP)
- Linux (DEB)

To create native installers | Pour créer les installateurs natifs :

```bash
./gradlew packageReleaseDmg    # For macOS | Pour macOS
./gradlew packageReleaseMsi    # For Windows MSI installer | Pour Windows installateur MSI
./gradlew createReleaseDistributable    # For Windows distributable files | Pour Windows fichiers distribuables
# Then zip the files | Puis compresser les fichiers
# Windows: Compress-Archive -Path build/compose/binaries/main-release/app/* -DestinationPath RedmineTime-portable.zip
# Linux/macOS: zip -r RedmineTime-portable.zip build/compose/binaries/main-release/app/*
./gradlew packageReleaseDeb    # For Linux | Pour Linux
```

### Continuous Integration | Intégration Continue

The project uses GitHub Actions for continuous integration and automated builds. On each push to the main branch or pull
request:

1. The application is built and tested on Windows and macOS
2. Native installers are created automatically:
    - Windows MSI installer
    - Windows portable application (ZIP)
    - macOS DMG

These artifacts are available for download from the GitHub Actions workflow run.

## Usage | Utilisation

1. Initial Setup | Configuration initiale
    - Option 1: Click the settings icon in the top bar to configure your Redmine connection | Cliquer sur l'icône des
      paramètres dans la barre supérieure pour configurer votre connexion Redmine
    - Option 2: Set the environment variables as described in the Configuration section | Définir les variables
      d'environnement comme décrit dans la section Configuration
2. Launch the application | Lancer l'application
3. Navigate to the desired month using the navigation buttons or keyboard shortcuts | Naviguer vers le mois souhaité en
   utilisant les boutons de navigation ou les raccourcis clavier
4. Click the "+" button to add a new time entry | Cliquer sur le bouton "+" pour ajouter une nouvelle saisie de temps
5. Fill in the required information | Remplir les informations requises :
    - Date | Date
    - Hours | Heures
    - Project | Projet
    - Activity | Activité
    - Comments (optional) | Commentaires (optionnel)
6. Save the time entry | Sauvegarder la saisie de temps

Note: You can update your Redmine connection settings at any time by clicking the settings icon in the top bar. The
application will restart to apply the new configuration. |
Note : Vous pouvez mettre à jour vos paramètres de connexion Redmine à tout moment en cliquant sur l'icône des
paramètres dans la barre supérieure. L'application redémarrera pour appliquer la nouvelle configuration.

## Keyboard Shortcuts | Raccourcis clavier

- `Ctrl/Cmd + S`: Save current time entry | Sauvegarder la saisie de temps
- `Escape`: Cancel current operation | Annuler l'opération en cours
- `Alt + Left Arrow`: Navigate to previous month | Naviguer vers le mois précédent
- `Alt + Right Arrow`: Navigate to next month | Naviguer vers le mois suivant
- `Alt + T`: Jump to current month | Aller au mois courant

## Technical Details | Détails techniques

Built with | Construit avec :

- Kotlin 1.9.21
- Compose for Desktop 1.5.11
- Redmine Java API 3.1.3
- Kotlin Coroutines 1.7.3 | Coroutines Kotlin 1.7.3
- Kotlinx DateTime 0.5.0 | DateTime Kotlinx 0.5.0
- Koin 3.5.0 (Dependency Injection)
- Apache HttpClient 4.5.14

## Credits | Crédits

- Application icon created by Fabrice Perez | Icône de l'application créée par Fabrice Perez
    - LinkedIn: [https://www.linkedin.com/in/perezfabrice/](https://www.linkedin.com/in/perezfabrice/)
