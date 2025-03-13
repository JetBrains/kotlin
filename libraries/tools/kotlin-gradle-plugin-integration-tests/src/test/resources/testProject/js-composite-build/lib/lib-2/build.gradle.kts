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
    implementation("com.example:base2")

    implementation(npm("tiny-invariant", "1.3.3"))
    api(npm("is-obj", "3.0.0"))
    runtimeOnly(npm("async", "2.6.2"))
    // No compileOnly dependency because they are not supported. See  IncorrectCompileOnlyDependenciesChecker.
}
