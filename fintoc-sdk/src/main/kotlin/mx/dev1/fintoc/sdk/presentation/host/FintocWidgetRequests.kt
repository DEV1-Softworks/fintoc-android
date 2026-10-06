package mx.dev1.fintoc.sdk.presentation.host

import java.util.UUID
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions

/**
 * Keeps the options of the screens that are about to open, or open, in memory.
 *
 * The options hold session tokens, so they never travel inside an `Intent`: the intent only carries the random
 * identifier of an entry. An Activity that the system restores after the process died finds nothing here, and closes.
 */
internal object FintocWidgetRequests {

    /** Only the last few are kept, so a screen that was prepared but never opened cannot hold a token for long. */
    private const val MAX_PENDING_REQUESTS = 4

    private val pending = LinkedHashMap<String, FintocWidgetOptions>()

    @Synchronized
    fun register(options: FintocWidgetOptions): String {
        while (pending.size >= MAX_PENDING_REQUESTS) {
            pending.remove(pending.keys.first())
        }
        val requestId = UUID.randomUUID().toString()
        pending[requestId] = options
        return requestId
    }

    @Synchronized
    fun find(requestId: String): FintocWidgetOptions? = pending[requestId]

    @Synchronized
    fun remove(requestId: String) {
        pending.remove(requestId)
    }

    @Synchronized
    fun clear() {
        pending.clear()
    }
}
