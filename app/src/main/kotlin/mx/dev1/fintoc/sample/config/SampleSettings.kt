package mx.dev1.fintoc.sample.config

import mx.dev1.fintoc.BuildConfig

/**
 * Settings of the sample. The public key comes from `local.properties` (`fintoc.publicKey=pk_test_…`), which Git
 * ignores. Public keys are safe to ship in an app; the build refuses a secret key (`sk_…`).
 */
class SampleSettings(val publicKey: String) {

    /** `true` while nobody has set a real key, in which case the Widget cannot start a real flow. */
    val usesPlaceholderKey: Boolean
        get() = publicKey == PLACEHOLDER_PUBLIC_KEY

    companion object {
        const val PLACEHOLDER_PUBLIC_KEY = "pk_test_replace_me"

        fun fromBuildConfig(): SampleSettings = SampleSettings(BuildConfig.FINTOC_PUBLIC_KEY)
    }
}
