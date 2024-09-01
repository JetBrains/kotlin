/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * This precompiled script plugin is intended to be a temporary solution for KT-70247.
 * It should be removed after a proper resolution is provided.
 * Also, update the mention of this plugin from `gradle.properties`
 */

plugins {
    kotlin("jvm")
}

val projectsUsedInIntelliJKotlinPlugin: Array<String> by rootProject.extra
val kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin: String by rootProject.extra

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        if (project.path !in projectsUsedInIntelliJKotlinPlugin || KotlinVersion.fromVersion(kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin) > KotlinVersion.KOTLIN_2_0) {
            // check the `configureKotlinCompilationOptions` in `common-configurations.gradle.kts` out
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
        }
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}