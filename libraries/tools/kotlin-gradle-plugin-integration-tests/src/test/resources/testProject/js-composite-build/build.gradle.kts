plugins {
    kotlin("js")
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
}

tasks.named("browserTest") {
    enabled = false
}

tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>("kotlinNpmInstall") {
    args.addAll(
        listOf(
            "--network-concurrency",
            "1",
            "--mutex",
            "network"
        )
    )
}

dependencies {
    implementation("com.example:lib-2")
    implementation(npm("node-fetch", "3.2.8"))
    testImplementation(kotlin("test"))
}
