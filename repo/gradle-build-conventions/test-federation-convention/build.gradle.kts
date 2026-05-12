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
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")

        //youtrack.jetbrains.com/issue/KT-85412
        moduleName.set(project.name)
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
    generatedKotlin.srcDir(tasks.named("generateDomainSources"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    /* The shape of the entire repo is considered an input. Always re-run this task */
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Always run this tests") { true }

    workingDir = gradle.linearClosure { it.parent }.last().rootProject.isolated.projectDirectory.asFile

    inputs.file(workingDir.resolve("repo/domains.yaml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("domains.yaml")

    environment("GRADLE_USER_HOME", gradle.gradleUserHomeDir.absolutePath)
}

dependencies {
    implementation(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    implementation(kotlin("tooling-core", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test-junit5", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(libs.jgit)
    testImplementation(libs.opentest4j)
    testImplementation(gradleTestKit())
}
