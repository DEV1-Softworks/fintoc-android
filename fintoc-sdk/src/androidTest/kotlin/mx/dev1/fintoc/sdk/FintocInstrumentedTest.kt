package mx.dev1.fintoc.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FintocInstrumentedTest {

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    @Test
    fun initializeBuildsTheDependencyGraphOnARealDevice() {
        val configuration = FintocConfiguration(authToken = "token")

        Fintoc.initialize(applicationContext, configuration)

        assertTrue(Fintoc.isInitialized)
        assertEquals(configuration, Fintoc.requireKoin().get<FintocConfiguration>())
        assertSame(applicationContext, Fintoc.requireKoin().get<Context>())
    }

    @Test
    fun shutdownReleasesTheSdk() {
        Fintoc.initialize(applicationContext, FintocConfiguration(authToken = "token"))

        Fintoc.shutdown()

        assertFalse(Fintoc.isInitialized)
    }
}
