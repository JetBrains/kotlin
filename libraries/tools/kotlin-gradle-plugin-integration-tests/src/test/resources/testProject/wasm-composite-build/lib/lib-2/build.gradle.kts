plugins {
    kotlin("multiplatform")
}

group = "com.example"

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
                implementation("com.example:base2")
                implementation(npm("async", "2.6.2"))
            }
        }
    }
}

tasks.named("wasmJsBrowserTest") {
    enabled = false
}