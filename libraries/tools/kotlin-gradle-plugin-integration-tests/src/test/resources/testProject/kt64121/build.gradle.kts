plugins {
    kotlin("multiplatform")
}

group = "com.example.bar"
version = "1.0"

kotlin {
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val linuxAndJsMain by creating {
            dependsOn(commonMain)
        }

        js().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        linuxX64().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
        }
    }
}
