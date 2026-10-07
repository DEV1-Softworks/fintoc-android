package mx.dev1.fintoc.sample.backend

import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions

/**
 * Turns what was pasted into Payments options, or `null` when it cannot be a session token. The SDK does the checking:
 * it refuses blank values, values with spaces and secret keys, so the sample reuses that instead of repeating it.
 */
fun parsePaymentsOptions(pasted: String): FintocWidgetOptions.Payments? =
    runCatching { FintocWidgetOptions.Payments(sessionToken = pasted.trim()) }.getOrNull()
