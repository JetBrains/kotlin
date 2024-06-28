plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    macosX64("macos")
    linuxX64("linux")
    mingwX64("windows")

    val commonMain by sourceSets.getting
    val macosMain by sourceSets.getting
    val linuxMain by sourceSets.getting

    val unixMain by sourceSets.creating

    unixMain.dependsOn(commonMain)
    linuxMain.dependsOn(unixMain)
    macosMain.dependsOn(unixMain)

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
