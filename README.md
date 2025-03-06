# RedmineTime | Gestion du temps Redmine

A desktop application for managing time entries in Redmine with a modern user interface built using Compose for Desktop.

Une application de bureau pour gérer les saisies de temps dans Redmine avec une interface utilisateur moderne construite
avec Compose for Desktop.

![RedmineTime Application Screenshot | Capture d'écran de l'application RedmineTime](docs/images/redmine-time-screenshot.png)

## Features | Fonctionnalités

- Monthly time entry overview | Vue d'ensemble mensuelle des temps
- Add and edit time entries | Ajout et modification des temps
- Project and activity selection | Sélection de projet et d'activité
- Easy navigation between months | Navigation facile entre les mois
- Quick time entry creation and editing | Création et modification rapide des temps
- SSL support (including self-signed certificates with automatic trust and hostname verification disabled) | Support
  SSL (y compris les certificats auto-signés avec confiance automatique et vérification du nom d'hôte désactivée)
- Native look and feel | Apparence native
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

The application uses French by default. To change the language, set the REDMINE_LANG environment variable:
L'application utilise le français par défaut. Pour changer la langue, définissez la variable d'environnement
REDMINE_LANG :

```bash
# For English | Pour l'anglais
export REDMINE_LANG=en

# For French (default) | Pour le français (par défaut)
export REDMINE_LANG=fr    # Or leave unset | Ou laisser non définie
```

Note: The application will automatically handle missing translations by falling back to the alternative language.
Note : L'application gère automatiquement les traductions manquantes en utilisant la langue alternative.

## Prerequisites | Prérequis

- Java Development Kit (JDK) 17 or later | Java Development Kit (JDK) 17 ou supérieur
- Redmine server instance (with API access) | Instance serveur Redmine (avec accès API)

## Configuration | Configuration

The application requires the following environment variables to be set:
L'application nécessite la configuration des variables d'environnement suivantes :

- `REDMINE_URL`: The URL of your Redmine server | L'URL de votre serveur Redmine (
  default: "https://redmine-restreint.packsolutions.local")
- `REDMINE_USERNAME`: Your Redmine username | Votre nom d'utilisateur Redmine
- `REDMINE_PASSWORD`: Your Redmine password | Votre mot de passe Redmine

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
- Windows (MSI)
- Linux (DEB)

To create native installers | Pour créer les installateurs natifs :

```bash
./gradlew packageReleaseDmg    # For macOS | Pour macOS
./gradlew packageReleaseMsi    # For Windows | Pour Windows
./gradlew packageReleaseDeb    # For Linux | Pour Linux
```

## Usage | Utilisation

1. Set the required environment variables | Définir les variables d'environnement requises
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
