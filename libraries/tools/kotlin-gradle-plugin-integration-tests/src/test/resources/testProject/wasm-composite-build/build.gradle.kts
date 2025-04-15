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
        binaries.executable()
    }
}
kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation("com.example:lib-2")
                implementation(npm("node-fetch", "3.2.8"))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.named("wasmJsBrowserTest") {
    enabled = false
}

rootProject.tasks
    .withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java)
    .named("kotlinWasmNpmInstall")
    .configure {
        args.addAll(
            listOf(
                "--network-concurrency",
                "1",
                "--mutex",
                "network"
            )
        )
    }