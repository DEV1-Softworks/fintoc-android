package mx.dev1.fintoc.sdk.di

import android.content.Context
import mx.dev1.fintoc.sdk.FintocConfiguration
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/**
 * Owns the dependency graph of the SDK.
 *
 * The graph lives in its own [KoinApplication] instead of the global Koin context, so it can
 * never clash with a Koin instance started by the host application.
 */
internal class FintocKoinContainer(
    applicationContext: Context,
    configuration: FintocConfiguration,
) {
    private val koinApplication: KoinApplication = koinApplication {
        modules(coreModule(applicationContext.applicationContext, configuration), widgetModule())
    }

    val koin: Koin
        get() = koinApplication.koin

    /** Releases everything the container owns, such as the session tokens of the screens that were prepared. */
    fun close() {
        koinApplication.close()
    }
}
