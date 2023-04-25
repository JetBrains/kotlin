/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.jvm

/**
 * Controls JVM target validation mode between Kotlin JVM compilation task from this plugin and related Java compilation task from Gradle.
 *
 * See [org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile.jvmTargetValidationMode] for more details.
 *
 * @since 1.9.0
 */
enum class JvmTargetValidationMode {
    /**
     * Ignores JVM target mismatches and proceeds with compilation.
     */
    IGNORE,

    /**
     * Produces a warning message in the console output on JVM target mismatch and proceeds with compilation.
     */
    WARNING,

    /**
     * Throws an exception on JVM target mismatch and stops execution.
     */
    ERROR,
}
