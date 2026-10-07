package mx.dev1.fintoc.sdk.di

import android.content.Context
import mx.dev1.fintoc.sdk.FintocConfiguration
import org.koin.dsl.module

/** What every part of the SDK needs: the application context and the settings the host initialized the SDK with. */
internal fun coreModule(applicationContext: Context, configuration: FintocConfiguration) = module {
    single<Context> { applicationContext }
    single { configuration }
}
