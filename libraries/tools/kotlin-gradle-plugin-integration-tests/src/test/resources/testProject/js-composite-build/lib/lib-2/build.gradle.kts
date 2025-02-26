plugins {
    kotlin("js")
}

group = "com.example"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.js {
    nodejs()
    browser()
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
    implementation("com.example:base")
    implementation(npm("async", "2.6.2"))
}
