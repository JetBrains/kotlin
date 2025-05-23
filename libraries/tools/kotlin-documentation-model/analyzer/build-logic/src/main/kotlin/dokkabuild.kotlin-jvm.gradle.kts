/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.utils.configureGradleKotlinCompatibility

plugins {
    id("dokkabuild.java")
    kotlin("jvm")
}

val rootProjectsWithoutDependencyOnDokkaCore = listOf("dokka-integration-tests")

configureGradleKotlinCompatibility()

kotlin {
    explicitApi()

    compilerOptions {
        allWarningsAsErrors = true

        // These projects know nothing about the `@InternalDokkaApi` annotation, so the Kotlin compiler
        // will complain about an unresolved opt-in requirement marker and fail the build if it's not excluded.
        if (rootProject.name !in rootProjectsWithoutDependencyOnDokkaCore) {
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "org.jetbrains.dokka.InternalDokkaApi"
            )
        }
    }
}
