package mx.dev1.fintoc.sample.log

import java.util.TimeZone
import mx.dev1.fintoc.sdk.domain.widget.FintocLinkIntentResult
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SampleEventLogTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun useUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `a new log is empty`() {
        assertTrue(SampleEventLog().entries.isEmpty())
    }

    @Test
    fun `events are listed in order, each with its time`() {
        var now = 0L
        val log = SampleEventLog(clock = { now })

        log.record(FintocWidgetEvent.Occurred(name = "opened"))
        now = 65_000L
        log.record(FintocWidgetEvent.Exited)

        assertEquals(listOf("00:00:00  Event: opened", "00:01:05  Exited"), log.entries)
    }

    @Test
    fun `every kind of event has a description`() {
        assertEquals("Succeeded", SampleEventLog.describe(FintocWidgetEvent.Succeeded()))
        assertEquals("Exited", SampleEventLog.describe(FintocWidgetEvent.Exited))
        assertEquals("Event: on_error", SampleEventLog.describe(FintocWidgetEvent.Occurred("on_error")))
    }

    @Test
    fun `a bank connection is reported without its exchange token`() {
        val event = FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_secret_token", id = "li_1"))

        val line = SampleEventLog.describe(event)

        assertTrue(line.contains("bank account connected"))
        assertFalse(line.contains("et_secret_token"))
        assertFalse(line.contains("li_1"))
    }

    @Test
    fun `values the widget sends are never printed`() {
        val event = FintocWidgetEvent.Occurred("opened", metadata = mapOf("secret" to "sensitive-value"))

        assertFalse(SampleEventLog.describe(event).contains("sensitive-value"))
    }
}
