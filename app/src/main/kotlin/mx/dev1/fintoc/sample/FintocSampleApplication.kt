package mx.dev1.fintoc.sample

import android.app.Application
import mx.dev1.fintoc.sample.config.SampleSdk
import mx.dev1.fintoc.sample.config.SampleSettings

class FintocSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SampleSdk.initialize(context = this, settings = SampleSettings.fromBuildConfig())
    }
}
