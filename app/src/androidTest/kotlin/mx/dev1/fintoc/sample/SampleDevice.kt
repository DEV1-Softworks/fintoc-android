package mx.dev1.fintoc.sample

import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

/** Looks at the device from outside the app, which is how a screen of the SDK's own Activity has to be checked. */
object SampleDevice {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    /** The Activity that has the screen right now, as `package/class`, or an empty text when it cannot be read. */
    fun topActivity(): String =
        Regex("topResumedActivity=ActivityRecord\\{[^ ]+ u\\d+ ([^ ]+) ")
            .find(shell("dumpsys activity activities"))?.groupValues?.get(1).orEmpty()

    /**
     * `true` when some text, or content description, on the screen of the foreground app contains [text].
     *
     * The tree is walked here because Compose does not implement `findAccessibilityNodeInfosByText`: it always
     * answers with nothing, even for text that is plainly on screen.
     */
    fun screenShows(text: String): Boolean = instrumentation.uiAutomation.rootInActiveWindow?.shows(text) ?: false

    private fun AccessibilityNodeInfo.shows(text: String): Boolean {
        if (this.text?.contains(text) == true || contentDescription?.contains(text) == true) return true
        return (0 until childCount).any { index -> getChild(index)?.shows(text) == true }
    }

    fun waitUntil(timeoutMillis: Long = 15_000L, description: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for: $description" }
            Thread.sleep(250L)
        }
    }
}
