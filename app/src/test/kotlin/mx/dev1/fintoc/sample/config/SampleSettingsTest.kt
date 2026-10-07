package mx.dev1.fintoc.sample.config

import mx.dev1.fintoc.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleSettingsTest {

    @Test
    fun `the placeholder key is recognized so the screen can warn about it`() {
        assertTrue(SampleSettings(SampleSettings.PLACEHOLDER_PUBLIC_KEY).usesPlaceholderKey)
    }

    @Test
    fun `a real key is not a placeholder`() {
        assertFalse(SampleSettings("pk_test_abc123").usesPlaceholderKey)
        assertFalse(SampleSettings("pk_live_abc123").usesPlaceholderKey)
    }

    @Test
    fun `the settings read the public key of the build`() {
        assertEquals(BuildConfig.FINTOC_PUBLIC_KEY, SampleSettings.fromBuildConfig().publicKey)
    }

    @Test
    fun `the build never ships a secret key`() {
        assertFalse(BuildConfig.FINTOC_PUBLIC_KEY.startsWith("sk_"))
        assertTrue(BuildConfig.FINTOC_PUBLIC_KEY.matches(Regex("pk_(test|live)_[A-Za-z0-9_]+")))
    }
}
