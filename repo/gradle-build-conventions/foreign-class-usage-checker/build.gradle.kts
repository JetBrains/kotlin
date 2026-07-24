import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

description = "Foreign Class Usage Checker – track dependency usage in libraries"

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    implementation(libs.intellij.asm)
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.`for`.gradle.plugins.compilation.get()}")
    implementation(libs.diff.utils)
}

gradlePlugin {
    plugins {
        create("foreign-class-usage-checker") {
            id = "kotlin-git.gradle-build-conventions.foreign-class-usage-checker"
            implementationClass = "org.jetbrains.kotlin.build.foreign.ForeignClassUsageCheckerPlugin"
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // reproducible builds https://docs.gradle.org/8.8/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
    }
}

kotlin.compilerOptions.moduleName.value(project.name)
