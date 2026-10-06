package mx.dev1.fintoc.sdk.di

import android.content.Context
import mx.dev1.fintoc.sdk.presentation.checkout.AndroidCustomTabLauncher
import mx.dev1.fintoc.sdk.presentation.checkout.CustomTabLauncher
import org.koin.dsl.module

/** What the hosted checkout needs from Android. */
internal fun checkoutModule() = module {
    // Needs the screen the customer is on: pass it with `parametersOf(context)`.
    factory<CustomTabLauncher> { parameters -> AndroidCustomTabLauncher(parameters.get<Context>()) }
}
