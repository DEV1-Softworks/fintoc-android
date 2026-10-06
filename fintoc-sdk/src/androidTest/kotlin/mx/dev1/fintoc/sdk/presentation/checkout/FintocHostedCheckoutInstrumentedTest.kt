package mx.dev1.fintoc.sdk.presentation.checkout

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens a hosted checkout on a real device, so a browser has to come to the foreground. The address is a made-up
 * session of the real `pay.fintoc.com`: only what Android does is checked, not what the page shows, so the test gives
 * the same answer whether or not the page loads.
 */
@RunWith(AndroidJUnit4::class)
class FintocHostedCheckoutInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val checkout = FintocHostedCheckout(
        successUrl = "https://merchant.example/pay/success?n=secret123",
        cancelUrl = "https://merchant.example/pay/cancel?n=secret123",
    )

    @Before
    fun initializeSdk() {
        Fintoc.initialize(context, FintocConfiguration(publicKey = "pk_test_instrumented_tests"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader -> reader.readText() }
    }

    /** Package of the app that has the screen right now, or an empty text when it cannot be read. */
    private fun foregroundPackage(): String =
        Regex("topResumedActivity=ActivityRecord\\{[^ ]+ u\\d+ ([^/ ]+)/")
            .find(shell("dumpsys activity activities"))?.groupValues?.get(1).orEmpty()

    private fun waitUntil(timeoutMillis: Long = WAIT_MILLIS, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue("Timed out waiting", System.currentTimeMillis() < deadline)
            Thread.sleep(POLL_MILLIS)
        }
    }

    @Test
    fun aHostedCheckoutBringsABrowserToTheForegroundAndBackReturnsToTheApp() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            waitUntil { foregroundPackage() == context.packageName }
            var opened = false
            scenario.onActivity { activity ->
                opened = checkout.open(activity, "https://pay.fintoc.com/checkout/cs_instrumented_test")
            }
            assertTrue("A device with a browser must be able to open it", opened)

            waitUntil { foregroundPackage() !in setOf("", context.packageName) }
            assertNotEquals("The app must have given the screen to the browser", context.packageName, foregroundPackage())

            shell("input keyevent KEYCODE_BACK")
            waitUntil { scenario.state == Lifecycle.State.RESUMED && foregroundPackage() == context.packageName }
        }
    }

    @Test
    fun anAddressThatReachedTheAppIsMatchedAgainstTheConfiguredOnes() {
        assertEquals(
            FintocHostedCheckoutOutcome.Succeeded,
            checkout.outcomeOf(Uri.parse("https://merchant.example/pay/success?n=secret123&cs=cs_1")),
        )
        assertEquals(
            FintocHostedCheckoutOutcome.Cancelled,
            checkout.outcomeOf(Uri.parse("https://merchant.example/pay/cancel?n=secret123")),
        )
        assertEquals(
            FintocHostedCheckoutOutcome.Unrelated,
            checkout.outcomeOf(Uri.parse("https://merchant.example/pay/success?n=guess")),
        )
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
        const val POLL_MILLIS = 250L
    }
}
