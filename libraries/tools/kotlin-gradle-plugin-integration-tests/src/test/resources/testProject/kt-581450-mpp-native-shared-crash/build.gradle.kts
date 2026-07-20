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
    linuxArm64()
    sourceSets {
        val commonMain = getByName("commonMain")
        val jvmMain = getByName("jvmMain") {
            dependsOn(commonMain)
        }
        val nativeMain = create("nativeMain") {
            dependsOn(commonMain)
        }
        val linuxX64Main = getByName("linuxX64Main") {
            dependsOn(nativeMain)
        }
        val linuxArm64Main = getByName("linuxArm64Main") {
            dependsOn(nativeMain)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
}