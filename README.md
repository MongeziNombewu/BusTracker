# BusTracker

BusTracker is an Android app that helps users search for stops, plan journeys, and track live bus positions using
Transport for London (TfL) APIs.

## Conventions

### Code organization

The codebase follows a layered structure with clear package boundaries:

- `data`: network APIs, DTOs, and repositories.
- `domain`: use-cases and domain models.
- `presentation`: Compose UI, navigation, UI state, and ViewModels.
- `di`: dependency injection wiring via Koin modules.

Inside `presentation`, files are organized by feature (`search`, `journeyresults`, `tracking`) to keep UI logic
cohesive.
Normally for larger apps I would structure by feature at the top level, but given the small size of this app I felt it
was more readable to keep all presentation logic together and separate by feature within that.

### Naming and structure

- `*Repository` classes handle remote data access.
- `*UseCase` classes hold business rules and map data layer results into domain models.
- `*ViewModel` classes orchestrate screen state and events.
- `*UiState` sealed types represent screen states explicitly (`Loading`, `Results`, `Error`, etc.).
- Route names are centralized in `presentation/navigation/NavGraph.kt` (`Routes` object).

### State and async conventions

- UI state is exposed as `StateFlow` from ViewModels.
- One-off navigation events are emitted through channels/flows.
- Coroutines are used for asynchronous work

### Dependency injection conventions

DI is configured in `di/AppModules.kt` and grouped by concern:

- `networkModule`
- `repositoryModule`
- `useCaseModule`
- `viewModelModule`

This keeps wiring explicit and makes unit testing easier by allowing dependencies to be mocked.

## Architecture Used

BusTracker uses a pragmatic layered Clean Architecture style with unidirectional UI state flow:

1. **Presentation layer**: Compose screens and ViewModels consume use-cases and render immutable UI states.
2. **Domain layer**: use-cases encapsulate business operations (`SearchStopPointsUseCase`, `PlanJourneyUseCase`, etc.).
3. **Data layer**: repositories call `TflApiService` and map API behavior into app-friendly results.

## General Considerations

- **Platform baseline**: `minSdk = 33`, `targetSdk = 36`, Java/Kotlin 17 toolchain.
- **UI stack**: Jetpack Compose + Material 3 + Navigation Compose.
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization.
- **Maps**: Google Maps SDK + Maps Compose.
- **Logging**: Timber for app logs; OkHttp logging interceptor is configured at BODY level in `AppModules.kt`.
- **Error handling**: repository logic includes special handling for TfL disambiguation responses (HTTP `300`).
- **Testing**: unit tests target repositories, use-cases, and ViewModels with MockK and MockWebServer.

## Handling Sensitive Data and Secrets

BusTracker uses property-based key injection and avoids hardcoding secrets in source files.
The Gradle Secrets Plugin is configured to read keys from `local.properties` and inject them as build config fields
and manifest placeholders, keeping them out of version control.

### What must be treated as secret

- `TFL_API_KEY`
- `MAPS_API_KEY`

### Local development setup

Store keys in `local.properties` (already ignored by `.gitignore`):

```properties
# local.properties (not committed to VCS)
TFL_API_KEY=your_tfl_api_key_here
MAPS_API_KEY=your_google_maps_api_key_here
```

### How keys are consumed in the app

- `app/build.gradle.kts` reads Gradle properties and injects:
  - `BuildConfig.TFL_API_KEY`
  - `BuildConfig.MAPS_API_KEY`
- `MAPS_API_KEY` is passed into AndroidManifest via `manifestPlaceholders` and consumed by:
  - `<meta-data android:name="com.google.android.geo.API_KEY" ... />`
- `TFL_API_KEY` is appended by `ApiKeyInterceptor` as query parameter `app_key` on outgoing TfL requests.

## Other considerations

- Interfaces and abstractions are kept minimal for simplicity, but could be expanded for more complex features or
  testing needs
- Design and UX was based on low fidelity screen , while the app's visual style is based on Material 3 defaults. This
  was due to time constraints and no access to Figma
- The app is not localized and only supports English, but all strings are stored in `strings.xml` for easy future
  localization
- The app does not include analytics or crash reporting, but these could be added in the future
- Some error cases (e.g. network failures) are handled with standard messages, but more specific handling could be added
  in the future
- The app's Compose UI has room for improvement (e.g. better adaptation for different orientation/screen sizes, more
  polished loading/error states, etc.)
  but is functional and demonstrates the core features effectively

## Third-Party Dependencies

### External service providers

| Provider                           | Purpose                                                           |
|------------------------------------|-------------------------------------------------------------------|
| **Transport for London (TfL) API** | Stop search, journey planning, line and arrival data.             |
| **Google Maps Platform**           | Map rendering and route/bus position visualization in the app UI. |

### Build plugins and tooling

| Dependency                                                        | Type                   | Purpose                                                                   |
|-------------------------------------------------------------------|------------------------|---------------------------------------------------------------------------|
| `com.android.application` (AGP)                                   | Gradle plugin          | Android application build pipeline (compile, package, signing, variants). |
| `org.jetbrains.kotlin.android`                                    | Gradle plugin          | Kotlin support for Android modules.                                       |
| `org.jetbrains.kotlin.plugin.compose`                             | Gradle plugin          | Compose compiler integration.                                             |
| `org.jetbrains.kotlin.plugin.serialization`                       | Gradle plugin          | Kotlinx Serialization codegen support.                                    |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | Gradle plugin          | Secure property loading for API keys and manifest placeholders.           |
| `org.gradle.toolchains.foojay-resolver-convention`                | Gradle settings plugin | Resolves JVM toolchains via Foojay.                                       |

### AndroidX and UI

| Dependency                                       | Purpose                                         |
|--------------------------------------------------|-------------------------------------------------|
| `androidx.core:core-ktx`                         | Kotlin-friendly Android core APIs.              |
| `androidx.lifecycle:lifecycle-runtime-ktx`       | Lifecycle-aware coroutines and runtime support. |
| `androidx.lifecycle:lifecycle-runtime-compose`   | Lifecycle bindings for Compose.                 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel integration for Compose.              |
| `androidx.activity:activity-compose`             | Compose host Activity integration.              |
| `androidx.compose:compose-bom`                   | Version alignment for Compose artifacts.        |
| `androidx.compose.ui:ui`                         | Base Compose UI toolkit.                        |
| `androidx.compose.ui:ui-graphics`                | Graphics primitives for Compose.                |
| `androidx.compose.ui:ui-tooling-preview`         | Design-time preview support.                    |
| `androidx.compose.ui:ui-tooling`                 | Debug tooling/inspection for Compose UI.        |
| `androidx.compose.material3:material3`           | Material 3 components and theming.              |
| `androidx.navigation:navigation-compose`         | In-app navigation for Compose screens.          |

### Dependency injection

| Dependency                             | Purpose                                             |
|----------------------------------------|-----------------------------------------------------|
| `io.insert-koin:koin-android`          | Runtime dependency injection container for Android. |
| `io.insert-koin:koin-androidx-compose` | Koin integration in Compose.                        |

### Networking and serialization

| Dependency                                               | Purpose                                              |
|----------------------------------------------------------|------------------------------------------------------|
| `com.squareup.retrofit2:retrofit`                        | Declarative HTTP client interface for TfL endpoints. |
| `com.squareup.retrofit2:converter-kotlinx-serialization` | Retrofit converter for Kotlinx Serialization.        |
| `com.squareup.okhttp3:okhttp`                            | HTTP transport layer used by Retrofit.               |
| `com.squareup.okhttp3:logging-interceptor`               | HTTP request/response logging.                       |
| `org.jetbrains.kotlinx:kotlinx-serialization-json`       | JSON parsing/encoding for DTOs.                      |

### Maps and location visualization

| Dependency                                  | Purpose                                     |
|---------------------------------------------|---------------------------------------------|
| `com.google.maps.android:maps-compose`      | Compose wrappers for Google Maps.           |
| `com.google.android.gms:play-services-maps` | Core Google Maps Android SDK functionality. |

### Concurrency and logging

| Dependency                                         | Purpose                                   |
|----------------------------------------------------|-------------------------------------------|
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Main/UI coroutine dispatching on Android. |
| `com.jakewharton.timber:timber`                    | Structured application logging.           |

### Testing

| Dependency                                      | Purpose                                      |
|-------------------------------------------------|----------------------------------------------|
| `junit:junit`                                   | Unit test framework.                         |
| `io.mockk:mockk`                                | Kotlin-first mocking framework.              |
| `androidx.arch.core:core-testing`               | Architecture component test helpers.         |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | Coroutine testing utilities and dispatchers. |
| `com.squareup.okhttp3:mockwebserver`            | Local HTTP server for networking tests.      |
