package mx.dev1.fintoc.sdk

import mx.dev1.fintoc.sdk.domain.security.SecretKeyGuard

/**
 * Settings required to initialize the SDK through [Fintoc.initialize].
 *
 * The SDK only ever needs the **public** key. Secret keys (`sk_…`) give full access to your Fintoc account, so they
 * must stay on your backend and are rejected here.
 *
 * @property publicKey Public key of your Fintoc account: `pk_test_…` for the sandbox or `pk_live_…` for production.
 * @throws IllegalArgumentException If the key is blank, contains whitespace, looks like a secret key, or does not
 * start with `pk_test_` or `pk_live_`. The message never repeats the rejected value.
 */
public data class FintocConfiguration(
    val publicKey: String,
) {
    init {
        require(publicKey.isNotBlank()) { "publicKey must not be blank." }
        require(publicKey.none { character -> character.isWhitespace() }) {
            "publicKey must not contain whitespace."
        }
        SecretKeyGuard.requireNotSecretKey(parameterName = "publicKey", value = publicKey)
        require(publicKey.startsWith(TEST_KEY_PREFIX) || publicKey.startsWith(LIVE_KEY_PREFIX)) {
            "publicKey must start with $TEST_KEY_PREFIX or $LIVE_KEY_PREFIX."
        }
    }

    /** Environment deduced from the [publicKey] prefix. */
    public val environment: FintocEnvironment
        get() = if (publicKey.startsWith(LIVE_KEY_PREFIX)) FintocEnvironment.LIVE else FintocEnvironment.TEST

    /** Shows the environment only, so keys never reach logs or crash reports even though public keys are not secret. */
    override fun toString(): String = "FintocConfiguration(publicKey=<redacted>, environment=$environment)"

    private companion object {
        const val TEST_KEY_PREFIX = "pk_test_"
        const val LIVE_KEY_PREFIX = "pk_live_"
    }
}
