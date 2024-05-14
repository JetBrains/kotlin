plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "com.example"
version = "1.0"

kotlin {
    val nodeJs = js("nodeJs")
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.example:sample-lib:1.0")
            }
        }
    }
}
