/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.java")
    kotlin("jvm")
}

val rootProjectsWithoutDependencyOnDokkaCore = listOf("dokka-integration-tests")

kotlin {
    explicitApi()
    compilerOptions {
        allWarningsAsErrors = true
        languageVersion = dokkaBuild.kotlinLanguageLevel
        apiVersion = dokkaBuild.kotlinLanguageLevel

        // These projects know nothing about the `@InternalDokkaApi` annotation, so the Kotlin compiler
        // will complain about an unresolved opt-in requirement marker and fail the build if it's not excluded.
        if (rootProject.name !in rootProjectsWithoutDependencyOnDokkaCore) {
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "org.jetbrains.dokka.InternalDokkaApi"
            )
        }

        freeCompilerArgs.addAll(
            // need 1.4 support, otherwise there might be problems
            // with Gradle 6.x (it's bundling Kotlin 1.4)
            "-Xsuppress-version-warnings",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
        )
    }
}
