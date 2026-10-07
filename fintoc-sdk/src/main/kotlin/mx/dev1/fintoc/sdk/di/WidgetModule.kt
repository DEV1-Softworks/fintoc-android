package mx.dev1.fintoc.sdk.di

import android.content.Context
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetRequests
import mx.dev1.fintoc.sdk.presentation.widget.AndroidExternalLinkLauncher
import mx.dev1.fintoc.sdk.presentation.widget.ExternalLinkLauncher
import mx.dev1.fintoc.sdk.presentation.widget.FintocWebViewFactory
import mx.dev1.fintoc.sdk.presentation.widget.PlatformWebViewFactory
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * What the Widget needs that has state or depends on Android. Pure functions, such as the URL builder, the navigation
 * policy and the redirect parser, stay plain objects: there is nothing to inject into them or to release.
 */
internal fun widgetModule() = module {
    // Holds session tokens, so it lives and dies with the container instead of with the process.
    single { FintocWidgetRequests() } onClose { requests -> requests?.clear() }

    // Needs the context the Widget is shown in: pass it with `parametersOf(context)`.
    factory<ExternalLinkLauncher> { parameters -> AndroidExternalLinkLauncher(parameters.get<Context>()) }

    // Android decides whether a WebView can be created at all, so tests replace this one to make creation fail.
    factory<FintocWebViewFactory> { PlatformWebViewFactory }
}
