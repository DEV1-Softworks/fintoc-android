package mx.dev1.fintoc.sdk.domain.widget

/** What the WebView must do with an address the Widget page wants to navigate to. */
internal sealed interface FintocWidgetNavigation {

    /**
     * A `fintocwidget://` redirect. The WebView never opens it. [event] is `null` when the parser ignored the redirect.
     */
    data class Redirect(val event: FintocWidgetEvent?) : FintocWidgetNavigation

    /** Let the WebView load the address itself. */
    data object InsideWidget : FintocWidgetNavigation

    /** Hand the address to the system browser. The address may hold tokens, so `toString()` hides it. */
    data class OutsideWidget(val url: String) : FintocWidgetNavigation {
        override fun toString(): String = "OutsideWidget(url=<redacted>)"
    }

    /** Do nothing: the address is not something the Widget needs. */
    data object Blocked : FintocWidgetNavigation
}
