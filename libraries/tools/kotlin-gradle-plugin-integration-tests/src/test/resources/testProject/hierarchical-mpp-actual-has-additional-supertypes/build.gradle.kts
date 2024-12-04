import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    maven("<localRepo>")
    mavenCentral()
}

kotlin {
    jvm {}
    sourceSets {
        val common = maybeCreate("commonMain")
        val concurrent = maybeCreate("concurrentMain")
        val jvm = maybeCreate("jvmMain")

        concurrent.dependsOn(common)
        jvm.dependsOn(concurrent)
        jvm.dependsOn(common)
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        with(project.providers) {
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }
}
