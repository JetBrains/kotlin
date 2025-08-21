import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm {
        compilerOptions.freeCompilerArgs.add("-XXlenient-mode")
    }
}
