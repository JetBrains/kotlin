plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        nodejs()
        browser()
        binaries.executable()
    }
    sourceSets {
        jsMain {
            dependencies {
                implementation("com.example:lib-2")
                implementation(npm("node-fetch", "3.2.8"))
            }
        }
        jsTest {
            dependencies {
                implementation(kotlin("test"))
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
