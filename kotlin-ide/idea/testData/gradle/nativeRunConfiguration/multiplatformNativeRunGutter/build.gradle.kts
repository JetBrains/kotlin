plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("{{kotlin_plugin_version}}")
}
repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    mavenLocal()
}

kotlin {
    macosX64("macos") {
        binaries {
            executable {
                entryPoint = "sample.main"
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val macosMain by getting {
        }
    }
}