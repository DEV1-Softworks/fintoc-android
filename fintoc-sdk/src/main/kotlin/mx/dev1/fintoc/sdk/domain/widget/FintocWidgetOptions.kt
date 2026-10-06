package mx.dev1.fintoc.sdk.domain.widget

import java.net.URI
import mx.dev1.fintoc.sdk.domain.security.SecretKeyGuard

/**
 * What the Fintoc Widget should do. Each subtype is one Fintoc product and carries only the settings that product
 * accepts, so an invalid combination cannot be expressed.
 *
 * Tokens come from **your backend** (it creates them with its secret key), never from the app. Every subtype hides its
 * tokens in `toString()` so they never reach logs or crash reports, and rejects values that look like secret keys.
 */
public sealed interface FintocWidgetOptions {

    /**
     * Pays with a bank transfer: SPEI in Mexico, bank transfer in Chile.
     *
     * @property sessionToken The `session_token` of a Checkout Session, which your backend creates with its secret key.
     * See Fintoc's API reference for the session settings that produce one. Mint a new one for every payment attempt.
     */
    public data class Payments(val sessionToken: String) : FintocWidgetOptions {
        init {
            requireValidToken(parameterName = "sessionToken", value = sessionToken)
        }

        override fun toString(): String = "Payments(sessionToken=<redacted>)"
    }

    /**
     * Connects a bank account to read its movements.
     *
     * @property holderType Whether the accounts belong to a person or a company.
     * @property country Country of the bank. Defaults to Chile, as Fintoc does.
     * @property linkToken Identifier of an existing link, to reconnect it instead of creating a new one.
     * @property webhookUrl HTTPS URL that Fintoc calls after the link is created.
     */
    public data class Movements(
        val holderType: FintocHolderType,
        val country: FintocCountry = FintocCountry.CHILE,
        val linkToken: String? = null,
        val webhookUrl: String? = null,
    ) : FintocWidgetOptions {
        init {
            linkToken?.let { token -> requireValidToken(parameterName = "linkToken", value = token) }
            webhookUrl?.let { url -> requireHttpsUrl(parameterName = "webhookUrl", value = url) }
        }

        override fun toString(): String = "Movements(holderType=$holderType, country=$country, " +
            "linkToken=${if (linkToken == null) "null" else "<redacted>"}, " +
            "webhookUrl=${if (webhookUrl == null) "null" else "<redacted>"})"
    }

    /**
     * Sets up a recurring payment method.
     *
     * @property widgetToken Token your backend creates to configure the Widget for subscriptions.
     * @property holderType Whether the account belongs to a person or a company.
     * @property country Country of the bank. Defaults to Chile, as Fintoc does.
     */
    public data class Subscriptions(
        val widgetToken: String,
        val holderType: FintocHolderType,
        val country: FintocCountry = FintocCountry.CHILE,
    ) : FintocWidgetOptions {
        init {
            requireValidToken(parameterName = "widgetToken", value = widgetToken)
        }

        override fun toString(): String =
            "Subscriptions(widgetToken=<redacted>, holderType=$holderType, country=$country)"
    }
}

private fun requireValidToken(parameterName: String, value: String) {
    require(value.isNotBlank()) { "$parameterName must not be blank." }
    require(value.none { character -> character.isWhitespace() }) { "$parameterName must not contain whitespace." }
    SecretKeyGuard.requireNotSecretKey(parameterName = parameterName, value = value)
}

private fun requireHttpsUrl(parameterName: String, value: String) {
    require(value.none { character -> character.isWhitespace() }) { "$parameterName must not contain whitespace." }
    val uri = runCatching { URI(value) }.getOrNull()
    require(uri != null && uri.scheme == "https" && !uri.host.isNullOrEmpty()) {
        "$parameterName must be an absolute https URL."
    }
}
