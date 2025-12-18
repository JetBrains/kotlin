plugins {
    kotlin("multiplatform")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation("it.unibo.tuprolog:parser-core-js:0.11.1")
            }
        }
    }
}

kotlin {
    js {
        nodejs()
    }
}