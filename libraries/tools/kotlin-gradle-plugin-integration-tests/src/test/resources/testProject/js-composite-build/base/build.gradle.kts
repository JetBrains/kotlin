plugins {
    kotlin("js")
}

group = "com.example"

kotlin {
    js {
        nodejs()
        browser()
    }
}

tasks.named("browserTest") {
    enabled = false
}

rootProject.tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>("kotlinNpmInstall") {
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
    implementation(npm("decamelize", "1.1.1"))
}
