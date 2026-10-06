package mx.dev1.fintoc.sdk.presentation.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetNavigationPolicy

/** Opens an address outside the Widget, in the system browser. */
internal fun interface ExternalLinkLauncher {
    fun open(url: String)
}

/**
 * Opens `https` addresses with a `VIEW` intent. Anything else is dropped, and so is an address nothing on the device
 * can open, because the Widget cannot do anything useful with that failure.
 */
internal class AndroidExternalLinkLauncher(private val context: Context) : ExternalLinkLauncher {

    override fun open(url: String) {
        if (!FintocWidgetNavigationPolicy.isOpenableOutsideWidget(url)) return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (noBrowser: ActivityNotFoundException) {
            // Nothing on this device can open the address. The message is not logged: the address may hold a token.
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
