package mx.dev1.fintoc.sdk.presentation.widget

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions

/** Where the session token that the host's backend provides is on its way to the Widget. */
internal sealed interface FintocSessionTokenState {
    data object Loading : FintocSessionTokenState

    data class Ready(val options: FintocWidgetOptions.Payments) : FintocSessionTokenState

    data object Failed : FintocSessionTokenState
}

/**
 * Asks the host for a session token and turns it into Widget options.
 *
 * A failure of the provider, which is host code that usually calls a backend, becomes [FintocSessionTokenState.Failed]
 * so the user can try again. Cancellation is never swallowed. Logs name the kind of failure but never repeat a message
 * from the provider or the token, because either may hold secrets.
 */
internal suspend fun loadPaymentsOptions(sessionTokenProvider: suspend () -> String): FintocSessionTokenState {
    val sessionToken = try {
        sessionTokenProvider()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Log.w(LOG_TAG, "The session token provider failed with ${failure.javaClass.name}.")
        return FintocSessionTokenState.Failed
    }

    return try {
        FintocSessionTokenState.Ready(FintocWidgetOptions.Payments(sessionToken))
    } catch (invalidToken: IllegalArgumentException) {
        // The message of these checks never repeats the rejected value.
        Log.w(LOG_TAG, "The session token provider returned an unusable token: ${invalidToken.message}")
        FintocSessionTokenState.Failed
    }
}

private const val LOG_TAG = "FintocWidget"
