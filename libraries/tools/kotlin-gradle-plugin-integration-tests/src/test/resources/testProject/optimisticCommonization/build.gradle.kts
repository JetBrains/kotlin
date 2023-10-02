plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    linuxArm64("shortPlatform") {
        compilations.get("main").cinterops.create("intPropertyInterop") {
            header(file("libs/shortPlatform.h"))
        }
    }
    linuxX64("longPlatform") {
        compilations.get("main").cinterops.create("intPropertyInterop") {
            header(file("libs/longPlatform.h"))
        }
    }
}
