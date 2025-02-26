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
                implementation("com.example:base")
                implementation(npm("async", "2.6.2"))
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
