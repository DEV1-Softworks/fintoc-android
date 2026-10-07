package mx.dev1.fintoc.sdk.presentation.localization

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FintocStringsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun retryIn(language: FintocLanguage?): String =
        FintocStrings(context.resourcesIn(language)).get(R.string.fintoc_widget_error_retry)

    @Test
    fun `every language has its own text`() {
        assertEquals("Try again", retryIn(FintocLanguage.ENGLISH))
        assertEquals("Reintentar", retryIn(FintocLanguage.SPANISH))
        assertEquals("Réessayer", retryIn(FintocLanguage.FRENCH))
        assertEquals("Tentar novamente", retryIn(FintocLanguage.PORTUGUESE))
    }

    @Test
    fun `an override wins over the language of the device`() {
        assertEquals("Reintentar", retryIn(FintocLanguage.SPANISH))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `without an override the language of the device is followed`() {
        assertEquals("Réessayer", retryIn(language = null))
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `regional variants of the device language are covered`() {
        assertEquals("Tentar novamente", retryIn(language = null))
    }

    @Test
    @Config(qualifiers = "de")
    fun `a device language the sdk does not translate falls back to english`() {
        assertEquals("Try again", retryIn(language = null))
    }

    @Test
    fun `an override keeps the rest of the configuration such as the font scale`() {
        val largeTextContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { fontScale = 2f },
        )

        val resources = largeTextContext.resourcesIn(FintocLanguage.FRENCH)

        assertEquals(2f, resources.configuration.fontScale, 0f)
        assertEquals("fr", resources.configuration.locales[0].language)
    }

    @Test
    fun `overriding does not change the resources of the context`() {
        context.resourcesIn(FintocLanguage.SPANISH)

        assertEquals("Try again", FintocStrings(context.resources).get(R.string.fintoc_widget_error_retry))
    }

    @Test
    fun `no text is missing or left untranslated in any language`() {
        val textIds = listOf(
            R.string.fintoc_widget_loading,
            R.string.fintoc_widget_error_message,
            R.string.fintoc_widget_error_retry,
        )

        textIds.forEach { textId ->
            val english = FintocStrings(context.resourcesIn(FintocLanguage.ENGLISH)).get(textId)
            assertTrue(english.isNotBlank())
            listOf(FintocLanguage.SPANISH, FintocLanguage.FRENCH, FintocLanguage.PORTUGUESE).forEach { language ->
                val translated = FintocStrings(context.resourcesIn(language)).get(textId)
                assertTrue("$language $textId", translated.isNotBlank())
                assertNotEquals("$language $textId is not translated", english, translated)
            }
        }
    }
}
