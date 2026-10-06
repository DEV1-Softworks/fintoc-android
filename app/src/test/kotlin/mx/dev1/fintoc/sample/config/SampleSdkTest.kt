package mx.dev1.fintoc.sample.config

import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocLanguage
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SampleSdkTest {

    private val settings = SampleSettings("pk_test_abc123")

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    @Test
    fun `the sdk is initialized with the key of the settings and follows the device language by default`() {
        Fintoc.shutdown()

        SampleSdk.initialize(ApplicationProvider.getApplicationContext(), settings)

        assertTrue(Fintoc.isInitialized)
    }

    @Test
    fun `a language can be chosen and changed by initializing again`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        SampleSdk.initialize(context, settings, FintocLanguage.SPANISH)
        SampleSdk.initialize(context, settings, FintocLanguage.FRENCH)
        SampleSdk.initialize(context, settings, language = null)

        assertTrue(Fintoc.isInitialized)
    }

    @Test
    fun `shutting down leaves the sdk uninitialized`() {
        SampleSdk.initialize(ApplicationProvider.getApplicationContext(), settings)
        Fintoc.shutdown()

        assertFalse(Fintoc.isInitialized)
    }
}
