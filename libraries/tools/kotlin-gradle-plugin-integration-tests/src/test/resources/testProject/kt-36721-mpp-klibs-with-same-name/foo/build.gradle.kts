plugins {
    kotlin("multiplatform")
}

group = "org.sample.one"

kotlin {
    linuxX64("linux") {
        compilations["main"].cinterops.create("bar")
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
