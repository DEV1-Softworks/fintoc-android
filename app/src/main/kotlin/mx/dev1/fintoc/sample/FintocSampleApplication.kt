package mx.dev1.fintoc.sample

import android.app.Application
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration

class FintocSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Fintoc.initialize(
            context = this,
            configuration = FintocConfiguration(authToken = SAMPLE_AUTH_TOKEN),
        )
    }

    private companion object {
        // Placeholder: the sample does not call the API yet. Never ship real credentials in an app.
        const val SAMPLE_AUTH_TOKEN = "sample-auth-token"
    }
}
