package mx.dev1.fintoc.sdk.domain.security

/** Stops Fintoc secret keys (`sk_…`) from entering the SDK. Secret keys belong on the host app's backend only. */
internal object SecretKeyGuard {
    private const val SECRET_KEY_PREFIX = "sk_"

    fun looksLikeSecretKey(value: String): Boolean = value.startsWith(SECRET_KEY_PREFIX)

    /** The failure message never repeats [value], so a leaked key does not also end up in a stack trace. */
    fun requireNotSecretKey(parameterName: String, value: String) {
        require(!looksLikeSecretKey(value)) {
            "$parameterName looks like a secret key (sk_). Secret keys must never be embedded in an app: " +
                "keep them on your backend."
        }
    }
}
