package mx.dev1.fintoc.sdk.presentation.host

import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FintocWidgetRequestsTest {

    private val requests = FintocWidgetRequests()

    private fun options(token: String) = FintocWidgetOptions.Payments(sessionToken = token)

    @Test
    fun `a registered request is found by its identifier`() {
        val requestId = requests.register(options("cs_1"))

        assertEquals(options("cs_1"), requests.find(requestId))
    }

    @Test
    fun `identifiers are unique and hard to guess`() {
        val first = requests.register(options("cs_1"))
        val second = requests.register(options("cs_1"))

        assertNotEquals(first, second)
        assertEquals(36, first.length)
    }

    @Test
    fun `an unknown identifier finds nothing`() {
        assertNull(requests.find("not-a-request"))
    }

    @Test
    fun `a removed request is gone`() {
        val requestId = requests.register(options("cs_1"))

        requests.remove(requestId)

        assertNull(requests.find(requestId))
    }

    @Test
    fun `finding a request does not consume it, so a screen can be recreated`() {
        val requestId = requests.register(options("cs_1"))

        requests.find(requestId)

        assertEquals(options("cs_1"), requests.find(requestId))
    }

    @Test
    fun `only the latest few requests are kept`() {
        val requestIds = (1..6).map { number -> requests.register(options("cs_$number")) }

        assertNull(requests.find(requestIds[0]))
        assertNull(requests.find(requestIds[1]))
        requestIds.drop(2).forEachIndexed { index, requestId ->
            assertEquals(options("cs_${index + 3}"), requests.find(requestId))
        }
    }

    @Test
    fun `clear forgets everything`() {
        val requestId = requests.register(options("cs_1"))

        requests.clear()

        assertNull(requests.find(requestId))
    }
}
