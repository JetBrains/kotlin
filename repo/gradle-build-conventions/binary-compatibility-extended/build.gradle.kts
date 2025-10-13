import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

description = "Binary Compatibility Validator compat - track ABI changes"

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)
}

dependencies {
    implementation(libs.javaDiffUtils)
    compileOnly(libs.kotlinx.bcv)
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
}

val generateBcvProperties by tasks.registering {
    val generatedSrcDir = layout.buildDirectory.dir("src/generated/kotlin")
    outputs.dir(generatedSrcDir).withPropertyName("generatedSrcDir")
    outputs.cacheIf { true }

    val bcvVersion = libs.versions.kotlinx.bcv
    inputs.property("bcvVersion", bcvVersion)

    doLast {
        val outputDir = generatedSrcDir.get().asFile
        outputDir.mkdirs()
        outputDir.resolve("BcvProperties.kt").writeText(
            """
            |package org.jetbrains.kotlin.build.bcv.internal
            |
            |internal object BcvProperties {
            |    const val KOTLINX_BCV_VERSION = "${bcvVersion.get()}"
            |}
            |
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateBcvProperties)
}

gradlePlugin {
    plugins {
        create("bcvCompat") {
            id = "kotlin-git.gradle-build-conventions.binary-compatibility-extended"
            implementationClass = "org.jetbrains.kotlin.build.bcv.BcvCompatPlugin"
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // reproducible builds https://docs.gradle.org/8.8/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
