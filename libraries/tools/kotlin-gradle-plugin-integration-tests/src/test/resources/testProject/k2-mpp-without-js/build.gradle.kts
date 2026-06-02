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
        val commonMain = getByName("commonMain") {}

        val intermediateMain = create("intermediateMain") {
            dependsOn(commonMain)
        }

        val jvmMain = getByName("jvmMain") {
            dependsOn(intermediateMain)
        }

        val linuxX64Main = getByName("linuxX64Main") {
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
