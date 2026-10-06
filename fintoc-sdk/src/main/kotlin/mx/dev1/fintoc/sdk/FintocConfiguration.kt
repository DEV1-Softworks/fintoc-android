package mx.dev1.fintoc.sdk

/**
 * Settings required to initialize the SDK through [Fintoc.initialize].
 *
 * @property authToken Token sent in the `Authorization` header of every API request.
 * @property baseUrl Root URL of the Fintoc API. Override it only to target a different environment.
 */
public data class FintocConfiguration(
    val authToken: String,
    val baseUrl: String = DEFAULT_BASE_URL,
) {
    init {
        require(authToken.isNotBlank()) { "authToken must not be blank." }
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank." }
    }

    /** Hides [authToken] so credentials never reach logs or crash reports. */
    override fun toString(): String = "FintocConfiguration(authToken=<redacted>, baseUrl=$baseUrl)"

    public companion object {
        /** Production endpoint of the Fintoc API. */
        public const val DEFAULT_BASE_URL: String = "https://api.fintoc.com"
    }
}
