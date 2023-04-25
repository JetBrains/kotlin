group = "com.example"

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

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

rootProject.tasks
    .withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java)
    .named("kotlinNpmInstall")
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

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(npm("decamelize", "1.1.1"))
}