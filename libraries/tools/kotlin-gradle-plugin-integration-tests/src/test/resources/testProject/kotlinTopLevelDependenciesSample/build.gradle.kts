plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

kotlin {
    jvm()
    js(IR) {
        browser()
    }
    linuxX64()

    @OptIn(org.jetbrains.kotlin.gradle.KotlinTopLevelDependencies::class)
    dependencies {
        // Add kotlinx coroutines as a top-level API dependency
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    }
}

// Configure publishing
publishing {
    repositories {
        maven(layout.buildDirectory.dir("repo"))
    }
}