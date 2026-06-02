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

    val commonMain = sourceSets.getByName("commonMain")
    val macosMain = sourceSets.getByName("macosMain")
    val linuxMain = sourceSets.getByName("linuxMain")

    val unixMain = sourceSets.create("unixMain")

    unixMain.dependsOn(commonMain)
    linuxMain.dependsOn(unixMain)
    macosMain.dependsOn(unixMain)

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
