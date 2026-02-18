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

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        content {
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
        }
    }
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
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

project.configurations.named(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main") {
    resolutionStrategy {
        eachDependency {
            if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
        }
    }
}
