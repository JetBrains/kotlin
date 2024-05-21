plugins {
    kotlin("multiplatform")
}

group = "org.sample.one"

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
        commonMain {
            dependencies {
                implementation(project(":foo:foo"))
            }
        }
    }
}
