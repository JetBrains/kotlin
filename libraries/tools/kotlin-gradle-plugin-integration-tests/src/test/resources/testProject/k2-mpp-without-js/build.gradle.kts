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
    jvm()
    linuxX64()

    sourceSets {
        val commonMain by getting {}

        val intermediateMain by creating {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(intermediateMain)
        }

        val linuxX64Main by getting {
            dependsOn(intermediateMain)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
        with(project.providers) {
            languageVersion.set(KotlinVersion.fromVersion("2.0"))
        }
    }
}
