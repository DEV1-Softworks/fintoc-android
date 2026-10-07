package mx.dev1.fintoc.sdk.presentation.widget

/** Where the Widget page is in its life, as far as the WebView can tell. */
internal enum class FintocWidgetLoadState {
    /** The page is being fetched. */
    LOADING,

    /** The page finished loading. The Widget itself keeps running inside it. */
    LOADED,

    /** The page could not be loaded, or the WebView's renderer died. */
    FAILED,

    /** The device cannot create a WebView at all, usually because no WebView provider is installed or enabled. */
    UNAVAILABLE,
}
