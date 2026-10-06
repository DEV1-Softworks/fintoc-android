package mx.dev1.fintoc.sdk.presentation.localization

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import mx.dev1.fintoc.sdk.FintocLanguage

/**
 * The SDK's own texts, in the language the host asked for or, when it asked for none, in the language of the device.
 *
 * Only the SDK's texts go through this. The composition's `Context` stays untouched, because the WebView must keep
 * the real Activity.
 */
internal class FintocStrings(private val resources: Resources) {

    fun get(@StringRes stringId: Int): String = resources.getString(stringId)
}

/**
 * Resources in [language], keeping everything else of this context's configuration, such as the font scale.
 *
 * Google Play only delivers the languages of the user's device to an app published as an Android App Bundle, unless the
 * host turns off language splits. That is the host's build setting, so it is documented rather than handled here.
 */
@SuppressLint("AppBundleLocaleChanges")
internal fun Context.resourcesIn(language: FintocLanguage?): Resources {
    if (language == null) return resources

    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.languageTag))
    }
    return createConfigurationContext(configuration).resources
}

/** Looks the texts up again when the device configuration changes, for example its language or font scale. */
@Composable
internal fun rememberFintocStrings(language: FintocLanguage?): FintocStrings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration, language) { FintocStrings(context.resourcesIn(language)) }
}
