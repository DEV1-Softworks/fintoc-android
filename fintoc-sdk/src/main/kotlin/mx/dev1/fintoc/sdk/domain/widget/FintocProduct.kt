package mx.dev1.fintoc.sdk.domain.widget

/** Fintoc products the Widget can run. Derived from the [FintocWidgetOptions] subtype, never chosen directly. */
internal enum class FintocProduct(val code: String) {
    PAYMENTS("payments"),
    MOVEMENTS("movements"),
    SUBSCRIPTIONS("subscriptions"),
}

/** Product the Widget runs for these options. */
internal val FintocWidgetOptions.product: FintocProduct
    get() = when (this) {
        is FintocWidgetOptions.Payments -> FintocProduct.PAYMENTS
        is FintocWidgetOptions.Movements -> FintocProduct.MOVEMENTS
        is FintocWidgetOptions.Subscriptions -> FintocProduct.SUBSCRIPTIONS
    }
