package mx.dev1.fintoc.sdk.presentation.checkout

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import mx.dev1.fintoc.sdk.presentation.findActivity

/** Opens a hosted checkout in a Custom Tab. */
internal fun interface CustomTabLauncher {

    /** @return `false` when nothing on the device can open the address. */
    fun launch(url: String): Boolean
}

/**
 * Opens the address in the Custom Tab of the browser of the device, or in the browser itself when that browser has no
 * Custom Tabs. The address bar stays visible and never hides while scrolling, so the customer can always see which site
 * is asking for their payment details.
 */
internal class AndroidCustomTabLauncher(private val context: Context) : CustomTabLauncher {

    override fun launch(url: String): Boolean {
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setBookmarksButtonEnabled(false)
            .setDownloadButtonEnabled(false)
            .setInstantAppsEnabled(false)
            .build()
        if (context.findActivity() == null) customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            customTab.launchUrl(context, url.toUri())
            true
        } catch (noBrowser: ActivityNotFoundException) {
            // Nothing on this device can open the address. It is not logged: it may hold a token.
            false
        }
    }
}
