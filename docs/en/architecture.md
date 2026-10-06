# Architecture

[Español](../es/architecture.md) · [Français](../fr/architecture.md) · [Português](../pt/architecture.md) · [Back to README](../../README.md)

This page explains how the project is organized. You do not need prior Android experience to follow it.

## Big picture

The repository is a Gradle build with two modules:

- **`fintoc-sdk`** is an *Android library*: code that other apps include as a dependency. It is what gets published to Maven.
- **`app`** is an *Android application* that uses the SDK the way a customer would. It doubles as a living example.

```mermaid
flowchart LR
    subgraph repo["fintoc-android repository"]
        app["app\n(sample application)"]
        sdk["fintoc-sdk\n(published library)"]
    end
    app -->|"implementation(project)"| sdk
    host["Customer app"] -->|"implementation('mx.dev1.fintoc:fintoc-sdk')"| sdk
```

## Clean architecture

Every module follows the same three layers. Dependencies only point **inwards**, towards the domain.

```mermaid
flowchart TB
    presentation["presentation\nJetpack Compose screens, view models"]
    domain["domain\nmodels, use cases, repository interfaces\n(pure Kotlin, no Android)"]
    data["data\nHTTP client, DTOs, repository implementations"]
    di["di\nKoin modules that wire the layers together"]

    presentation --> domain
    data --> domain
    di -.->|creates| presentation
    di -.->|creates| data
```

| Layer | Knows about | Must never know about |
|---|---|---|
| `domain` | Kotlin standard library, coroutines | Android, Koin, HTTP, Compose |
| `data` | `domain` | `presentation`, Compose |
| `presentation` | `domain` | `data` |
| `di` | all layers | — |

Today only the `di` package and the public entry point exist. The other layers are created as features arrive, under the
base package `mx.dev1.fintoc.sdk`.

## Public API and explicit API mode

The SDK enables Kotlin's **explicit API mode**. Every declaration is `internal` unless it is deliberately marked
`public`, so the public surface of the library is always intentional. Today it is:

| Type | Role |
|---|---|
| `Fintoc` | Entry point: `initialize`, `shutdown`, `isInitialized`. |
| `FintocConfiguration` | Settings: `authToken` and `baseUrl`. Its `toString()` redacts the token so it never reaches logs. |

## Dependency injection with an isolated Koin container

Koin is the dependency-injection framework. The SDK creates **its own** container instead of using Koin's global one.
That way the SDK can never collide with a Koin instance that the host app starts.

```mermaid
sequenceDiagram
    participant Host as Host app
    participant Fintoc as Fintoc (object)
    participant Container as FintocKoinContainer
    participant Koin as Isolated KoinApplication

    Host->>Fintoc: initialize(context, configuration)
    Fintoc->>Container: close previous container, if any
    Fintoc->>Container: FintocKoinContainer(context, configuration)
    Container->>Koin: koinApplication { modules(coreModule) }
    Note over Koin: Registers Context (application context)<br/>and FintocConfiguration
    Host->>Fintoc: shutdown()
    Fintoc->>Container: close()
```

Internal code gets dependencies through `Fintoc.requireKoin()`. If the host forgot to call `initialize`, it fails
immediately with a message explaining what to do.

## Build decisions

| Decision | Value | Why |
|---|---|---|
| Kotlin | 2.4.20 | Current stable release. Android Gradle Plugin 9 ships Kotlin support built in, so no separate Kotlin plugin is applied. |
| Android Gradle Plugin | 9.4.1 (requires Gradle 9.6.0) | Current stable release. |
| `minSdk` | 28 (Android 9) | Kept from the project template. Lowering it is a one-line change in each module. |
| `compileSdk` | 37 | Required by Compose BOM 2026.09.00. |
| `targetSdk` (sample app) | 36 | The level currently required by Google Play. |
| Jetpack Compose | BOM 2026.09.00 | Compose-first UI. XML is only used where the platform requires it (manifest, window theme). |
| Java bytecode | 11 | Keeps the library consumable by as many host apps as possible. |
| Versions | `gradle/libs.versions.toml` | Single place for every dependency version. |

## Testing and coverage

Three kinds of tests exist, and the coverage report merges all of them:

| Kind | Location | Tools | Runs on |
|---|---|---|---|
| Unit | `src/test` | JUnit 4, Mockito, Robolectric | Your computer |
| Instrumented | `src/androidTest` | AndroidX Test, Espresso, Compose UI test | Device or emulator |
| Coverage | `gradle/jacoco-coverage.gradle.kts` | JaCoCo | Combines both |

The build fails when line or instruction coverage of a module drops below **80%**. See [Contributing](contributing.md)
for the exact commands.
