group = "com.h0tk3y.mpp.demo"
version = "1.0"

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
	mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting{
            dependencies{
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
            }
        }
    }
}
