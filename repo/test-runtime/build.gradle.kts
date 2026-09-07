@file:OptIn(TemporaryTestFederationApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.testFederation.DelicateTestFederationApi
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.GenerateTestFederationRuntimeCodeTask
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.fromArgumentStringOrThrow
import org.jetbrains.kotlin.testFederation.smokeTestConfig
import org.jetbrains.kotlin.testFederation.testFederationDomains

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    coreLibrariesVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
}

val generateSources = tasks.register<GenerateTestFederationRuntimeCodeTask>("generateTestFederationSources")

kotlin.sourceSets.main.configure {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    generatedKotlin.srcDir(generateSources.map { it.outputDir })
}

kotlin.target.compilations.all {
    compileTaskProvider.configure {
        compilerOptions {
            freeCompilerArgs.add("-Xsuppress-version-warnings")
            languageVersion.set(KotlinVersion.KOTLIN_2_2)
            apiVersion.set(KotlinVersion.KOTLIN_2_2)
        }
    }
}

kotlin.target.compilations.getByName("main").compileTaskProvider.configure {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.testFederation.InternalTestFederationApi")
    }
}

tasks.test.configure {
    useJUnitPlatform()

    /* Used by the TestFederationFunctionalTest and 'PseudoTest' for testing the test federations behavior */
    providers.environmentVariable("_PSEUDO_TEST_").orNull?.let { value ->
        smokeTestConfig = when (value) {
            "RunAllTests" -> SmokeTestConfig.RunAllTests
            "Disabled" -> SmokeTestConfig.Disabled
            else -> error("Unknown _PSEUDO_TEST_ configuration")
        }
    }

    @OptIn(DelicateTestFederationApi::class)
    providers.environmentVariable("_DOMAINS_OVERRIDE_").orNull?.let { value ->
        testFederationDomains = Domain.fromArgumentStringOrThrow(value)
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    implementation(libs.junit.jupiter.api)

    testImplementation(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(kotlin("test-junit", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.api)

    compileOnly(libs.junit.platform.launcher)
}

/* Create synthetic test tasks */
run {
    val junit5TestCompilation = kotlin.target.compilations.create("junit5Tests")

    tasks.register<Test>("junit5Tests") {
        description = "Synthetic Tests: Used by functional tests to create test build behavior (on junit5)"
        useJUnitPlatform()
        testClassesDirs = junit5TestCompilation.output.classesDirs
        classpath = junit5TestCompilation.runtimeDependencyFiles

        providers.gradleProperty("tests.additionalJvmArgument").orNull?.let { args ->
            jvmArgs(args.split(" "))
        }

        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    dependencies {
        junit5TestCompilation.configurations.implementationConfiguration(kotlin("test-junit5"))
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.api)
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.engine)
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.params)
    }
}
