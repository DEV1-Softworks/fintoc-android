// Shared Robolectric setup applied by every Android module:
//   apply(from = rootProject.file("gradle/robolectric.gradle.kts"))

tasks.withType<Test>().configureEach {
    // Robolectric reaches into JDK internals to emulate Android file descriptors.
    // Without this, every Robolectric test fails on JDK 17+ with an IllegalAccessException.
    jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
}
