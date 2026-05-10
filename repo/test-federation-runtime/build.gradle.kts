@file:OptIn(TemporaryTestFederationApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.testFederation.GenerateTestFederationRuntimeCodeTask
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.isSmokeTest

plugins {
    kotlin("jvm")
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
    providers.environmentVariable("_PSEUDO_TEST_isSmokeTest").orNull?.let {
        isSmokeTest = it.toBoolean()
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


/* Create build test */
val functionalTestCompilation = kotlin.target.compilations.create("functionalTest") {
    associateWith(kotlin.target.compilations.getByName("main"))
}

val functionalTest = tasks.register<Test>("functionalTest") {
    useJUnitPlatform()
    workingDir = project.rootDir
    javaLauncher = getToolchainLauncherFor(JdkMajorVersion.JDK_21_0)
    classpath = project.files(functionalTestCompilation.output.classesDirs, functionalTestCompilation.runtimeDependencyFiles)
    testClassesDirs = functionalTestCompilation.output.classesDirs
    testLogging {
        showStandardStreams = true
    }

    @OptIn(TemporaryTestFederationApi::class)
    isSmokeTest = true

}

dependencies {
    functionalTestCompilation.implementationConfigurationName(kotlin("stdlib"))
    functionalTestCompilation.implementationConfigurationName(kotlin("test-junit5"))
}
