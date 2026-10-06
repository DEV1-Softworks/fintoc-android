package mx.dev1.fintoc.sdk.presentation.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidExternalLinkLauncherTest {

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an https address opens as a browsable view intent`() {
        AndroidExternalLinkLauncher(applicationContext).open("https://files.example.com/voucher.pdf")

        val intent = shadowOf(applicationContext as android.app.Application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://files.example.com/voucher.pdf", intent.dataString)
        assertTrue(intent.categories.contains(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `a context without an activity starts a new task`() {
        AndroidExternalLinkLauncher(applicationContext).open("https://files.example.com/voucher.pdf")

        val intent = shadowOf(applicationContext as android.app.Application).nextStartedActivity
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `an activity wrapped by other contexts is found and keeps the current task`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextWrapper(ContextWrapper(activity))

        AndroidExternalLinkLauncher(wrapped).open("https://files.example.com/voucher.pdf")

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `addresses that are not https are dropped`() {
        listOf("http://files.example.com/voucher.pdf", "intent://x#Intent;end", "fintocwidget://exit", "", "https://")
            .forEach { url -> AndroidExternalLinkLauncher(applicationContext).open(url) }

        assertNull(shadowOf(applicationContext as android.app.Application).nextStartedActivity)
    }

    @Test
    fun `a device with no browser is tolerated`() {
        val context: Context = mock { on { startActivity(any()) } doThrow ActivityNotFoundException() }

        AndroidExternalLinkLauncher(context).open("https://files.example.com/voucher.pdf")

        verify(context).startActivity(any())
    }

    @Test
    fun `nothing is started for a dropped address`() {
        val context: Context = mock()

        AndroidExternalLinkLauncher(context).open("http://files.example.com/voucher.pdf")

        verify(context, never()).startActivity(any())
    }
}
