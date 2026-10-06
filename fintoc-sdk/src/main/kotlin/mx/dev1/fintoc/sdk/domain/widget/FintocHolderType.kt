package mx.dev1.fintoc.sdk.domain.widget

/**
 * Kind of account holder the Widget connects.
 *
 * @property code Value sent to Fintoc.
 */
public enum class FintocHolderType(public val code: String) {
    /** A person's accounts. */
    INDIVIDUAL("individual"),

    /** A company's accounts. */
    BUSINESS("business"),
}
