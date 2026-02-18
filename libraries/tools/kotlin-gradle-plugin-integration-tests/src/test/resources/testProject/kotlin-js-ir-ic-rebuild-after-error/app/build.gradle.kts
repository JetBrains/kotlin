plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
            compilerOptions.freeCompilerArgs.add("-Xpartial-linkage=disable")
        }
    }
}
