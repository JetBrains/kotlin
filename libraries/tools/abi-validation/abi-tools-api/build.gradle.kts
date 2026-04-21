@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
    id("test-inputs-check")
}

kotlin {
    explicitApi()
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()

dependencies {
    // remove stdlib dependency from api artifact in order not to affect the dependencies of the user project
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform()
    }
}
