plugins {
    kotlin("js")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
            kotlinOptions.freeCompilerArgs += "-Xpartial-linkage=disable"
        }
    }
}
