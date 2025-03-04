# RedmineTime

A desktop application for managing time entries in Redmine with a modern user interface built using Compose for Desktop.

## Features

- Monthly time entry overview
- Add and edit time entries
- Project and activity selection
- Easy navigation between months
- Quick time entry creation and editing
- SSL support (including self-signed certificates)
- Native look and feel
- Keyboard shortcuts for improved productivity

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Redmine server instance (with API access)

## Configuration

The application requires the following environment variables to be set:

- `REDMINE_URL`: The URL of your Redmine server (e.g., "https://redmine.example.com")
- `REDMINE_USERNAME`: Your Redmine username
- `REDMINE_PASSWORD`: Your Redmine password
- `REDMINE_PROJECT_ID`: (Optional) The default project identifier to use for new time entries (defaults to "pml-pac-4025")

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
- Windows (MSI)
- Linux (DEB)

To create native installers:
```bash
./gradlew packageReleaseDmg    # For macOS
./gradlew packageReleaseMsi    # For Windows
./gradlew packageReleaseDeb    # For Linux
```

## Usage

1. Set the required environment variables
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

## Keyboard Shortcuts

- `Ctrl/Cmd + S`: Save current time entry
- `Escape`: Cancel current operation
- `Alt + Left Arrow`: Navigate to previous month
- `Alt + Right Arrow`: Navigate to next month
- `Alt + T`: Jump to current month

## Technical Details

Built with:
- Kotlin 1.8.20
- Compose for Desktop 1.7.3
- Redmine Java API 3.1.3
- Kotlin Coroutines
- Kotlinx DateTime
