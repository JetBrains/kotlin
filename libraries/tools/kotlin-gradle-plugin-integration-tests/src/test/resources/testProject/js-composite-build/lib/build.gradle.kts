group = "com.example"

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
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
    implementation(npm("async", "2.6.2"))
}