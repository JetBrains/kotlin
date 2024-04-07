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

subprojects {
    apply {
        plugin("kotlin-multiplatform")
    }

    kotlin {
        applyDefaultHierarchyTemplate()
        linuxX64()
        macosX64()
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
        }
    }
}

