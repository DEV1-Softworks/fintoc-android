package mx.dev1.fintoc.sample.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FintocSampleThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var isDarkTheme by mutableStateOf(false)
    private lateinit var appliedColorScheme: ColorScheme

    private fun showTheme(isDark: Boolean) {
        isDarkTheme = isDark
        composeRule.setContent {
            FintocSampleTheme(isDarkTheme = isDarkTheme) { appliedColorScheme = MaterialTheme.colorScheme }
        }
        composeRule.waitForIdle()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `devices without dynamic color use the static light palette`() {
        showTheme(isDark = false)

        assertEquals(lightColorScheme().primary, appliedColorScheme.primary)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `devices without dynamic color use the static dark palette`() {
        showTheme(isDark = true)

        assertEquals(darkColorScheme().primary, appliedColorScheme.primary)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `devices with dynamic color keep light and dark palettes distinct`() {
        showTheme(isDark = false)
        val lightPrimary = appliedColorScheme.primary

        isDarkTheme = true
        composeRule.waitForIdle()

        assertNotEquals(lightPrimary, appliedColorScheme.primary)
    }
}
