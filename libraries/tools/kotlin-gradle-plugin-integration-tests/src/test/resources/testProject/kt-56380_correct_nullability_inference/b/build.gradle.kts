import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a"))
            }
        }
    }
}

tasks.withType(KotlinCompilationTask::class.java).configureEach {
    compilerOptions.allWarningsAsErrors.set(true)
}