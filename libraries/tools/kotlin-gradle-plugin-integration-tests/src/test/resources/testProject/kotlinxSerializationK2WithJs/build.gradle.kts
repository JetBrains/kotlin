plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            }
        }
    }
}

kotlin {
    js {
        binaries.executable()
        browser {}
    }
}
