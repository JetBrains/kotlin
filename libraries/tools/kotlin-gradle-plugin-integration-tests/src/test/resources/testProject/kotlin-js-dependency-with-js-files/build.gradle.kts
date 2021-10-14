plugins {
    kotlin("js")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("it.unibo.tuprolog:parser-core-js:0.11.1")
}

kotlin {
    js {
        nodejs()
    }
}