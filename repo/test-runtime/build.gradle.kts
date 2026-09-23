import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.testFederation.DelicateTestFederationApi
import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.GenerateTestFederationRuntimeCodeTask
import org.jetbrains.kotlin.testFederation.fromArgumentStringOrThrow
import org.jetbrains.kotlin.testFederation.testFederation
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    /* Used by the TestFederationFunctionalTest and 'PseudoTest' for testing the test federations behavior */
    testFederation {
        providers.environmentVariable("_SMOKE_TESTS_INCLUDE_ALL_").orNull?.let {
            smokeTests { includeAll() }
        }
        providers.environmentVariable("_SKIP_SMOKES_").orNull?.let {
            smokeTests { skip() }
        }
        providers.environmentVariable("_SKIP_CONTRACTS_").orNull?.let {
            contractTests { skip() }
        }
    }

    @OptIn(DelicateTestFederationApi::class)
    providers.environmentVariable("_DOMAINS_OVERRIDE_").orNull?.let { value ->
        testFederationDomains = Domain.fromArgumentStringOrThrow(value)
    }

    testLogging {
        // Required by 'TestFederationFunctionalTest': events are asserted
        events("passed", "skipped", "failed")

        // Required by 'TestFederationFunctionalTest': fixture output is asserted via 'runTestEvents'
        showStandardStreams = true
    }
}

dependencies {
    compileOnly(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.platform.launcher)

    testImplementation(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(kotlin("test-junit", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
}
