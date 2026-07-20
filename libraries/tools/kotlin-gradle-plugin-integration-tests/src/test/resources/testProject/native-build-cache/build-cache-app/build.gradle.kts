plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
        val hostMain = getByName("hostMain") {
            dependencies {
                implementation("com.example:build-cache-lib:1.0")
                api(project(":build-cache-app:lib-module"))
            }
        }
    }
}
