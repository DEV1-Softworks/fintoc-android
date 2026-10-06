package mx.dev1.fintoc.sdk.di

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetRequests
import mx.dev1.fintoc.sdk.presentation.widget.AndroidExternalLinkLauncher
import mx.dev1.fintoc.sdk.presentation.widget.ExternalLinkLauncher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.parameter.parametersOf
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocKoinContainerTest {

    private val application: Context = ApplicationProvider.getApplicationContext()
    private val configuration = FintocConfiguration(publicKey = "pk_test_abc123")
    private val containers = mutableListOf<FintocKoinContainer>()

    @After
    fun closeContainers() {
        containers.forEach { container -> container.close() }
    }

    private fun container(context: Context = application): FintocKoinContainer =
        FintocKoinContainer(context, configuration).also { container -> containers += container }

    @Test
    fun `the container provides the configuration it was created with`() {
        assertSame(configuration, container().koin.get<FintocConfiguration>())
    }

    @Test
    fun `only the application context is kept, even when given an activity`() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        assertSame(activity.applicationContext, container(activity).koin.get<Context>())
    }

    @Test
    fun `the request registry is a single instance per container`() {
        val koin = container().koin

        assertSame(koin.get<FintocWidgetRequests>(), koin.get<FintocWidgetRequests>())
    }

    @Test
    fun `containers do not share their request registries`() {
        val first = container().koin.get<FintocWidgetRequests>()
        val second = container().koin.get<FintocWidgetRequests>()

        assertNotSame(first, second)
    }

    @Test
    fun `closing the container forgets the session tokens it held`() {
        val container = container()
        val registry = container.koin.get<FintocWidgetRequests>()
        val requestId = registry.register(FintocWidgetOptions.Payments(sessionToken = "cs_secret"))

        container.close()

        assertNull(registry.find(requestId))
    }

    @Test
    fun `closing one container leaves the registry of another alone`() {
        val closed = container()
        val open = container()
        val requestId = open.koin.get<FintocWidgetRequests>()
            .register(FintocWidgetOptions.Payments(sessionToken = "cs_secret"))

        closed.close()

        assertEquals(
            FintocWidgetOptions.Payments(sessionToken = "cs_secret"),
            open.koin.get<FintocWidgetRequests>().find(requestId),
        )
    }

    @Test
    fun `the link launcher is created for the context it is asked for`() {
        val koin = container().koin
        val screen: Context = mock()

        val launcher = koin.get<ExternalLinkLauncher> { parametersOf(screen) }
        launcher.open("https://files.example.com/voucher.pdf")

        assertTrue(launcher is AndroidExternalLinkLauncher)
        verify(screen).startActivity(any())
    }

    @Test
    fun `each request for a link launcher builds a new one`() {
        val koin = container().koin

        val first = koin.get<ExternalLinkLauncher> { parametersOf(application) }
        val second = koin.get<ExternalLinkLauncher> { parametersOf(application) }

        assertNotSame(first, second)
    }

    @Test
    fun `a link launcher cannot be built without the context it needs`() {
        val koin = container().koin

        assertThrows(Exception::class.java) { koin.get<ExternalLinkLauncher>() }
    }
}
