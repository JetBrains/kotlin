plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val kotlin_version: String by extra

kotlin {
    sourceSets {
        jsTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
            }
        }
    }
}

kotlin {
    js {
        binaries.executable()
        nodejs()
    }
}