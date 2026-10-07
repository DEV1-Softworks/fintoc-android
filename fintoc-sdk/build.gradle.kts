import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

apply(from = rootProject.file("gradle/jacoco-coverage.gradle.kts"))
apply(from = rootProject.file("gradle/robolectric.gradle.kts"))

android {
    namespace = "mx.dev1.fintoc.sdk"
    resourcePrefix = "fintoc_"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
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

kotlin {
    explicitApi()

    // Kotlin writes the language version into the metadata of every class, and a compiler can only read metadata up to
    // one version newer than itself. Compiling at 2.2 lets apps on Kotlin 2.1 and later consume the library, whatever
    // version this build uses. Older levels are deprecated by the compiler, and in practice an app needs Android
    // Gradle Plugin 9 to compile against API 37, which ships Kotlin 2.2.
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }

    // The standard library is a dependency of the published library, and Gradle gives an app the newest version that
    // any dependency asks for. Asking for the one of this build (2.4) would hand apps on an older Kotlin a library their
    // compiler cannot read.
    coreLibrariesVersion = "2.2.0"
}

// Coordinates, version and POM data are in gradle.properties. Credentials are never in the repository: see
// docs/en/releasing.md.
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    // Exposed in the public API: FintocWidgetContract is an ActivityResultContract.
    api(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
