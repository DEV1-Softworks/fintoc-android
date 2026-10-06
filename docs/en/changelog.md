# Changelog

[Español](../es/changelog.md) · [Français](../fr/changelog.md) · [Português](../pt/changelog.md) · [Back to README](../../README.md)

All notable changes are listed here, newest first. The format follows [Keep a Changelog](https://keepachangelog.com), and the
project follows [Semantic Versioning](https://semver.org). While the major version is 0, a minor version may change the
public API.

## Unreleased

The first release will be `0.1.0`. It contains:

### Added

- **Widget configuration.** `FintocConfiguration`, which accepts only public keys (`pk_test_` and `pk_live_`) and refuses
  secret keys, and `FintocWidgetOptions` for `Payments`, `Movements` and `Subscriptions`, with a URL builder that
  percent-encodes every value.
- **Widget events.** `FintocWidgetEvent` (`Succeeded`, `Exited`, `Occurred`), `FintocWidgetEventType` and
  `FintocLinkIntentResult`, read from the `fintocwidget://` redirects of the Widget by a parser that never throws.
- **`FintocWidget` for Compose**, with an overload that takes the options and one that asks a `suspend` provider for the
  session token. The WebView is hardened, only Fintoc hosts stay inside it, other `https` links open in the browser, and a
  failed load shows the SDK's own message with a retry instead of the address of the page.
- **An Activity host for apps without Compose.** `FintocWidgetContract` and `FintocWidgetResult`. The session token never
  travels in the `Intent`, the screen hides itself from screenshots, and it survives a rotation.
- **`FintocHostedCheckout`**, which opens a Fintoc-hosted checkout in a Custom Tab and tells whether the address that came
  back is your success address, your cancel address or neither. Secret values in your own addresses must come back.
- **Four languages** for the texts of the SDK (English, Spanish, French and Portuguese), following the device or forced
  with `FintocConfiguration.language`.
- **Accessibility checks** with Google's Accessibility Test Framework, at the default and at the largest font and display
  sizes.
- **An isolated Koin container** that owns what has state, so `Fintoc.shutdown()` releases every session token.
- **A sample app** that uses every part of the SDK against Fintoc's sandbox.
- **Publishing to Maven Central**, with sources, API documentation and signatures.

### Compatibility

- Android 6.0 (API 23) and newer. Apps must compile against `compileSdk 37`.
- Kotlin 2.2 or newer. The library is compiled at language level 2.2 and asks for the 2.2 standard library.
