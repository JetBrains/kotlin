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
            freeCompilerArgs.addAll("-Xsuppress-version-warnings", "-Xcontext-parameters")
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

    when (providers.environmentVariable("_TEST_FRAMEWORK_").orNull) {
        "JUnit5", null -> useJUnitPlatform {
            /* Fixtures are executed by the tests themselves and may fail deliberately */
            excludeTags("tests.fixture")
        }
        "JUnit4" -> useJUnit()
    }

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
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)

    testImplementation(kotlin("stdlib", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(kotlin("test-junit", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit4)
}

/* Create synthetic test tasks */
run {
    val junit5TestCompilation = kotlin.target.compilations.create("junit5Tests")

    /* Synthetic tests may use the APIs of this module (e.g. 'DynamicTestSharding') */
    junit5TestCompilation.associateWith(kotlin.target.compilations.getByName("main"))

    tasks.register<Test>("junit5Tests") {
        description = "Synthetic Tests: Used by functional tests to create test build behavior (on junit5)"
        useJUnitPlatform()
        testClassesDirs = junit5TestCompilation.output.classesDirs
        classpath = junit5TestCompilation.runtimeDependencyFiles

        providers.gradleProperty("tests.additionalJvmArgument").orNull?.let { args ->
            jvmArgs(args.split(" "))
        }

        /* Used by 'TestShardingFunctionalTest' to shard all test classes by their methods */
        providers.gradleProperty("tests.shardByMethod").orNull?.let { value ->
            systemProperty("tests.shardByMethod", value)
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
