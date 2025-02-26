plugins {
    kotlin("multiplatform")
}

group = "com.example"

kotlin {
    js {
        nodejs()
        browser()
    }
    sourceSets {
        jsMain {
            dependencies {
                implementation(npm("decamelize", "1.1.1"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.addAll(
        listOf(
            "--network-concurrency",
            "1",
            "--mutex",
            "network"
        )
    )
}
