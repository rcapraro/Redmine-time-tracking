# RedmineTime Development Guidelines

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
- **Automatic Updates**: Built-in update system that checks for new versions and handles downloads/installation

## Technical Architecture

### Technology Stack

- **Language**: Kotlin 2.2.0
- **UI Framework**: Compose for Desktop 1.8.2
- **HTTP Client**: Ktor 3.2.0
- **Asynchronous Operations**: Kotlin Coroutines 1.10.2
- **Date/Time Handling**: Kotlinx DateTime 0.7.0
- **Dependency Injection**: Koin 4.1.0
- **Serialization**: Kotlinx Serialization 1.9.0
- **Testing**: JUnit 5, MockK, Coroutines Test

### Architecture Overview

The application follows a modern layered architecture with:

1. **UI Layer**: Compose UI components in a declarative style
2. **Business Logic**: Kotlin coroutines for asynchronous operations
3. **Data Layer**: Ktor client for API communication
4. **Configuration**: ConfigurationManager for storing and retrieving user preferences
5. **Dependency Injection**: Koin for managing dependencies

### Key Components

- **Main.kt**: Application entry point and main UI structure
- **KtorRedmineClient**: Handles communication with the Redmine API
- **ConfigurationManager**: Manages user configuration and preferences
- **UpdateManager**: Manages automatic update checks, downloads, and installation
- **UpdateService**: Handles GitHub API communication for version checking and file downloads
- **UI Components**: Modular Compose components for different parts of the interface
- **Internationalization**: Custom Strings resource system for multilingual support

## Kotlin Development Best Practices

### Code Style and Conventions

#### Naming Conventions

- **Classes**: Use PascalCase (`UserRepository`, `TimeEntryService`)
- **Functions and Variables**: Use camelCase (`getUserData`, `timeEntryList`)
- **Constants**: Use SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Packages**: Use lowercase with dots (`com.ps.redmine.api`)

#### Function Design

```kotlin
// ✅ Good: Single responsibility, clear naming
fun calculateWorkingHours(entries: List<TimeEntry>): Double {
    return entries.sumOf { it.hours }
}

// ❌ Bad: Multiple responsibilities, unclear naming
fun processData(data: Any): Any {
    // Complex logic doing multiple things
}
```

#### Null Safety

```kotlin
// ✅ Good: Explicit null handling
fun getProjectName(project: Project?): String {
    return project?.name ?: "Unknown Project"
}

// ✅ Good: Safe calls with let
project?.let { proj ->
    updateUI(proj.name)
}

// ❌ Bad: Force unwrapping
val name = project!!.name // Dangerous!
```

#### Data Classes and Immutability

```kotlin
// ✅ Good: Immutable data class
data class TimeEntry(
    val id: Int,
    val projectId: Int,
    val hours: Double,
    val date: LocalDate,
    val description: String
)

// ✅ Good: Use copy for modifications
val updatedEntry = originalEntry.copy(hours = 8.0)
```

### Coroutines Best Practices

#### Structured Concurrency

```kotlin
// ✅ Good: Use structured concurrency
class TimeEntryRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun loadTimeEntries(): List<TimeEntry> = withContext(Dispatchers.IO) {
        // Network call
    }

    fun cleanup() {
        scope.cancel()
    }
}
```

#### Error Handling

```kotlin
// ✅ Good: Proper error handling with Result
suspend fun fetchTimeEntries(): Result<List<TimeEntry>> {
    return try {
        val entries = api.getTimeEntries()
        Result.success(entries)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### Flow Usage

```kotlin
// ✅ Good: Use Flow for reactive data
class TimeEntryViewModel {
    private val _timeEntries = MutableStateFlow<List<TimeEntry>>(emptyList())
    val timeEntries: StateFlow<List<TimeEntry>> = _timeEntries.asStateFlow()

    fun loadEntries() {
        viewModelScope.launch {
            repository.getTimeEntries()
                .catch { emit(emptyList()) }
                .collect { _timeEntries.value = it }
        }
    }
}
```

### Performance Optimization

#### Collection Operations

```kotlin
// ✅ Good: Use appropriate collection operations
val activeProjects = projects.filter { it.isActive }
val projectNames = projects.map { it.name }
val totalHours = entries.sumOf { it.hours }

// ✅ Good: Use sequences for large datasets
val result = largeList.asSequence()
    .filter { it.isValid }
    .map { it.transform() }
    .take(10)
    .toList()
```

#### Memory Management

```kotlin
// ✅ Good: Use lazy initialization
class ExpensiveResource {
    private val heavyComputation by lazy {
        performHeavyComputation()
    }
}
```

## Compose for Desktop Best Practices

### Component Design

#### Composable Function Structure

```kotlin
// ✅ Good: Well-structured composable
@Composable
fun TimeEntryCard(
    timeEntry: TimeEntry,
    onEdit: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        // Content
    }
}
```

#### State Management

```kotlin
// ✅ Good: Proper state hoisting
@Composable
fun TimeEntryForm(
    initialEntry: TimeEntry? = null,
    onSave: (TimeEntry) -> Unit,
    onCancel: () -> Unit
) {
    var description by remember { mutableStateOf(initialEntry?.description ?: "") }
    var hours by remember { mutableStateOf(initialEntry?.hours?.toString() ?: "") }

    // UI implementation
}
```

#### Performance Optimization

```kotlin
// ✅ Good: Use remember for expensive calculations
@Composable
fun TimeEntriesList(entries: List<TimeEntry>) {
    val sortedEntries = remember(entries) {
        entries.sortedByDescending { it.date }
    }

    LazyColumn {
        items(sortedEntries, key = { it.id }) { entry ->
            TimeEntryItem(entry = entry)
        }
    }
}

// ✅ Good: Use derivedStateOf for computed state
@Composable
fun ProjectSummary(entries: List<TimeEntry>) {
    val totalHours by remember {
        derivedStateOf {
            entries.sumOf { it.hours }
        }
    }

    Text("Total: $totalHours hours")
}
```

### UI Best Practices

#### Layout and Spacing

```kotlin
// ✅ Good: Consistent spacing and layout
@Composable
fun TimeEntryForm() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form fields
    }
}
```

#### Accessibility

```kotlin
// ✅ Good: Accessibility support
@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = "Delete time entry"
            role = Role.Button
        }
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
    }
}
```

#### Theme and Styling

```kotlin
// ✅ Good: Use Material Theme
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors()
    } else {
        lightColors()
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### Side Effects Management

#### LaunchedEffect Usage

```kotlin
// ✅ Good: Proper LaunchedEffect usage
@Composable
fun TimeEntriesScreen(viewModel: TimeEntriesViewModel) {
    val entries by viewModel.timeEntries.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTimeEntries()
    }

    // UI implementation
}
```

#### DisposableEffect for Cleanup

```kotlin
// ✅ Good: Cleanup resources
@Composable
fun TimerComponent() {
    DisposableEffect(Unit) {
        val timer = Timer()
        // Start timer

        onDispose {
            timer.cancel()
        }
    }
}
```

## Architecture Patterns

### MVVM Pattern

```kotlin
// ✅ Good: ViewModel implementation
class TimeEntriesViewModel(
    private val repository: TimeEntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeEntriesUiState())
    val uiState: StateFlow<TimeEntriesUiState> = _uiState.asStateFlow()

    fun loadTimeEntries(month: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getTimeEntries(month)
                .onSuccess { entries ->
                    _uiState.value = _uiState.value.copy(
                        entries = entries,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
        }
    }
}

data class TimeEntriesUiState(
    val entries: List<TimeEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Repository Pattern

```kotlin
// ✅ Good: Repository with interface
interface TimeEntryRepository {
    suspend fun getTimeEntries(month: YearMonth): Result<List<TimeEntry>>
    suspend fun createTimeEntry(entry: TimeEntry): Result<TimeEntry>
    suspend fun updateTimeEntry(entry: TimeEntry): Result<TimeEntry>
    suspend fun deleteTimeEntry(id: Int): Result<Unit>
}

class TimeEntryRepositoryImpl(
    private val apiClient: RedmineClient
) : TimeEntryRepository {

    override suspend fun getTimeEntries(month: YearMonth): Result<List<TimeEntry>> {
        return try {
            val entries = apiClient.getTimeEntries(month)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Testing Best Practices

### Unit Testing

```kotlin
// ✅ Good: Comprehensive unit tests
class TimeEntryRepositoryTest {

    @MockK
    private lateinit var apiClient: RedmineClient

    private lateinit var repository: TimeEntryRepository

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        repository = TimeEntryRepositoryImpl(apiClient)
    }

    @Test
    fun `getTimeEntries returns success when API call succeeds`() = runTest {
        // Given
        val month = YearMonth.of(2024, 1)
        val expectedEntries = listOf(
            TimeEntry(1, 1, 8.0, LocalDate.of(2024, 1, 1), "Work")
        )
        coEvery { apiClient.getTimeEntries(month) } returns expectedEntries

        // When
        val result = repository.getTimeEntries(month)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedEntries, result.getOrNull())
    }
}
```

### Compose Testing

```kotlin
// ✅ Good: Compose UI tests
class TimeEntryFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `form displays initial values correctly`() {
        val initialEntry = TimeEntry(1, 1, 8.0, LocalDate.now(), "Test")

        composeTestRule.setContent {
            TimeEntryForm(
                initialEntry = initialEntry,
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule
            .onNodeWithText("Test")
            .assertIsDisplayed()
    }
}
```

## Error Handling and Logging

### Exception Handling

```kotlin
// ✅ Good: Structured error handling
sealed class AppError : Exception() {
    object NetworkError : AppError()
    object AuthenticationError : AppError()
    data class ValidationError(val field: String) : AppError()
}

// ✅ Good: Error handling in UI
@Composable
fun ErrorDialog(
    error: AppError?,
    onDismiss: () -> Unit
) {
    error?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error") },
            text = {
                Text(
                    when (it) {
                        is AppError.NetworkError -> "Network connection failed"
                        is AppError.AuthenticationError -> "Authentication failed"
                        is AppError.ValidationError -> "Invalid ${it.field}"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    }
}
```

## Build Configuration and Dependencies

### Gradle Best Practices

```kotlin
// ✅ Good: Version catalog usage
dependencies {
    implementation(libs.compose.desktop.currentOs)
    implementation(libs.ktor.client.core)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}

// ✅ Good: Compiler optimizations
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
            )
        )
    }
}
```

### Dependency Management

- Use version catalogs for dependency management
- Keep dependencies up to date with automated tools
- Separate implementation and API dependencies
- Use test-specific dependencies appropriately

## Code Quality and Documentation

### Documentation Standards

```kotlin
/**
 * Represents a time entry in the Redmine system.
 *
 * @property id Unique identifier for the time entry
 * @property projectId ID of the associated project
 * @property hours Number of hours worked
 * @property date Date when the work was performed
 * @property description Description of the work performed
 */
data class TimeEntry(
    val id: Int,
    val projectId: Int,
    val hours: Double,
    val date: LocalDate,
    val description: String
)

/**
 * Loads time entries for the specified month.
 *
 * @param month The year-month to load entries for
 * @return Result containing the list of time entries or an error
 */
suspend fun loadTimeEntries(month: YearMonth): Result<List<TimeEntry>>
```

### Code Review Guidelines

1. **Functionality**: Does the code work as intended?
2. **Readability**: Is the code easy to understand?
3. **Performance**: Are there any performance concerns?
4. **Testing**: Are there adequate tests?
5. **Security**: Are there any security vulnerabilities?
6. **Architecture**: Does it follow the established patterns?

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
4. Run tests:
   ```bash
   ./gradlew test
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

The project follows a standard Kotlin/Gradle structure with clear separation of concerns:

```
redmine-time/
├── .github/workflows/    # CI/CD workflows
├── src/
│   ├── main/
│   │   ├── kotlin/com/ps/redmine/
│   │   │   ├── api/      # API client code
│   │   │   ├── components/ # Reusable UI components
│   │   │   ├── config/   # Configuration management
│   │   │   ├── di/       # Dependency injection modules
│   │   │   ├── model/    # Data models and DTOs
│   │   │   ├── resources/ # String resources and i18n
│   │   │   ├── util/     # Utility functions and extensions
│   │   │   └── Main.kt   # Application entry point
│   │   ├── composeResources/ # Compose resources (images, etc.)
│   │   └── resources/    # Application resources (icons, etc.)
│   └── test/             # Test code mirroring main structure
├── build.gradle.kts      # Gradle build configuration
├── gradle.properties     # Gradle properties
├── gradle/libs.versions.toml # Version catalog
└── README.md             # Project documentation
```

## Contributing Guidelines

When contributing to this project, please follow these comprehensive guidelines:

### Code Standards

1. **Kotlin Conventions**: Follow official Kotlin coding conventions
2. **Compose Guidelines**: Adhere to Compose best practices outlined above
3. **Architecture**: Maintain the established MVVM pattern and layered architecture
4. **Testing**: Write comprehensive unit and integration tests
5. **Documentation**: Document public APIs with KDoc
6. **Performance**: Consider performance implications of changes

### Development Process

1. **Branch Strategy**: Create feature branches from main
2. **Commit Messages**: Use conventional commit format
3. **Code Review**: All changes must be reviewed before merging
4. **Testing**: Ensure all tests pass before submitting PR
5. **Documentation**: Update relevant documentation

### Quality Assurance

1. **Static Analysis**: Use ktlint and detekt for code quality
2. **Test Coverage**: Maintain high test coverage
3. **Performance Testing**: Profile performance-critical changes
4. **Accessibility**: Ensure UI components are accessible
5. **Internationalization**: Add all user-facing strings to resource system

## License

This project is proprietary software. All rights reserved.

## Credits

- Application icon created by Fabrice Perez
    - LinkedIn: [https://www.linkedin.com/in/perezfabrice/](https://www.linkedin.com/in/perezfabrice/)