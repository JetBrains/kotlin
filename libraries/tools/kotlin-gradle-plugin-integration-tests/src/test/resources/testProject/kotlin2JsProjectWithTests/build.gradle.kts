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
        jsMain {
            dependencies {
                testImplementation("org.jetbrains.kotlin:kotlin-test-js:$kotlin_version")
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