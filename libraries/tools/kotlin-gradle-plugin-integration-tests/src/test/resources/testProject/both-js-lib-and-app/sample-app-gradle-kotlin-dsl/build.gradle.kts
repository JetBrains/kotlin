import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.withType<KotlinCompile>().configureEach {
    /** Add a changing input, to enforce re-running KotlinCompile tasks in specific tests, without needing to re-run _all_ tasks. */
    val kotlinCompileCacheBuster = 0
    inputs.property("kotlinCompileCacheBuster", kotlinCompileCacheBuster)
}
