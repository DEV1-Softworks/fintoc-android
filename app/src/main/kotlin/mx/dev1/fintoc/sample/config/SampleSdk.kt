package mx.dev1.fintoc.sample.config

import android.content.Context
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.FintocLanguage

/** The only place where the sample configures the SDK, at startup and when the language picker changes. */
object SampleSdk {

    /** @param language The language for the SDK's own texts, or `null` to follow the device. */
    fun initialize(context: Context, settings: SampleSettings, language: FintocLanguage? = null) {
        Fintoc.initialize(
            context = context,
            configuration = FintocConfiguration(publicKey = settings.publicKey, language = language),
        )
    }
}
