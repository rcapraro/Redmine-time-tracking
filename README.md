# RedmineTime | Gestion du temps Redmine

A desktop application for managing time entries in Redmine with a modern user interface built using Compose for Desktop.

Une application de bureau pour gérer les saisies de temps dans Redmine avec une interface utilisateur moderne construite avec Compose for Desktop.

## Features | Fonctionnalités

- Monthly time entry overview | Vue d'ensemble mensuelle des temps
- Add and edit time entries | Ajout et modification des temps
- Project and activity selection | Sélection de projet et d'activité
- Easy navigation between months | Navigation facile entre les mois
- Quick time entry creation and editing | Création et modification rapide des temps
- SSL support (including self-signed certificates) | Support SSL (y compris les certificats auto-signés)
- Native look and feel | Apparence native
- Keyboard shortcuts for improved productivity | Raccourcis clavier pour une meilleure productivité
- French language support | Support de la langue française

## Language Support | Support linguistique

The application is available in the following languages:
L'application est disponible dans les langues suivantes :
- French (default) | Français (par défaut)
- English (fallback) | Anglais (secours)

### Language Configuration | Configuration de la langue

The application uses French by default. To change the language to English, set the following environment variable:
L'application utilise le français par défaut. Pour changer la langue en anglais, définissez la variable d'environnement suivante :

```bash
export REDMINE_LANG=en    # For English | Pour l'anglais
export REDMINE_LANG=fr    # For French | Pour le français (par défaut)
```

## Prerequisites | Prérequis

- Java Development Kit (JDK) 17 or later | Java Development Kit (JDK) 17 ou supérieur
- Redmine server instance (with API access) | Instance serveur Redmine (avec accès API)

## Configuration | Configuration

The application requires the following environment variables to be set:
L'application nécessite la configuration des variables d'environnement suivantes :

- `REDMINE_URL`: The URL of your Redmine server | L'URL de votre serveur Redmine (ex: "https://redmine.example.com")
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
3. Navigate to the desired month using the navigation buttons or keyboard shortcuts | Naviguer vers le mois souhaité en utilisant les boutons de navigation ou les raccourcis clavier
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
- Kotlin 1.8.20
- Compose for Desktop 1.7.3
- Redmine Java API 3.1.3
- Kotlin Coroutines | Coroutines Kotlin
- Kotlinx DateTime | DateTime Kotlinx
