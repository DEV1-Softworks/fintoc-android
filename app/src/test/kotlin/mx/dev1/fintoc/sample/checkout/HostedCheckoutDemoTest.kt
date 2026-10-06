package mx.dev1.fintoc.sample.checkout

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sample.config.SampleSdk
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class HostedCheckoutDemoTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun initializeSdk() {
        SampleSdk.initialize(application, SampleSettings("pk_test_abc123"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    @Test
    fun `the return addresses use the apps own scheme and carry the secret value`() {
        val demo = HostedCheckoutDemo(secretValue = "abc123")

        assertEquals("fintocsample://checkout/success?n=abc123", demo.successUrl)
        assertEquals("fintocsample://checkout/cancel?n=abc123", demo.cancelUrl)
    }

    @Test
    fun `every demo gets a new secret value`() {
        assertNotEquals(HostedCheckoutDemo().secretValue, HostedCheckoutDemo().secretValue)
    }

    @Test
    fun `its own addresses are recognized and a forged one is not`() {
        val demo = HostedCheckoutDemo(secretValue = "abc123")

        assertEquals(FintocHostedCheckoutOutcome.Succeeded, demo.checkout.outcomeOf(Uri.parse(demo.successUrl)))
        assertEquals(FintocHostedCheckoutOutcome.Cancelled, demo.checkout.outcomeOf(Uri.parse(demo.cancelUrl)))
        assertEquals(
            FintocHostedCheckoutOutcome.Unrelated,
            demo.checkout.outcomeOf(Uri.parse("fintocsample://checkout/success?n=guess")),
        )
    }

    @Test
    fun `a fintoc address opens in a browser`() {
        val result = HostedCheckoutDemo().open(application, "  https://pay.fintoc.com/checkout/cs_1 ")

        assertEquals(HostedOpenResult.OPENED, result)
        assertEquals("https://pay.fintoc.com/checkout/cs_1", shadowOf(application).nextStartedActivity.dataString)
    }

    @Test
    fun `an address that is not on fintoc is refused and nothing opens`() {
        val result = HostedCheckoutDemo().open(application, "https://evil.example/checkout/cs_1")

        assertEquals(HostedOpenResult.NOT_TRUSTED, result)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun `a device with no browser is reported`() {
        val context: Context = mock { on { startActivity(any(), anyOrNull()) } doThrow ActivityNotFoundException() }

        assertEquals(HostedOpenResult.NO_BROWSER, HostedCheckoutDemo().open(context, "https://pay.fintoc.com/c/cs_1"))
    }

    @Test
    fun `the scheme and host match the ones the manifest declares`() {
        val declared = application.packageManager.getPackageInfo(
            application.packageName,
            android.content.pm.PackageManager.GET_ACTIVITIES,
        )

        assertTrue(declared.activities.orEmpty().any { it.name.endsWith("MainActivity") })
        assertEquals("fintocsample", HostedCheckoutDemo.SCHEME)
        assertEquals("checkout", HostedCheckoutDemo.HOST)
    }
}
