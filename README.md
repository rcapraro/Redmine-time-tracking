# RedmineTime

A desktop application for managing time entries in Redmine with a modern user interface built using Compose for Desktop.

[Version française (French version)](README_FR.md)

![RedmineTime Application Screenshot (Light Theme)](docs/images/redmine-time-screenshot.png)

*The screenshot above shows the application in light theme. The application also supports a dark theme that can be
enabled in the settings.*

## Features

- Monthly time entry overview
- Add and edit time entries
- Project and activity selection
- Easy navigation between months
- Quick time entry creation and editing
- SSL support (including self-signed certificates with automatic trust and hostname verification disabled)
- Native look and feel
- Light and dark theme support
- Keyboard shortcuts for improved productivity
- French language support

## Language Support

The application supports multiple languages with an intelligent fallback system:

- French (default language)
    - Primary language for all users
    - Falls back to English if a translation is missing

- English (fallback language)
    - Alternative language
    - Falls back to French if a translation is missing

### Language Configuration

The application uses French by default. You can change the language in the configuration panel:

1. Click the settings icon in the top bar
2. Select your preferred language (French or English) from the dropdown
3. Click Save

The application will reload with the selected language, and all dates will be formatted according to the selected
language.

Note: The application will automatically handle missing translations by falling back to the alternative language.

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Redmine server instance (with API access)

## Configuration

The application can be configured in two ways:

### GUI Configuration

Click the settings icon in the top bar to open the configuration dialog. You can set:

- Redmine URL
- Username
- Password
- Dark Theme

The configuration is automatically saved and stored securely using Java Preferences API in your system's preferences:

- Windows: Registry under `HKEY_CURRENT_USER\Software\JavaSoft\Prefs`
- macOS: `~/Library/Preferences/com.ps.redmine.plist` (Key: `/com/ps/redmine`)
- Linux: `~/.java/.userPrefs/com/ps/redmine/prefs.xml`

The configuration values are stored under the node `/com/ps/redmine` in these system-specific locations.

### Environment Variables

Alternatively, you can use environment variables (they take precedence over saved configuration):

- `REDMINE_URL`: The URL of your Redmine server (default: "https://redmine-restreint.packsolutions.local")
- `REDMINE_USERNAME`: Your Redmine username
- `REDMINE_PASSWORD`: Your Redmine password
- `REDMINE_DARK_THEME`: Set to "true" to enable dark theme (default: "false")

Note: Language settings can only be changed through the configuration panel.

## Installation

### From Source

1. Clone the repository
2. Build the application:
   ```bash
   ./gradlew build
   ```
3. Run the application:
   ```bash
   ./gradlew run
   ```

### Native Installers

The application can be packaged as a native installer for different platforms:

- macOS (DMG)
- Windows (MSI and portable ZIP)
- Linux (DEB)

To create native installers:

```bash
./gradlew packageReleaseDmg    # For macOS
./gradlew packageReleaseMsi    # For Windows MSI installer
./gradlew createReleaseDistributable    # For Windows distributable files
# Then zip the files
# Windows: Compress-Archive -Path build/compose/binaries/main-release/app/* -DestinationPath RedmineTime-portable.zip
# Linux/macOS: zip -r RedmineTime-portable.zip build/compose/binaries/main-release/app/*
./gradlew packageReleaseDeb    # For Linux
```

### Continuous Integration

The project uses GitHub Actions for continuous integration and automated builds. On each push to the main branch or pull
request:

1. The application is built and tested on Windows and macOS
2. Native installers are created automatically:
    - Windows MSI installer
    - Windows portable application (ZIP)
    - macOS DMG

These artifacts are available for download from the GitHub Actions workflow run.

### Releases

When a new version is ready for release:

1. Create and push a tag with the version number prefixed with 'v' (e.g., `v1.0.0`, `v2.1.3`)
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub Actions will automatically:
    - Build the application for all supported platforms
    - Create a new GitHub release with the tag name
    - Attach all built installers to the release

The released installers will be available on the GitHub Releases page.

## Usage

1. Initial Setup
    - Option 1: Click the settings icon in the top bar to configure your Redmine connection
    - Option 2: Set the environment variables as described in the Configuration section
2. Launch the application
3. Navigate to the desired month using the navigation buttons or keyboard shortcuts
4. Click the "+" button to add a new time entry
5. Fill in the required information:
    - Date
    - Hours
    - Project
    - Activity
    - Comments (optional)
6. Save the time entry

Note: You can update your Redmine connection settings at any time by clicking the settings icon in the top bar. The
application will restart to apply the new configuration.

## Keyboard Shortcuts

- `Ctrl/Cmd + S`: Save current time entry
- `Escape`: Cancel current operation
- `Alt + Left Arrow`: Navigate to previous month
- `Alt + Right Arrow`: Navigate to next month
- `Alt + T`: Jump to current month

## Technical Details

Built with:

- Kotlin 2.1.10
- Compose for Desktop 1.7.3
- Redmine Java API 3.1.3
- Kotlin Coroutines 1.10.1
- Kotlinx DateTime 0.6.2
- Koin 4.0.2 (Dependency Injection)
- Apache HttpClient 4.5.14

## Credits

- Application icon created by Fabrice Perez
    - LinkedIn: [https://www.linkedin.com/in/perezfabrice/](https://www.linkedin.com/in/perezfabrice/)
