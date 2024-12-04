plugins {
    kotlin("js")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val kotlin_version: String by extra

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-js:$kotlin_version")
}

kotlin {
    js {
        binaries.executable()
        nodejs()
    }
}