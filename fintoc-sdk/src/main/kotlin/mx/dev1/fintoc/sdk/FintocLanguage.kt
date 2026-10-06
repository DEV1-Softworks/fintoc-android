package mx.dev1.fintoc.sdk

/**
 * Languages of the SDK's own screens and messages, such as the loading indicator and the "try again" prompt.
 *
 * Without an override (see [FintocConfiguration.language]) the SDK follows the language of the device and, on Android
 * 13 and newer, the language chosen for your app in the system settings. The Widget page itself is Fintoc's web
 * page: it does not take its language from the SDK.
 *
 * @property languageTag IETF language tag of the language.
 */
public enum class FintocLanguage(public val languageTag: String) {
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr"),
    PORTUGUESE("pt"),
}
