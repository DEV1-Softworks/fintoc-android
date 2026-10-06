package mx.dev1.fintoc.sdk.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity behind this context, which Compose and themes may wrap in other contexts, or `null` if there is none. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
