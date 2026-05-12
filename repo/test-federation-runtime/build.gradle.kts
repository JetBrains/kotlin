@file:OptIn(TemporaryTestFederationApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.testFederation.GenerateTestFederationRuntimeCodeTask
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    kotlin("jvm")
    id("test-inputs-check")
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
            languageVersion.set(KotlinVersion.KOTLIN_2_1)
            apiVersion.set(KotlinVersion.KOTLIN_2_1)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    /* Used by the ContractAndSmokeTest and 'PseudoTest' for testing the test federations behavior */
    providers.environmentVariable("_PSEUDO_TEST_").orNull?.let { value ->
        smokeTestConfig = when (value) {
            "RunAllTests" -> SmokeTestConfig.RunAllTests
            "Disabled" -> SmokeTestConfig.Disabled
            else -> error("Unknown _PSEUDO_TEST_ configuration")
        }
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    implementation(libs.junit.jupiter.api)
    compileOnly(libs.junit4)

    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.api)
}
