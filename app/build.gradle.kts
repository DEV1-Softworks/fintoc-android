import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

apply(from = rootProject.file("gradle/jacoco-coverage.gradle.kts"))
apply(from = rootProject.file("gradle/robolectric.gradle.kts"))

// The sample reads your Fintoc public key from local.properties, which Git ignores:
//   fintoc.publicKey=pk_test_...
// Public keys are safe to ship in an app. Secret keys are not, so the build refuses one.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { stream -> load(stream) }
}
val fintocPublicKey: String = localProperties.getProperty("fintoc.publicKey")?.trim().orEmpty()
    .ifEmpty { "pk_test_replace_me" }
require(!fintocPublicKey.startsWith("sk_")) {
    "fintoc.publicKey in local.properties looks like a secret key (sk_). Use your public key (pk_test_...)."
}
require(fintocPublicKey.matches(Regex("pk_(test|live)_[A-Za-z0-9_]+"))) {
    "fintoc.publicKey in local.properties must look like pk_test_... or pk_live_..."
}

android {
    namespace = "mx.dev1.fintoc"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "mx.dev1.fintoc"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FINTOC_PUBLIC_KEY", "\"$fintocPublicKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            // The sample shrinks with R8, so every build checks that the SDK survives it.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { unitTestTask ->
                // Robolectric loads classes through its own classloader; JaCoCo skips them unless told otherwise.
                unitTestTask.extensions.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":fintoc-sdk"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
