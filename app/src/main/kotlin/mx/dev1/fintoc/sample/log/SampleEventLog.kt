package mx.dev1.fintoc.sample.log

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent

/**
 * The events a Widget reported, as lines of text for the screen.
 *
 * It never prints tokens or the values the Widget sends: a bank connection reports only that one was made, because its
 * exchange token belongs on your backend, not on a screen.
 */
class SampleEventLog(private val clock: () -> Long = System::currentTimeMillis) {

    private val lines = mutableStateListOf<String>()

    val entries: List<String>
        get() = lines

    fun record(event: FintocWidgetEvent) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date(clock()))
        lines += "$time  ${describe(event)}"
    }

    companion object {
        fun describe(event: FintocWidgetEvent): String = when (event) {
            is FintocWidgetEvent.Succeeded -> if (event.linkIntent != null) {
                "Succeeded: bank account connected (exchange token received, not shown)"
            } else {
                "Succeeded"
            }
            FintocWidgetEvent.Exited -> "Exited"
            is FintocWidgetEvent.Occurred -> "Event: ${event.name}"
        }
    }
}
