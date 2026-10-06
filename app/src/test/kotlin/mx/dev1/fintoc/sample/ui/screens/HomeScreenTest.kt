package mx.dev1.fintoc.sample.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.SampleDestination
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.FintocLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resourceId)

    private val opened = mutableListOf<SampleDestination>()
    private val languages = mutableListOf<FintocLanguage?>()

    private fun show(
        settings: SampleSettings = SampleSettings("pk_test_abc123"),
        language: FintocLanguage? = null,
    ) {
        composeRule.setContent {
            FintocSampleTheme {
                HomeScreen(
                    settings = settings,
                    language = language,
                    onLanguageSelected = { selected -> languages += selected },
                    onOpen = { destination -> opened += destination },
                )
            }
        }
    }

    @Test
    fun `the title is a heading and the introduction is shown`() {
        show()

        val title = composeRule.onNodeWithText(string(R.string.sample_title)).fetchSemanticsNode()
        assertTrue(title.config.contains(SemanticsProperties.Heading))
        composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
    }

    @Test
    fun `each demo opens its own screen`() {
        show()
        val expected = listOf(
            R.string.demo_movements_title to SampleDestination.MOVEMENTS,
            R.string.demo_payments_options_title to SampleDestination.PAYMENTS_OPTIONS,
            R.string.demo_payments_provider_title to SampleDestination.PAYMENTS_PROVIDER,
            R.string.demo_payments_activity_title to SampleDestination.PAYMENTS_ACTIVITY,
            R.string.demo_hosted_title to SampleDestination.HOSTED_CHECKOUT,
        )

        expected.forEach { (title, _) -> composeRule.onNodeWithText(string(title)).performScrollTo().performClick() }

        assertEquals(expected.map { it.second }, opened)
    }

    @Test
    fun `a placeholder key is warned about`() {
        show(settings = SampleSettings(SampleSettings.PLACEHOLDER_PUBLIC_KEY))

        composeRule.onNodeWithText(string(R.string.home_placeholder_key_warning)).assertIsDisplayed()
    }

    @Test
    fun `a real key is not warned about`() {
        show(settings = SampleSettings("pk_test_abc123"))

        composeRule.onNodeWithText(string(R.string.home_placeholder_key_warning)).assertDoesNotExist()
    }

    @Test
    fun `picking a language reports it, and automatic reports none`() {
        show(language = FintocLanguage.SPANISH)

        listOf(
            R.string.language_english, R.string.language_spanish, R.string.language_french,
            R.string.language_portuguese, R.string.language_automatic,
        ).forEach { name -> composeRule.onNodeWithText(string(name)).performScrollTo().performClick() }

        assertEquals(
            listOf(
                FintocLanguage.ENGLISH, FintocLanguage.SPANISH, FintocLanguage.FRENCH, FintocLanguage.PORTUGUESE, null,
            ),
            languages,
        )
    }

    @Test
    fun `the chosen language is the selected chip`() {
        show(language = FintocLanguage.FRENCH)

        composeRule.onNodeWithText(string(R.string.language_french)).performScrollTo().assertIsSelected()
    }

    @Test
    fun `automatic is selected when no language is chosen`() {
        show(language = null)

        composeRule.onNodeWithText(string(R.string.language_automatic)).performScrollTo().assertIsSelected()
    }
}
