plugins {
    kotlin("js") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("KOTLIN_REPO") }
}

dependencies {
    testImplementation(kotlin("test-js"))
}

kotlin {
    js(LEGACY) {
        nodejs {
            binaries.executable()
        }
    }
}