plugins {
    kotlin("multiplatform")
}

group = "org.sample.two"

kotlin {
    linuxX64("linux") {
        val bar by compilations["main"].cinterops.creating
    }
    js {
        nodejs()
    }

    sourceSets {
        configureEach {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}
