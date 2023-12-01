/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.smoke

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.mpp.KmpIncrementalITBase
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceWithVersion
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Multi-module KMP tests assert that IC is fine, when API changes between two modules, or common/platform parts of a module.
 */
@DisplayName("Basic multi-module incremental scenarios with KMP - K2")
@MppGradlePluginTests
open class MultiModuleIncrementalCompilationIT : KmpIncrementalITBase() {

    /**
     * Tests api change across the module + sourceSet boundary
     */
    @DisplayName("Verify IC builds on change in lib/commonMain")
    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testTouchLibCommon(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        /**
         * Step 1: touch util used in app common
         */
        val usedInAppCommon = resolvePath("lib", "commonMain", "UsedInAppCommon.kt")

        multiStepCheckIncrementalBuilds(
            incrementalPath = usedInAppCommon,
            steps = listOf(
                "1_addUnusedParameter",
                "2_changeReturnType"
            ),
            tasksExpectedToExecuteOnEachStep = mainCompileTasks,
            afterEachStep = {
                assertIncrementalCompilation(
                    listOf(
                        usedInAppCommon,
                        resolvePath("app", "commonMain", "DependsOnLibCommon.kt")
                    ).relativizeTo(projectPath)
                )
            }
        )

        /**
         * Step 2: touch class extended in app platform, then build each platform
         */
        val usedInAppPlatform = resolvePath("lib", "commonMain", "UsedInAppPlatform.kt")
        usedInAppPlatform.replaceWithVersion("1_addNewPublicApi")

        fun testIndividualTarget(moduleTask: String, extraAssertions: BuildResult.() -> Unit = {}) {
            build(":app:$moduleTask", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                val targetTasks = setOf(
                    ":app:$moduleTask",
                    ":lib:$moduleTask"
                )
                assertTasksExecuted(targetTasks)
                assertTasksAreNotInTaskGraph(*(mainCompileTasks - targetTasks).toTypedArray())
                extraAssertions()
            }
        }

        testIndividualTarget("compileKotlinJvm") {
            assertCompiledKotlinSources(
                listOf(
                    usedInAppPlatform,
                    resolvePath("app", "jvmMain", "DependsOnLibCommon.kt")
                ).relativizeTo(projectPath),
                output
            )
        }
        testIndividualTarget("compileKotlinJs") {
            assertIncrementalCompilation(
                listOf(
                    usedInAppPlatform,
                    resolvePath("app", "jsMain", "DependsOnLibCommon.kt")
                ).relativizeTo(projectPath)
            )
        }
        testIndividualTarget("compileKotlinNative")
    }

    /**
     * Three platforms, two steps for each. Do source-compatible changes: first add default parameter,
     * then change return type.
     * lib/platform utils are used in app/platform with deduced return type.
     */
    @DisplayName("Verify IC builds on change in lib/platformMain")
    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testTouchLibPlatform(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        val commonSteps = listOf("1_addUnusedParameter", "2_changeReturnType")

        /**
         * Step 1 - jvm
         */
        val jvmUtil = resolvePath("lib", "jvmMain", "UsedInAppJvmAndLibTests.kt")
        multiStepCheckIncrementalBuilds(
            incrementalPath = jvmUtil,
            steps = commonSteps,
            tasksExpectedToExecuteOnEachStep = setOf(
                ":app:compileKotlinJvm",
                ":lib:compileKotlinJvm"
            ),
            afterEachStep = {
                assertCompiledKotlinSources(
                    expectedSources = listOf(
                        jvmUtil,
                        resolvePath("app", "jvmMain", "DependsOnLibJvm.kt")
                    ).relativizeTo(projectPath),
                    output = output
                )
            }
        )

        /**
         * Step 2 - js
         */
        val jsUtil = resolvePath("lib", "jsMain", "UsedInAppJsAndLibTests.kt")
        multiStepCheckIncrementalBuilds(
            incrementalPath = jsUtil,
            steps = commonSteps,
            tasksExpectedToExecuteOnEachStep = setOf(
                ":app:compileKotlinJs",
                ":lib:compileKotlinJs"
            ),
            afterEachStep = {
                assertIncrementalCompilation(
                    listOf(
                        jsUtil,
                        resolvePath("app", "jsMain", "DependsOnLibJs.kt")
                    ).relativizeTo(projectPath)
                )
            }
        )

        /**
         * Step 3 - native
         */
        val nativeUtil = resolvePath("lib", "nativeMain", "UsedInAppNativeAndLibTests.kt")
        multiStepCheckIncrementalBuilds(
            incrementalPath = nativeUtil,
            steps = commonSteps,
            tasksExpectedToExecuteOnEachStep = setOf(
                ":app:compileKotlinNative",
                ":lib:compileKotlinNative"
            )
        )
    }

    /**
     * Main smoke tests for api changes on the source set boundary
     */
    @DisplayName("Verify IC builds on change in app/commonMain")
    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testTouchAppCommon(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        val utilPath = resolvePath("app", "commonMain", "UsedInAppPlatform.kt")

        multiStepCheckIncrementalBuilds(
            incrementalPath = utilPath,
            steps = listOf(
                "1_changeReturnType",
                "2_addUnusedParameter"
                // test changes in a different order for robustness
            ),
            tasksExpectedToExecuteOnEachStep = setOf(
                ":app:compileCommonMainKotlinMetadata",
                ":app:compileKotlinJvm",
                ":app:compileKotlinJs",
                ":app:compileKotlinNative"
            ),
            afterEachStep = {
                assertIncrementalCompilation(
                    listOf(
                        utilPath,
                        resolvePath("app", "jsMain", "DependsOnAppCommon.kt"),
                        resolvePath("app", "jvmMain", "DependsOnAppCommon.kt")
                    ).relativizeTo(projectPath)
                )
            }
        )
    }


    /**
     * Platform changes in a non-dependency shouldn't affect anything else
     */
    @DisplayName("Verify IC builds on change in app/platformMain")
    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testTouchAppPlatform(gradleVersion: GradleVersion) = withProject(gradleVersion) {
        build("assemble")

        /**
         * Step 1 - jvm
         */
        val changedJvmSource = resolvePath("app", "jvmMain", "UnusedJvm.kt")
            .replaceWithVersion("addParent")
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinJvm")
        ) {
            assertCompiledKotlinSources(listOf(changedJvmSource).relativizeTo(projectPath), output)
        }

        /**
         * Step 2 - js
         */
        val changedJsSource = resolvePath("app", "jsMain", "UnusedJs.kt")
            .replaceWithVersion("addParent")
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinJs")
        ) {
            assertIncrementalCompilation(listOf(changedJsSource).relativizeTo(projectPath))
        }

        /**
         * Step 3 - native
         */
        resolvePath("app", "nativeMain", "UnusedNative.kt")
            .replaceWithVersion("addParent")
        checkIncrementalBuild(
            tasksExpectedToExecute = setOf(":app:compileKotlinNative")
        )
    }
}

@DisplayName("Incremental scenarios with expect/actual - K1")
class MultiModuleIncrementalCompilationK1IT : MultiModuleIncrementalCompilationIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}
