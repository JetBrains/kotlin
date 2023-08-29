plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":lib"))
}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    friendPaths.from(project(":lib").buildDir.resolve("libs/lib.klib"))
}