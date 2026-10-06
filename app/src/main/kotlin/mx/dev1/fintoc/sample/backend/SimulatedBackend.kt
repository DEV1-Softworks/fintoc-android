package mx.dev1.fintoc.sample.backend

import java.io.IOException
import kotlinx.coroutines.delay

/**
 * Stands in for your backend, which is the only place that can create a Fintoc session token because it needs your
 * secret key. This one hands back a token that was pasted into the sample, after a pause, and can fail the first time
 * to show how the SDK reacts to a provider that throws.
 */
class SimulatedBackend(
    private val sessionToken: String,
    private val delayMillis: Long = DEFAULT_DELAY_MILLIS,
    private val failFirstAttempt: Boolean = false,
) {
    private var attempts = 0

    suspend fun createSessionToken(): String {
        attempts += 1
        delay(delayMillis)
        if (failFirstAttempt && attempts == 1) throw IOException("Simulated backend failure.")
        return sessionToken
    }

    private companion object {
        const val DEFAULT_DELAY_MILLIS = 1_500L
    }
}
