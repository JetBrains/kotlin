plugins {
    kotlin("multiplatform")
    id("java-test-fixtures")
}

kotlin {
    jvmToolchain(21)

    jvm()
    linuxX64()
}

dependencies {
    "testFixturesApi"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1") // only awailable since Gradle 8.4
    "jvmTestFixturesApi"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}
