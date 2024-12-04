/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage

/**
 * Function used to wrap any checks/assertions done on the current project configuration / project model.
 *
 * Runs the given [check] only on projects that are considered "healthy".
 * A "healthy" project did evaluate correctly which means "without exceptions/errors".
 *
 * This function has to be used over "just running the check", because running project configuration checks
 * on projects that failed to configure will lead to false positive error reporting.
 * In most cases (when called in 'afterEvaluate') such false positive error message will even swallow the real root cause
 * of configuration failure.
 *
 * Note:
 * During Gradle/IDEA sync (import), Gradle will be set into `lenientMode` and will catch all exceptions
 * during evaluation of the build script. Those exceptions will be put into the [ClassPathModeExceptionCollector].
 * Any project that contains caught and collected exceptions in this 'collector' should be considered failed
 * and running project model checks is undesirable. In this mode, throwing exceptions in `afterEvaluate` will even fail the process
 * which would swallow the previously collected exceptions.
 *
 * Example:
 * We have a post-evaluation check that will report users an error if no Kotlin target
 * was registered.
 *
 * Consider the following build script:
 *
 * ```kotlin
 * plugins {
 *      kotlin("multiplatform")
 * }
 *
 * error("Something went wrong during the configuration phase")
 *
 * kotlin {
 *      jvm() // <- * Note: jvm target registered
 *      js()  // <- * Note: js target registered
 * }
 * ```
 *
 * In this example, the exception is thrown before the configuration of Kotlin targets.
 * During IDEA import, this exception will be caught and put into the [ClassPathModeExceptionCollector].
 * When running the assertion just plainly (*without this wrapper function*), the user
 * will not see the real cause of failure, but a rather bizarre:
 * "Please initialize at least one Kotlin target"
 * error message. Which is not helpful at all.
 *
 */
internal inline fun Project.runProjectConfigurationHealthCheck(check: Project.() -> Unit) {
    /* Running configuration checks on a failed project will only lead to false positive error messages */
    if (failures.isNotEmpty()) {
        return
    }

    check()
}


/**
 * Convenience function for
 * ```kotlin
 *  whenEvaluated {
 *      runProjectConfigurationCheck(action)
 *  }
 * ```
 * @see runProjectConfigurationHealthCheck
 */
internal inline fun Project.runProjectConfigurationHealthCheckWhenEvaluated(crossinline check: Project.() -> Unit) {
    launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
        runProjectConfigurationHealthCheck(check)
    }
}
