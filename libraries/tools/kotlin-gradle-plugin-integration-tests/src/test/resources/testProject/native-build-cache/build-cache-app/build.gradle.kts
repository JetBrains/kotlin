plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64("host") {
        binaries {
            sharedLib(listOf(DEBUG))
            staticLib(listOf(DEBUG))
        }
    }

    sourceSets {
        val hostMain by getting {
            dependencies {
                implementation("com.example:build-cache-lib:1.0")
                api(project(":build-cache-app:lib-module"))
            }
        }
    }
}
