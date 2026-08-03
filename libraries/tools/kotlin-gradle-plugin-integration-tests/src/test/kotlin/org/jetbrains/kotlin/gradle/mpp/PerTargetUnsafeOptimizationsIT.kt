/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.AffectedByBuildToolsApi
import org.jetbrains.kotlin.testFederation.AffectedByCompiler
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

/**
 * `enableUnsafeOptimizationsForMultiplatform` is configured per target (KT-87522), so enabling it for one target
 * must not change the incremental compilation behavior of the others.
 *
 * These tests check incremental compilation behavior, not KGP configuration, but the Build Tools API tests do not
 * support KMP compilation scenarios yet. Move them there once they do.
 */
@MppGradlePluginTests
@DisplayName("Per-target unsafe optimizations for KMP incremental compilation")
@AffectedByCompiler
@AffectedByBuildToolsApi
class PerTargetUnsafeOptimizationsIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LogLevel.DEBUG,
            languageVersion = "2.0",
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    @DisplayName("Enabling it for JVM only keeps the whole-module rebuild for JS")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testEnabledForJvmOnly(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                enableJvmUnsafeIncrementalCompilationForMultiplatform = true,
                enableJsUnsafeIncrementalCompilationForMultiplatform = false,
            )
        ) {
            setupJvmAndJsTargets()

            build(JVM_TASK, JS_TASK)
            changeCommonSource()

            build(JVM_TASK) {
                assertIncrementalCompilation()
            }
            build(JS_TASK) {
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }

    @GradleTest
    @DisplayName("Enabling it for JS only keeps the whole-module rebuild for JVM")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testEnabledForJsOnly(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                enableJvmUnsafeIncrementalCompilationForMultiplatform = false,
                enableJsUnsafeIncrementalCompilationForMultiplatform = true,
            )
        ) {
            setupJvmAndJsTargets()

            build(JVM_TASK, JS_TASK)
            changeCommonSource()

            build(JS_TASK) {
                assertIncrementalCompilation()
            }
            build(JVM_TASK) {
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }

    private fun TestProject.setupJvmAndJsTargets() = buildScriptInjection {
        kotlinMultiplatform.js()
        kotlinMultiplatform.jvm().compilations.all { compilation ->
            compilation.compileTaskProvider.configure { task ->
                // log level isn't properly used to set `verbose` in the default configuration, fix is WIP in KT-64698
                task.compilerOptions.verbose.convention(true)
            }
        }
    }

    /** A change in a common source file that is safe to compile incrementally for every target. */
    private fun TestProject.changeCommonSource() {
        projectPath.resolve("src/commonMain/kotlin/dependedOnByRiskyCode.kt")
            .writeText("val dependedOnByRiskyCode = 2\n")
    }

    private companion object {
        const val JVM_TASK = ":compileKotlinJvm"
        const val JS_TASK = ":compileKotlinJs"
    }
}
