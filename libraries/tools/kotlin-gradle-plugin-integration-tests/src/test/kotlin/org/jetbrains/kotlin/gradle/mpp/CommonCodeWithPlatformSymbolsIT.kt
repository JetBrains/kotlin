/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Normal behavior is tested by smokes: [org.jetbrains.kotlin.gradle.mpp.smoke.MultiModuleIncrementalCompilationIT]
 *
 * [CommonCodeWithPlatformSymbolsIT] should be removed with the IC option when the underlying compiler issue is fixed
 */

abstract class CommonCodeWithPlatformSymbolsITBase(
    val platformType: KotlinPlatformType,
    val taskToExecute: String,
    val setupBuildScript: GradleProjectBuildScriptInjectionContext.() -> Unit,
) : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LogLevel.DEBUG,
            languageVersion = "2.0",
            enableUnsafeIncrementalCompilationForMultiplatform = false,
            isolatedProjects = when (platformType) {
                KotlinPlatformType.js -> BuildOptions.IsolatedProjectsMode.DISABLED
                else -> super.defaultBuildOptions.isolatedProjects
            }
        )

    private val platformSourceSet = "${platformType.name}Main"

    @GradleTest
    @DisplayName("Baseline - compilation failure without hotfix")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testBaselineForCommonSourceSetIC(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(enableUnsafeIncrementalCompilationForMultiplatform = true)
        ) {
            buildScriptInjection(setupBuildScript)

            // initial build is good
            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            projectPath.resolve("src/commonMain/kotlin/riskyCode.kt").replaceWithVersion("useMemberFunctionFromExpectClass")

            // common source file is recompiled, and the incorrect overload resolution happens:
            buildAndFail(taskToExecute) {
                assertTasksFailed(taskToExecute)
                assertOutputContains("Return type mismatch: expected 'String', actual '${platformSourceSet}OnlyType'.")
            }

            // but changed code is valid for full compilation:
            build("clean", taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Use member function of an expect class that is overloaded in the actual class")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testIncrementalBuildWithOverloadedMemberFunction(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary", gradleVersion
        ) {
            buildScriptInjection(setupBuildScript)

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            projectPath.resolve("src/commonMain/kotlin/riskyCode.kt").replaceWithVersion("useMemberFunctionFromExpectClass")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }

    @GradleTest
    @DisplayName("Use top level function that is overloaded in platform sourceSet")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testIncrementalBuildWithOverloadedTopLevelFunction(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary", gradleVersion
        ) {
            buildScriptInjection(setupBuildScript)

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            projectPath.resolve("src/commonMain/kotlin/riskyCode.kt").replaceWithVersion("useTopLevelFunction")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }

    @GradleTest
    @DisplayName("Touch risky code by change in common sourceSet")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testIncrementalBuildWhereRiskyCodeIsTouchedIndirectly(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary", gradleVersion
        ) {
            buildScriptInjection(setupBuildScript)

            projectPath.resolve("src/commonMain/kotlin/riskyCode.kt").replaceWithVersion("useMemberFunctionFromExpectClass")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            projectPath.resolve("src/commonMain/kotlin/dependedOnByRiskyCode.kt")
                .replaceWithVersion("changeType")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }

    @GradleTest
    @DisplayName("Touch risky code by change in platform sourceSet")
    @TestMetadata("kt-62686-mpp-source-set-boundary")
    fun testIncrementalBuildWhereRiskyCodeIsTouchedByChangeInPlatformSourceSet(gradleVersion: GradleVersion) {
        project(
            "kt-62686-mpp-source-set-boundary", gradleVersion
        ) {
            buildScriptInjection(setupBuildScript)

            projectPath.resolve("src/commonMain/kotlin/riskyCode.kt").replaceWithVersion("useMemberFunctionFromExpectClass")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
            }

            projectPath.resolve("src/$platformSourceSet/kotlin/dependedOnByActualDeclaration.kt")
                .replaceWithVersion("changeType")

            build(taskToExecute) {
                assertTasksExecuted(taskToExecute)
                assertNonIncrementalCompilation(BuildAttribute.UNSAFE_INCREMENTAL_CHANGE_KT_62686)
            }
        }
    }
}

@MppGradlePluginTests
@DisplayName("Tests for IC with compatible overloads in common and platform sourceSets - Jvm")
class CommonCodeWithPlatformSymbolsJvmIT() : CommonCodeWithPlatformSymbolsITBase(
    platformType = KotlinPlatformType.jvm,
    taskToExecute = ":compileKotlinJvm",
    setupBuildScript = {
        kotlinMultiplatform.jvm().compilations.all { compilation ->
            compilation.compileTaskProvider.configure { task ->
                // log level isn't properly used to set `verbose` in the default configuration, fix is WIP in KT-64698
                task.compilerOptions.verbose.convention(true)
            }
        }
    }
)

@MppGradlePluginTests
@DisplayName("Tests for IC with compatible overloads in common and platform sourceSets - Js")
class CommonCodeWithPlatformSymbolsJsIT() : CommonCodeWithPlatformSymbolsITBase(
    platformType = KotlinPlatformType.js,
    taskToExecute = ":compileKotlinJs",
    setupBuildScript = {
        kotlinMultiplatform.js(KotlinJsCompilerType.IR)
    }
)
