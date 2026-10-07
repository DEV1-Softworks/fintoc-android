package mx.dev1.fintoc.sample.backend

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedBackendTest {

    @Test
    fun `it hands back the token it was given after its delay`() = runTest {
        val backend = SimulatedBackend(sessionToken = "cs_123", delayMillis = 1_500L)

        assertEquals("cs_123", backend.createSessionToken())
        assertEquals(1_500L, currentTime)
    }

    @Test
    fun `nothing is returned before the delay has passed`() = runTest {
        val backend = SimulatedBackend(sessionToken = "cs_123", delayMillis = 1_000L)
        var token: String? = null
        launch { token = backend.createSessionToken() }

        advanceTimeBy(999L)

        assertEquals(null, token)
    }

    @Test
    fun `it can fail the first attempt only`() = runTest {
        val backend = SimulatedBackend("cs_123", delayMillis = 10L, failFirstAttempt = true)

        val failure = runCatching { backend.createSessionToken() }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("Simulated"))
        assertEquals("cs_123", backend.createSessionToken())
        assertEquals("cs_123", backend.createSessionToken())
    }

    @Test
    fun `it never fails unless asked to`() = runTest {
        val backend = SimulatedBackend("cs_123", delayMillis = 10L)

        repeat(3) { assertEquals("cs_123", backend.createSessionToken()) }
    }
}
