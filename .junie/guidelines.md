# RedmineTime Project Guidelines

## Project Overview

RedmineTime is a desktop application designed to simplify time entry management in Redmine. Built with modern UI
technologies, it provides a user-friendly interface for tracking and managing time spent on various projects and tasks.

### Purpose

The primary purpose of RedmineTime is to offer a more intuitive and efficient alternative to Redmine's native time entry
interface. It allows users to:

- Quickly view and manage their time entries on a monthly basis
- Add and edit time entries with minimal friction
- Navigate between months easily
- Select projects, activities, and issues from searchable dropdowns
- Work efficiently with keyboard shortcuts

### Target Users

RedmineTime is designed for:

- Developers, project managers, and other professionals who use Redmine for time tracking
- Teams that need an efficient way to log time against Redmine projects and issues
- Organizations that use Redmine as their project management tool

## Key Features

- **Monthly Overview**: View all time entries for a selected month in a clean, organized list
- **Quick Entry Creation**: Add new time entries with intelligent defaults and searchable dropdowns
- **Easy Navigation**: Move between months with navigation buttons or keyboard shortcuts
- **Project & Activity Selection**: Choose from all available Redmine projects and activities
- **Issue Integration**: Select issues associated with the chosen project
- **SSL Support**: Works with secure Redmine instances, including those with self-signed certificates
- **Multilingual Support**: Available in French (default) and English with intelligent fallback
- **Theme Options**: Choose between light and dark themes
- **Keyboard Shortcuts**: Improve productivity with keyboard shortcuts for common actions
- **Native Look and Feel**: Integrates with the host operating system's appearance

## Technical Architecture

### Technology Stack

- **Language**: Kotlin 2.1.10
- **UI Framework**: Compose for Desktop 1.7.3
- **API Client**: Redmine Java API 3.1.3
- **Asynchronous Operations**: Kotlin Coroutines 1.10.1
- **Date/Time Handling**: Kotlinx DateTime 0.6.2
- **Dependency Injection**: Koin 4.0.2
- **HTTP Client**: Apache HttpClient 4.5.14

### Architecture Overview

The application follows a modern architecture with:

1. **UI Layer**: Compose UI components in a declarative style
2. **Business Logic**: Kotlin coroutines for asynchronous operations
3. **Data Layer**: RedmineClient for API communication
4. **Configuration**: ConfigurationManager for storing and retrieving user preferences
5. **Dependency Injection**: Koin for managing dependencies

### Key Components

- **Main.kt**: Application entry point and main UI structure
- **RedmineClient**: Handles communication with the Redmine API
- **ConfigurationManager**: Manages user configuration and preferences
- **UI Components**: Modular Compose components for different parts of the interface
- **Internationalization**: Custom Strings resource system for multilingual support

## Development Workflow

### Building from Source

1. Clone the repository
2. Build the application:
   ```bash
   ./gradlew build
   ```
3. Run the application:
   ```bash
   ./gradlew run
   ```

### Creating Native Installers

The application can be packaged as native installers for different platforms:

- **macOS (DMG)**:
  ```bash
  ./gradlew packageReleaseDmg
  ```
- **Windows (MSI)**:
  ```bash
  ./gradlew packageReleaseMsi
  ```
- **Windows (Portable)**:
  ```bash
  ./gradlew createReleaseDistributable
  # Then zip the files
  ```
- **Linux (DEB)**:
  ```bash
  ./gradlew packageReleaseDeb
  ```

### Continuous Integration

The project uses GitHub Actions for continuous integration:

1. On each push to the main branch or pull request:
    - The application is built and tested on Windows and macOS
    - Native installers are created automatically

2. When a new version is ready for release:
    - Create and push a tag with the version number prefixed with 'v' (e.g., `v1.0.0`)
    - GitHub Actions will automatically build installers and create a release

## Configuration

The application can be configured in two ways:

### GUI Configuration

Access the configuration dialog by clicking the settings icon in the top bar. You can set:

- Redmine URL
- Username
- Password
- Dark Theme preference
- Language preference

### Environment Variables

Alternatively, use environment variables (they take precedence over saved configuration):

- `REDMINE_URL`: The URL of your Redmine server
- `REDMINE_USERNAME`: Your Redmine username
- `REDMINE_PASSWORD`: Your Redmine password
- `REDMINE_DARK_THEME`: Set to "true" to enable dark theme

## Project Structure

The project follows a standard Kotlin/Gradle structure:

```
redmine-time/
├── .github/workflows/    # CI/CD workflows
├── src/
│   ├── main/
│   │   ├── kotlin/com/ps/redmine/
│   │   │   ├── api/      # API client code
│   │   │   ├── components/ # UI components
│   │   │   ├── config/   # Configuration management
│   │   │   ├── di/       # Dependency injection
│   │   │   ├── model/    # Data models
│   │   │   ├── resources/ # String resources
│   │   │   ├── util/     # Utility functions
│   │   │   └── Main.kt   # Application entry point
│   │   └── resources/    # Application resources
│   └── test/             # Test code
├── build.gradle.kts      # Gradle build configuration
├── gradle.properties     # Gradle properties
└── README.md             # Project documentation
```

## Contributing Guidelines

When contributing to this project, please follow these guidelines:

1. **Code Style**: Follow Kotlin coding conventions
2. **Internationalization**: Ensure all user-facing strings are added to the Strings resource system
3. **Error Handling**: Use the provided error handling mechanisms for consistent user experience
4. **Testing**: Add appropriate tests for new functionality
5. **Documentation**: Update documentation when adding or changing features
6. **Pull Requests**: Create pull requests against the main branch with clear descriptions

## License

This project is proprietary software. All rights reserved.

## Credits

- Application icon created by Fabrice Perez
    - LinkedIn: [https://www.linkedin.com/in/perezfabrice/](https://www.linkedin.com/in/perezfabrice/)