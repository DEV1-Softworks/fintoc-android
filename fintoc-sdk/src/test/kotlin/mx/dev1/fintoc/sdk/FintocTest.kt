package mx.dev1.fintoc.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetRequests
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocTest {

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    @Test
    fun `sdk is not initialized by default`() {
        assertFalse(Fintoc.isInitialized)
    }

    @Test
    fun `requireKoin fails with a clear message before initialization`() {
        val failure = assertThrows(IllegalStateException::class.java) { Fintoc.requireKoin() }

        assertTrue(failure.message.orEmpty().contains("Fintoc.initialize"))
    }

    @Test
    fun `the dependency graph is only there while the sdk is initialized`() {
        assertNull(Fintoc.koinOrNull())

        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))
        assertSame(Fintoc.requireKoin(), Fintoc.koinOrNull())

        Fintoc.shutdown()
        assertNull(Fintoc.koinOrNull())
    }

    @Test
    fun `shutdown forgets the session tokens of screens that were prepared`() {
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))
        val registry = Fintoc.requireKoin().get<FintocWidgetRequests>()
        val requestId = registry.register(FintocWidgetOptions.Payments(sessionToken = "cs_secret"))

        Fintoc.shutdown()

        assertNull(registry.find(requestId))
    }

    @Test
    fun `initializing again forgets the session tokens of screens that were prepared`() {
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))
        val requestId = Fintoc.requireKoin().get<FintocWidgetRequests>()
            .register(FintocWidgetOptions.Payments(sessionToken = "cs_secret"))

        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))

        assertNull(Fintoc.requireKoin().get<FintocWidgetRequests>().find(requestId))
    }

    @Test
    fun `initialize exposes the configuration through the dependency graph`() {
        val configuration = FintocConfiguration(publicKey = "pk_test_token")

        Fintoc.initialize(applicationContext, configuration)

        assertTrue(Fintoc.isInitialized)
        assertEquals(configuration, Fintoc.requireKoin().get<FintocConfiguration>())
    }

    @Test
    fun `initialize retains only the application context`() {
        val activityContext: Context = mock()
        whenever(activityContext.applicationContext).thenReturn(applicationContext)

        Fintoc.initialize(activityContext, FintocConfiguration(publicKey = "pk_test_token"))

        assertSame(applicationContext, Fintoc.requireKoin().get<Context>())
    }

    @Test
    fun `initialize again replaces the previous configuration`() {
        val replacement = FintocConfiguration(publicKey = "pk_test_replacement")
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_first"))

        Fintoc.initialize(applicationContext, replacement)

        assertEquals(replacement, Fintoc.requireKoin().get<FintocConfiguration>())
    }

    @Test
    fun `shutdown releases the sdk and can be repeated`() {
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))

        Fintoc.shutdown()
        Fintoc.shutdown()

        assertFalse(Fintoc.isInitialized)
    }

    @Test
    fun `dependency graph is isolated from the global koin context`() {
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_token"))

        assertNull(GlobalContext.getOrNull())
    }
}
