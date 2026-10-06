package mx.dev1.fintoc.sdk.presentation.checkout

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FintocHostedCheckoutTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val checkout = FintocHostedCheckout(
        successUrl = "https://merchant.com/pay/success?n=secret123",
        cancelUrl = "https://merchant.com/pay/cancel?n=secret123",
    )

    @Before
    fun initializeSdk() {
        Fintoc.initialize(application, FintocConfiguration(publicKey = "pk_test_abc123"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    private fun startedIntent(): Intent = shadowOf(application).nextStartedActivity

    @Test
    fun `the success address means success`() {
        assertEquals(
            FintocHostedCheckoutOutcome.Succeeded,
            checkout.outcomeOf(Uri.parse("https://merchant.com/pay/success?n=secret123&session=cs_1")),
        )
    }

    @Test
    fun `the cancel address means cancelled`() {
        assertEquals(
            FintocHostedCheckoutOutcome.Cancelled,
            checkout.outcomeOf(Uri.parse("https://merchant.com/pay/cancel?n=secret123")),
        )
    }

    @Test
    fun `an intent carries its address in its data`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://merchant.com/pay/success?n=secret123"))

        assertEquals(FintocHostedCheckoutOutcome.Succeeded, checkout.outcomeOf(intent))
    }

    @Test
    fun `a forged return without the secret value is not acted on`() {
        assertEquals(
            FintocHostedCheckoutOutcome.Unrelated,
            checkout.outcomeOf(Uri.parse("https://merchant.com/pay/success")),
        )
        assertEquals(
            FintocHostedCheckoutOutcome.Unrelated,
            checkout.outcomeOf(Uri.parse("https://merchant.com/pay/success?n=guess")),
        )
    }

    @Test
    fun `other addresses, no address and no intent are unrelated`() {
        assertEquals(FintocHostedCheckoutOutcome.Unrelated, checkout.outcomeOf(Uri.parse("https://other.com/x")))
        assertEquals(FintocHostedCheckoutOutcome.Unrelated, checkout.outcomeOf(null as Uri?))
        assertEquals(FintocHostedCheckoutOutcome.Unrelated, checkout.outcomeOf(null as Intent?))
        assertEquals(FintocHostedCheckoutOutcome.Unrelated, checkout.outcomeOf(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `an address that satisfies both is ambiguous and not acted on`() {
        val ambiguous = FintocHostedCheckout("https://merchant.com/pay?a=1", "https://merchant.com/pay?b=2")

        assertEquals(
            FintocHostedCheckoutOutcome.Unrelated,
            ambiguous.outcomeOf(Uri.parse("https://merchant.com/pay?a=1&b=2")),
        )
        assertEquals(
            FintocHostedCheckoutOutcome.Succeeded,
            ambiguous.outcomeOf(Uri.parse("https://merchant.com/pay?a=1")),
        )
        assertEquals(
            FintocHostedCheckoutOutcome.Cancelled,
            ambiguous.outcomeOf(Uri.parse("https://merchant.com/pay?b=2")),
        )
    }

    @Test
    fun `outcomes can be read without the sdk being initialized`() {
        Fintoc.shutdown()

        assertEquals(
            FintocHostedCheckoutOutcome.Succeeded,
            checkout.outcomeOf(Uri.parse("https://merchant.com/pay/success?n=secret123")),
        )
    }

    @Test
    fun `unusable addresses are refused when the checkout is created`() {
        assertThrows(IllegalArgumentException::class.java) { FintocHostedCheckout("", "https://merchant.com/c") }
        assertThrows(IllegalArgumentException::class.java) { FintocHostedCheckout("https://merchant.com/s", "") }
        assertThrows(IllegalArgumentException::class.java) {
            FintocHostedCheckout("http://merchant.com/s", "https://merchant.com/c")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FintocHostedCheckout("https://merchant.com/s", "javascript:alert(1)")
        }
    }

    @Test
    fun `the message names the address that is wrong and never repeats it`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            FintocHostedCheckout("https://merchant.com/s", "http://merchant.com/c?n=super-secret-value")
        }

        assertTrue(failure.message.orEmpty().contains("cancelUrl"))
        assertFalse(failure.message.orEmpty().contains("super-secret-value"))
    }

    @Test
    fun `two addresses that cannot be told apart are refused`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            FintocHostedCheckout("https://merchant.com/pay/done", "https://merchant.com/pay/done?status=cancel")
        }

        assertTrue(failure.message.orEmpty().contains("told apart"))
        assertThrows(IllegalArgumentException::class.java) {
            FintocHostedCheckout("https://merchant.com/pay/done", "https://merchant.com/pay/done")
        }
    }

    @Test
    fun `toString hides both addresses`() {
        val description = checkout.toString()

        assertFalse(description.contains("secret123"))
        assertFalse(description.contains("merchant.com"))
    }

    @Test
    fun `a hosted checkout opens as a custom tab with the address bar kept visible`() {
        val opened = checkout.open(application, "https://pay.fintoc.com/checkout/cs_li5531onlFDi235")

        assertTrue(opened)
        val intent = startedIntent()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://pay.fintoc.com/checkout/cs_li5531onlFDi235", intent.dataString)
        assertEquals(false, intent.getBooleanExtra(CustomTabsIntent.EXTRA_ENABLE_URLBAR_HIDING, true))
        assertEquals(
            CustomTabsIntent.SHARE_STATE_OFF,
            intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, CustomTabsIntent.SHARE_STATE_DEFAULT),
        )
        assertEquals(
            CustomTabsIntent.SHOW_PAGE_TITLE,
            intent.getIntExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE),
        )
    }

    @Test
    fun `a context that is not an activity starts a new task, and an activity keeps the current one`() {
        checkout.open(application, "https://pay.fintoc.com/checkout/cs_1")
        assertTrue(startedIntent().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        checkout.open(activity, "https://pay.fintoc.com/checkout/cs_1")
        assertEquals(0, shadowOf(activity).nextStartedActivity.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `an address that is not on fintoc is refused and nothing is opened`() {
        listOf(
            "https://evil.example/checkout/cs_1",
            "https://pay.fintoc.com.evil.example/checkout/cs_1",
            "http://pay.fintoc.com/checkout/cs_1",
            "javascript:alert(1)",
            "",
        ).forEach { url ->
            val failure = assertThrows(url, IllegalArgumentException::class.java) { checkout.open(application, url) }
            assertTrue(failure.message.orEmpty().contains("redirectUrl"))
        }

        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun `opening before initializing the sdk fails with a clear message`() {
        Fintoc.shutdown()

        val failure = assertThrows(IllegalStateException::class.java) {
            checkout.open(application, "https://pay.fintoc.com/checkout/cs_1")
        }

        assertTrue(failure.message.orEmpty().contains("Fintoc.initialize"))
    }

    @Test
    fun `a device with no browser reports that nothing was opened`() {
        val context: Context = mock { on { startActivity(any(), anyOrNull()) } doThrow ActivityNotFoundException() }

        assertFalse(checkout.open(context, "https://pay.fintoc.com/checkout/cs_1"))
    }
}
