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
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }
    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies{
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
            }
        }
    }
}
