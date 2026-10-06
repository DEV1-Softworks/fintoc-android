package mx.dev1.fintoc.sdk

import android.content.Context
import mx.dev1.fintoc.sdk.di.FintocKoinContainer
import org.koin.core.Koin

/**
 * Entry point of the Fintoc SDK.
 *
 * Call [initialize] once, typically from `Application.onCreate`, before using any other SDK API.
 */
public object Fintoc {
    private val containerLock = Any()

    @Volatile
    private var container: FintocKoinContainer? = null

    /** `true` once [initialize] has been called and [shutdown] has not been called since. */
    public val isInitialized: Boolean
        get() = container != null

    /**
     * Configures the SDK. Calling it again replaces the previous configuration.
     *
     * @param context Any context; only its application context is retained.
     * @param configuration Settings used by the SDK.
     */
    public fun initialize(context: Context, configuration: FintocConfiguration) {
        synchronized(containerLock) {
            container?.close()
            container = FintocKoinContainer(context, configuration)
        }
    }

    /** Releases every resource held by the SDK. It is safe to call when not initialized. */
    public fun shutdown() {
        synchronized(containerLock) {
            container?.close()
            container = null
        }
    }

    /** Dependency graph of the SDK. Fails fast when the host forgot to call [initialize]. */
    internal fun requireKoin(): Koin {
        val currentContainer = container
        check(currentContainer != null) {
            "Fintoc SDK is not initialized. Call Fintoc.initialize(context, configuration) first."
        }
        return currentContainer.koin
    }
}
