package mx.dev1.fintoc.sample

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.Fintoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocSampleApplicationTest {

    @Test
    fun `application initializes the sdk on startup`() {
        val application = ApplicationProvider.getApplicationContext<FintocSampleApplication>()

        assertEquals(FintocSampleApplication::class.java, application::class.java)
        assertTrue(Fintoc.isInitialized)
    }

    @Test
    fun `main activity launches`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertTrue(activity.hasWindowFocus() || !activity.isFinishing) }
        }
    }
}
