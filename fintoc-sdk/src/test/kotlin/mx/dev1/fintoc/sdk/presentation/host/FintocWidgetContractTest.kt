package mx.dev1.fintoc.sdk.presentation.host

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.widget.FintocLinkIntentResult
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocWidgetContractTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contract = FintocWidgetContract()
    private val options = FintocWidgetOptions.Payments(sessionToken = "cs_secret_session_token")

    @Before
    fun initializeSdk() {
        Fintoc.initialize(context, FintocConfiguration(publicKey = "pk_test_abc123"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    @Test
    fun `the intent opens the widget screen of this sdk`() {
        val intent = contract.createIntent(context, options)

        assertEquals(FintocWidgetActivity::class.java.name, intent.component?.className)
        assertEquals(context.packageName, intent.component?.packageName)
    }

    @Test
    fun `the intent carries a request identifier and never the session token`() {
        val intent = contract.createIntent(context, options)

        val requestId = intent.getStringExtra(FintocWidgetContract.EXTRA_REQUEST_ID)
        assertEquals(options, Fintoc.requireKoin().get<FintocWidgetRequests>().find(requestId.orEmpty()))
        assertEquals(setOf(FintocWidgetContract.EXTRA_REQUEST_ID), intent.extras?.keySet())
        assertFalse(intent.toString().contains("cs_secret_session_token"))
        assertFalse(intent.extras.toString().contains("cs_secret_session_token"))
    }

    @Test
    fun `preparing a screen before initializing the sdk fails with a clear message`() {
        Fintoc.shutdown()

        val failure = assertThrows(IllegalStateException::class.java) { contract.createIntent(context, options) }

        assertTrue(failure.message.orEmpty().contains("Fintoc.initialize"))
    }

    @Test
    fun `a successful result without a link intent is a plain success`() {
        assertEquals(
            FintocWidgetResult.Succeeded(linkIntent = null),
            contract.parseResult(Activity.RESULT_OK, Intent()),
        )
        assertEquals(FintocWidgetResult.Succeeded(linkIntent = null), contract.parseResult(Activity.RESULT_OK, null))
    }

    @Test
    fun `a successful result carries the link intent through the result intent`() {
        val resultIntent = FintocWidgetContract.resultIntent(FintocLinkIntentResult("et_123", id = "li_456"))

        assertEquals(
            FintocWidgetResult.Succeeded(FintocLinkIntentResult(exchangeToken = "et_123", id = "li_456")),
            contract.parseResult(Activity.RESULT_OK, resultIntent),
        )
    }

    @Test
    fun `a link intent without an id survives the round trip`() {
        val resultIntent = FintocWidgetContract.resultIntent(FintocLinkIntentResult("et_123"))

        assertNull((contract.parseResult(Activity.RESULT_OK, resultIntent) as FintocWidgetResult.Succeeded)
            .linkIntent?.id)
    }

    @Test
    fun `a blank exchange token in the result is ignored`() {
        val resultIntent = Intent().putExtra(FintocWidgetContract.EXTRA_EXCHANGE_TOKEN, "  ")

        assertEquals(FintocWidgetResult.Succeeded(null), contract.parseResult(Activity.RESULT_OK, resultIntent))
    }

    @Test
    fun `anything but ok is an exit`() {
        assertEquals(FintocWidgetResult.Exited, contract.parseResult(Activity.RESULT_CANCELED, null))
        assertEquals(FintocWidgetResult.Exited, contract.parseResult(Activity.RESULT_FIRST_USER, Intent()))
    }
}
