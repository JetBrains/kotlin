group = "com.example"

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        nodejs()
        browser()
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(npm("decamelize", "1.1.1"))
            }
        }
    }
}

tasks.named("wasmJsBrowserTest") {
    enabled = false
}