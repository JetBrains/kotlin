plugins {
    kotlin("multiplatform")
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries.executable()
    }

    sourceSets.commonMain {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
        }
    }
}