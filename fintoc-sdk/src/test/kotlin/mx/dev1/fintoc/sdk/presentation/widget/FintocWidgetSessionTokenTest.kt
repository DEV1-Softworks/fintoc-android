package mx.dev1.fintoc.sdk.presentation.widget

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocWidgetSessionTokenTest {

    @Test
    fun `a valid token becomes payments options`() = runTest {
        val state = loadPaymentsOptions { "cs_123_sec_456" }

        assertEquals(FintocSessionTokenState.Ready(FintocWidgetOptions.Payments("cs_123_sec_456")), state)
    }

    @Test
    fun `a provider that throws fails without crashing`() = runTest {
        val state = loadPaymentsOptions { throw IOException("backend down") }

        assertEquals(FintocSessionTokenState.Failed, state)
    }

    @Test
    fun `blank and secret key tokens fail`() = runTest {
        listOf("", "   ", "has space", "sk_live_secret").forEach { token ->
            assertEquals(token, FintocSessionTokenState.Failed, loadPaymentsOptions { token })
        }
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { loadPaymentsOptions { throw CancellationException("cancelled") } }
        }
    }

    @Test
    fun `ready state hides the token in toString`() {
        val state = FintocSessionTokenState.Ready(FintocWidgetOptions.Payments("cs_123_sec_456"))

        assertFalse(state.toString().contains("cs_123_sec_456"))
    }
}
