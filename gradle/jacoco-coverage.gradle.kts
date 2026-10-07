// Shared JaCoCo setup applied by every Android module:
//   apply(from = rootProject.file("gradle/jacoco-coverage.gradle.kts"))
//
// It merges the coverage of local unit tests (Robolectric included) and instrumented tests, then
// exposes two tasks per module:
//   jacocoDebugCoverageReport       -> HTML + XML report in build/reports/jacoco/
//   jacocoDebugCoverageVerification -> fails the build below MINIMUM_COVERAGE_RATIO
//
// Usage: ./gradlew testDebugUnitTest connectedDebugAndroidTest jacocoDebugCoverageVerification

import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

val minimumCoverageRatio = "0.80".toBigDecimal()

apply(plugin = "jacoco")

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
extensions.configure<JacocoPluginExtension> {
    toolVersion = versionCatalog.findVersion("jacoco").get().requiredVersion
}

val generatedClassPatterns = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*ComposableSingletons*.*",
    "**/*Preview*.*", // Android Studio tooling only, never executed at runtime.
)

// Output folder of the Kotlin compiler built into AGP 9 (debug variant).
val debugKotlinClasses = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
    exclude(generatedClassPatterns)
}

val coverageExecutionData = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
    include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
}

// Coverage tasks never trigger the tests (instrumented ones need a device), but when tests, or clean,
// are part of the same build they must finish first.
val tasksThatMustFinishFirst = tasks.matching {
    it.name in setOf("clean", "testDebugUnitTest", "connectedDebugAndroidTest")
}

tasks.register<JacocoReport>("jacocoDebugCoverageReport") {
    group = "verification"
    mustRunAfter(tasksThatMustFinishFirst)
    description = "Generates the merged unit + instrumented coverage report for the debug variant."

    executionData.setFrom(coverageExecutionData)
    classDirectories.setFrom(debugKotlinClasses)
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main/kotlin"))

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoDebugCoverageVerification") {
    group = "verification"
    mustRunAfter(tasksThatMustFinishFirst)
    description = "Fails when merged coverage of the debug variant is below the required minimum."

    executionData.setFrom(coverageExecutionData)
    classDirectories.setFrom(debugKotlinClasses)
    sourceDirectories.setFrom(layout.projectDirectory.dir("src/main/kotlin"))

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = minimumCoverageRatio
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = minimumCoverageRatio
            }
        }
    }
}
