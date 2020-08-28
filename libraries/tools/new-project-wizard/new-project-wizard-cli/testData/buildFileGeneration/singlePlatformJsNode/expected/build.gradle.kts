plugins {
    kotlin("js") version "KOTLIN_VERSION"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
dependencies {
    testImplementation(kotlin("test-js"))
}
kotlin {
    js {
        nodejs {
            binaries.executable()
        }
    }
}