@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.tooling.core.linearClosure

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

apply(from = "codegen.gradle.kts")

repositories {
    /* Required for intellij-asm */
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        content {
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
        }
    }

    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

project.configurations.named(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main") {
    resolutionStrategy {
        eachDependency {
            if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
        }
    }
}

project.configurations.named(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Test") {
    resolutionStrategy {
        eachDependency {
            if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
        }
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(tasks.named("generateSubsystemSources"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    workingDir = gradle.linearClosure { it.parent }.last().rootProject.isolated.projectDirectory.asFile
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
    implementation(kotlin("tooling-core"))
    compileOnly(kotlin("gradle-plugin"))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.intellij.asm)
    //implementation(libs.asm)
    //implementation(libs.asm.tree)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test-junit5", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(libs.jgit)
    testImplementation(libs.opentest4j)
}
