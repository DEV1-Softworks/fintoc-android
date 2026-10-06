package mx.dev1.fintoc.sdk.domain.widget

/**
 * Widget events that Fintoc documents. Fintoc can add events at any time, so [FintocWidgetEvent.Occurred] always
 * carries the raw name too and [FintocWidgetEvent.Occurred.type] is `null` for a name this SDK does not know yet.
 *
 * @property code Event name exactly as the Widget sends it.
 */
public enum class FintocWidgetEventType(public val code: String) {
    /** The user opened the Widget. */
    OPENED("opened"),

    /** The user visited the terms and conditions. */
    ON_TERMS_AND_CONDITIONS("on_terms_and_conditions"),

    /** The user visited the Cobro Digital (CoDi) helper guide. */
    CODI_HELPER("codi_helper"),

    /** The user opened the financial institution selection view. */
    ON_AVAILABLE_INSTITUTIONS("on_available_institutions"),

    /** The user opened the view to request a new bank. */
    ON_REQUEST_BANK("on_request_bank"),

    /** The user opened the financial institution authentication view. */
    ON_AUTHENTICATION_FORM("on_authentication_form"),

    /** The user must enter their mobile phone number to receive a CoDi payment request. */
    CODI_USERNAME_REQUIRED("codi_username_required"),

    /** The user must select the payment provider. */
    PAYMENT_IN_PROGRESS("payment_in_progress"),

    /** The institution is verifying the credentials and Fintoc is connecting the account. */
    CREATING_LINK("creating_link"),

    /** The institution approved the credentials and Fintoc created a link. */
    LINK_CREATED("link_created"),

    /** The user opened the account selection view. */
    SELECTING_ACCOUNT("selecting_account"),

    /** The user must authorize the payment. */
    PAYMENT_INTENT_CONFIRMATION_REQUIRED("payment_intent_confirmation_required"),

    /** The institution requires a second authentication factor through an app. */
    APP_AUTHENTICATION_REQUIRED("app_authentication_required"),

    /** The user started creating a subscription. */
    CREATING_SUBSCRIPTION("creating_subscription"),

    /** The user is viewing the direct transfer screen. */
    PAYMENT_DIRECT_TRANSFER("payment_direct_transfer"),

    /** The institution requires a second authentication factor through a physical device. */
    DEVICE_AUTHENTICATION_REQUIRED("device_authentication_required"),

    /** The institution requires a second authentication factor through a coordinate card. */
    CARD_AUTHENTICATION_REQUIRED("card_authentication_required"),

    /** The institution requires a second authentication factor by text message. */
    SMS_AUTHENTICATION_REQUIRED("sms_authentication_required"),

    /** The institution requires a second authentication factor by email. */
    EMAIL_AUTHENTICATION_REQUIRED("email_authentication_required"),

    /** The institution requires a second authentication factor through a captcha. */
    CAPTCHA_AUTHENTICATION_REQUIRED("captcha_authentication_required"),

    /** The institution is validating the second factor the user entered. */
    VALIDATING_SECOND_FACTOR("validating_second_factor"),

    /** Fintoc created a subscription. */
    SUBSCRIPTION_CREATED("subscription_created"),

    /** A constraint prevented Fintoc from completing the subscription, for example one that already exists. */
    SUBSCRIPTION_ABORTED("subscription_aborted"),

    /** A payment succeeded. Not proof of payment: confirm it with Fintoc webhooks on your backend. */
    PAYMENT_CREATED("payment_created"),

    /** A payment failed. */
    PAYMENT_ERROR("payment_error"),

    /** The user is selecting a second-factor authentication method. */
    PAYMENT_SELECTING_AUTH("payment_selecting_auth"),

    /** Fintoc is validating the payment. */
    VALIDATING_PAYMENT("validating_payment"),

    /** The user closed the Widget. */
    CLOSED("closed"),

    /** An error occurred in the Widget flow. */
    ON_ERROR("on_error"),
    ;

    internal companion object {
        private val typesByCode: Map<String, FintocWidgetEventType> = entries.associateBy { type -> type.code }

        fun fromCode(code: String): FintocWidgetEventType? = typesByCode[code]
    }
}
