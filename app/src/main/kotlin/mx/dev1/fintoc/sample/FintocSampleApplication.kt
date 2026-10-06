package mx.dev1.fintoc.sample

import android.app.Application
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration

class FintocSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Fintoc.initialize(
            context = this,
            configuration = FintocConfiguration(publicKey = SAMPLE_PUBLIC_KEY),
        )
    }

    private companion object {
        // Placeholder. Use your own pk_test_ key from the Fintoc dashboard. Public keys are safe to ship in an app;
        // secret keys (sk_) are not, and the SDK rejects them.
        const val SAMPLE_PUBLIC_KEY = "pk_test_sample_public_key"
    }
}
