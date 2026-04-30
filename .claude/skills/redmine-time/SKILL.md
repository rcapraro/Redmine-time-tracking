---
name: redmine-time
description: Project context for the RedmineTime codebase — architecture layers, domain rules (working days, daily hours, ISO week math), and non-obvious conventions. Invoke when implementing features, fixing bugs, reviewing code, or answering questions about how this Compose-for-Desktop Redmine time-tracking app is wired together.
---

# RedmineTime — codebase context

A Compose for Desktop (JVM) app for managing Redmine time entries. Compose targets `compose.desktop.currentOs`; this is
**not** an Android project. JDK 21, Kotlin 2.2, Gradle build.

## Architecture

### Top-level layers

- **`Main.kt`** — single large file with the application entry point and the top-level UI tree (window, theme,
  navigation, error handling). Most app-level state lives here. Koin is started here via
  `startKoin { properties(...); modules(appModule) }`, with the Redmine URI/API key passed as Koin properties.
- **`api/`** — `RedmineClientInterface` is the abstraction; `KtorRedmineClient` is the only implementation. The client
  speaks JSON to the Redmine REST API directly via Ktor + `kotlinx.serialization` (no Ktor `ContentNegotiation` plugin).
  It is `Closeable`, supports SSL with self-signed certs (trust-all + hostname-verification disabled), and exposes
  `updateConfiguration(uri, apiKey)` so the running client picks up new credentials without an app restart.
- **`components/`** — Compose UI components: `ConfigurationDialog`, `DatePicker`, `TimeEntriesList`,
  `WeeklyProgressBars`, `SearchableDropdown`, `ErrorDialog`, `UpdateDialog`.
- **`config/Config.kt`** — `ConfigurationManager` is a singleton object backed by `java.util.prefs.Preferences` under
  node `/com/ps/redmine`. Env vars (`REDMINE_URL`, `REDMINE_API_KEY`, `REDMINE_DARK_THEME`) act as defaults when no
  preference exists. The two Material 3 color schemes (light and dark) are defined in `ui/Color.kt`; selection is a
  single `isDarkTheme: Boolean`. The `Config` data class also holds `nonWorkingIsoDays` (subset of 1..5, capped at 4
  stored entries) and `dailyHours` (clamped to 4.0..7.5, rounded to 0.5).
- **`di/AppModule.kt`** — Koin module wiring `RedmineClientInterface` (singleton), `UpdateService`, `UpdateManager`.
- **`update/`** — `UpdateService` polls GitHub Releases; `UpdateManager` orchestrates download/install for the current
  OS.
- **`resources/Strings.kt`** — custom in-code i18n table (no Compose Resources string table).
  `Strings.updateLanguage("fr"|"en")` switches at runtime; default is French. **Add new user-facing strings to both
  language maps.**
- **`util/`** — date helpers built on `kotlinx.datetime` (not `java.time`), keyboard-shortcut formatting (`OSUtils`
  decides Cmd vs Ctrl), and `WorkHours.configuredDailyHours()` for the configured daily-hour value.

### Architecture pattern

The app is **not** strict MVVM — UI state lives directly in `Main.kt` (Compose `remember`/`mutableStateOf`), with the
Koin-injected `RedmineClientInterface` called from `LaunchedEffect`/coroutines. There is no ViewModel layer and no
Repository wrapper. Don't introduce these abstractions unless the user asks — match the existing flat style.

## Domain rules baked into the code

- **Working days:** Monday–Friday (ISO 1..5). The user can additionally mark up to four of those weekdays as non-working
  via `Config.nonWorkingIsoDays`. Saturday/Sunday are always weekend.
- **Daily hours:** the legacy default is 7.5h (`WorkHours.DAILY_STANDARD_HOURS`), but actual computations should use
  `WorkHours.configuredDailyHours()` so user overrides apply.
- **Weeks-in-month logic** (`getWeeksInMonth`, `isoWeekNumber` in `util/DateUtils.kt`) is ISO-8601 (Monday-start,
  Thursday-anchored). Treat these as the source of truth for week math — don't roll your own.
- **Connection-error classification:** `Main.kt::isConnectionError` distinguishes network/IO failures (and non-422
  `RedmineApiException`) from validation errors so the UI can route to the right dialog. **A 422 from Redmine is treated
  as a validation error, not a connectivity issue.**

## Versioning & releases

- App version comes from the Gradle property `appVersion` in `gradle.properties`. The Gradle task
  `generateVersionFile` (wired as a dependency of `compileKotlin`) writes `src/main/kotlin/com/ps/redmine/Version.kt`
  containing `Version.VERSION`. **Do not hand-edit `Version.kt`** — change `appVersion` instead.
- Releases are tag-driven: pushing `v<X.Y.Z>` triggers `.github/workflows/build.yml`, which rewrites `appVersion` from
  the tag, builds installers on Windows/macOS, and publishes a GitHub release.

## Conventions / gotchas

- Experimental opt-ins (`ExperimentalTime`, `ExperimentalMaterialApi`, `ExperimentalFoundationApi`) are configured *
  *globally** in `build.gradle.kts`. Don't repeat `@OptIn` annotations for these.
- Use `kotlinx.datetime` types (`LocalDate`, `LocalDateTime`, `Instant`) throughout. Avoid `java.time` except at JVM-API
  boundaries.
- The Ktor client manages JSON serialization manually — when adding endpoints, follow the existing pattern in
  `KtorRedmineClient` rather than introducing `ContentNegotiation`.
- Tests use JUnit 5 (`useJUnitPlatform()`), MockK, and `kotlinx-coroutines-test`. The `ktor-client-mock` engine is
  available for HTTP-level tests (see `KtorRedmineClientTest`).
- Toolchain pinned to JDK 21 via `mise.toml` (`liberica-21.0.9+15`) and `jvmToolchain(21)` in `build.gradle.kts`.

## Resource files

- `src/main/composeResources/drawable/` — Compose Resources (accessed via the generated
  `com.ps.redmine_time.generated.resources.Res`).
- `src/main/resources/` — JVM classpath resources, including platform icons (`app_icon.icns`/`.ico`/`.png`) referenced
  by `compose.desktop { nativeDistributions { ... } }`.
