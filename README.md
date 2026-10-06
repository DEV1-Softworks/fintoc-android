# Fintoc Android SDK

**English** · [Español](docs/es/README.md) · [Français](docs/fr/README.md) · [Português](docs/pt/README.md)

Unofficial, community-built Kotlin and Jetpack Compose SDK to add [Fintoc](https://fintoc.com) payments (such as
SPEI in Mexico) and bank connections to an Android app. It wraps Fintoc's Widget, is built Compose-first and uses Koin
for dependency injection.

> **Not affiliated with Fintoc.** This is a community project. "Fintoc" is a trademark of its respective owners.

> **Status: in development.** The build, the dependency-injection container, the test pipeline, the sample app and the
> Widget configuration (public key check, options per product and URL builder) are in place. The Widget view, event
> handling and hosted checkout arrive feature by feature through pull requests.

## Modules

| Module | Purpose |
|---|---|
| [`fintoc-sdk`](fintoc-sdk) | The library published to Maven: `mx.dev1.fintoc:fintoc-sdk`. |
| [`app`](app) | Sample application that consumes the SDK. Not published. |

```mermaid
flowchart LR
    host["Your app"] --> sdk["fintoc-sdk"]
    sample["app (sample)"] --> sdk
    sdk --> koin["Koin (isolated container)"]
    sdk --> compose["Jetpack Compose"]
```

## Requirements

| Tool | Version |
|---|---|
| JDK to launch Gradle | 11 or newer (the build provisions a JDK 21 toolchain automatically) |
| Android SDK Platform | 37 (`compileSdk`) |
| Android Studio | A recent stable release that supports Android Gradle Plugin 9.4 |
| Device or emulator | Android 6.0 (API 23) or newer, only needed for instrumented tests |

Supported on Android 6.0 (API 23) and newer. The SDK is compiled with `compileSdk 37`; because it depends on
Jetpack Compose, host applications must compile against `compileSdk 37` too, or pin an older Compose BOM.

## Quick start

```bash
git clone git@github.com:DEV1-Softworks/fintoc-android.git
cd fintoc-android

./gradlew :app:assembleDebug          # build the sample app
./gradlew testDebugUnitTest           # unit tests (JUnit, Robolectric, Mockito)
```

Using the SDK from an app:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Fintoc.initialize(
            context = this,
            configuration = FintocConfiguration(publicKey = "pk_test_…"),
        )
    }
}
```

Describe what to show with `FintocWidgetOptions`. The tokens come from your backend:

```kotlin
val options = FintocWidgetOptions.Payments(sessionToken = tokenFromYourBackend)
```

The Widget view that shows these options arrives in an upcoming pull request.

## Security model

- The app only holds the **public key** (`pk_test_` or `pk_live_`). The SDK refuses secret keys (`sk_…`).
- Your backend creates the Checkout Session with its secret key and hands the short-lived `session_token` to the app;
  the app passes it to the SDK.
- Tokens are never logged: the `toString()` of every option redacts them.
- What the Widget reports to the app is not proof of payment. Confirm payments with Fintoc webhooks on your backend.

## Documentation

| Topic | English | Español | Français | Português |
|---|---|---|---|---|
| Overview | this file | [es](docs/es/README.md) | [fr](docs/fr/README.md) | [pt](docs/pt/README.md) |
| Architecture | [en](docs/en/architecture.md) | [es](docs/es/architecture.md) | [fr](docs/fr/architecture.md) | [pt](docs/pt/architecture.md) |
| Contributing | [en](docs/en/contributing.md) | [es](docs/es/contributing.md) | [fr](docs/fr/contributing.md) | [pt](docs/pt/contributing.md) |

## Credits and license

The Widget integration follows [Fintoc's public documentation](https://docs.fintoc.com) and the behavior of the
official [React Native SDK](https://github.com/fintoc-com/fintoc-react-native). The MIT notice of the community Swift
client [sergiocampama/Fintoc](https://github.com/sergiocampama/Fintoc) (© 2021 Sergio Campamá) is kept in
[NOTICE](NOTICE) in case code derived from it is added.

This project is licensed under the [Apache License 2.0](LICENSE).
