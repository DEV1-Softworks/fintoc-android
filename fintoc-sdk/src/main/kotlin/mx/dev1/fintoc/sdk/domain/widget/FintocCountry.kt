package mx.dev1.fintoc.sdk.domain.widget

/**
 * Country the Widget connects to.
 *
 * @property code Lowercase ISO 3166-1 alpha-2 code sent to Fintoc.
 */
public enum class FintocCountry(public val code: String) {
    /** Chile. This is Fintoc's default. */
    CHILE("cl"),

    /** Mexico. */
    MEXICO("mx"),
}
