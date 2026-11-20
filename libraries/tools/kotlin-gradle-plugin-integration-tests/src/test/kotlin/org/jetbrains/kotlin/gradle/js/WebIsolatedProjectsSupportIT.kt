/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.Project
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.js.WebIsolatedProjectsSupportIT.Companion.WRITE_TOOLING_PLUGINS_TASK
import org.jetbrains.kotlin.gradle.js.WebIsolatedProjectsSupportIT.Companion.WRITE_REGISTERED_TASKS_TASK
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimePlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.readLines
import kotlin.test.assertEquals

@JsGradlePluginTests
@GradleTestVersions(
    // Temp smoke test until KT-80311. Only check the latest Gradle for faster CI and simpler tests.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
class WebIsolatedProjectsSupportIT : KGPBaseTest() {

    /**
     * `RootResolver` npm dependency management applies plugins to the root project from subprojects,
     * which Isolated Projects reports as a problem.
     * With `Preview` Isolated Projects support these must be no-ops, so the build is compatible with Isolated Projects.
     *
     * The JS and Wasm tooling plugins (KT-88733) are also checked, because they are configured using the root project.
     */
    @DisplayName("when isolatedProjectsSupport is Preview, expect the root project is not configured from subprojects")
    @GradleTest
    fun testPreviewIsCompatibleWithIsolatedProjects(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            gradleProperties.appendText(
                """
                |kotlin.internal.web.isolatedProjectsSupport=Preview
                |# Enable Swc
                |kotlin.js.delegated.transpilation=true
                |""".trimMargin()
            )

            plugins {
                kotlin("multiplatform").apply(false)
            }

            includeJsAndWasmSubproject("lib-a")
            includeJsAndWasmSubproject("lib-b")

            build(":lib-a:$WRITE_TOOLING_PLUGINS_TASK", ":lib-b:$WRITE_TOOLING_PLUGINS_TASK") {
                assertConfigurationCacheStored()
            }

            val expectedPlugins = listOf("BinaryenPlugin", "D8Plugin", "SwcPlugin", "WasmtimePlugin")
            assertEquals(expectedPlugins, subProject("lib-a").appliedToolingPlugins())
            assertEquals(expectedPlugins, subProject("lib-b").appliedToolingPlugins())
        }
    }

    /**
     * With `Preview` Isolated Projects support, no `@ObsoleteNpmDependencyModel` tasks are registered
     * in the root project or in the subprojects.
     */
    @DisplayName("when isolatedProjectsSupport is Preview, expect no obsolete npm tasks are registered")
    @GradleTest
    fun testPreviewDoesNotRegisterObsoleteNpmTasks(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            gradleProperties.appendText(
                """
                |kotlin.internal.web.isolatedProjectsSupport=Preview
                |""".trimMargin()
            )

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.registerWriteRegisteredTasksTask()
            }

            includeJsAndWasmSubproject("lib-a")
            includeJsAndWasmSubproject("lib-b")

            build(
                ":$WRITE_REGISTERED_TASKS_TASK",
                ":lib-a:$WRITE_REGISTERED_TASKS_TASK",
                ":lib-b:$WRITE_REGISTERED_TASKS_TASK",
            ) {
                assertConfigurationCacheStored()
            }

            assertEquals(emptyList(), registeredObsoleteNpmTasks())
            assertEquals(emptyList(), subProject("lib-a").registeredObsoleteNpmTasks())
            assertEquals(emptyList(), subProject("lib-b").registeredObsoleteNpmTasks())
        }
    }

    companion object {
        private const val WRITE_TOOLING_PLUGINS_TASK = "writeAppliedToolingPlugins"
        private const val TOOLING_PLUGINS_FILE = "applied-tooling-plugins.txt"
        private const val WRITE_REGISTERED_TASKS_TASK = "writeRegisteredTasks"
        private const val REGISTERED_TASKS_FILE = "registered-tasks.txt"

        /**
         * Names of the tasks that `RootResolver` npm dependency management registers for the projects in this test:
         * the subproject `package.json` tasks, and the root project's npm, Yarn and lock file tasks.
         *
         * Node.js, D8 and Binaryen setup tasks are not included, because they are still registered with `Preview`.
         */
        private val obsoleteNpmTaskNames = setOf(
            "jsPackageJson",
            "jsTestPackageJson",
            "kotlinKotlinNpmCachesSetup",
            "kotlinNpmInstall",
            "kotlinPackageJsonUmbrella",
            "kotlinRestorePackageLock",
            "kotlinRestoreYarnLock",
            "kotlinStorePackageLock",
            "kotlinStoreYarnLock",
            "kotlinUpgradePackageLock",
            "kotlinUpgradeYarnLock",
            "kotlinWasmKotlinNpmCachesSetup",
            "kotlinWasmNpmInstall",
            "kotlinWasmPackageJsonUmbrella",
            "kotlinWasmRestorePackageLock",
            "kotlinWasmRestoreYarnLock",
            "kotlinWasmStorePackageLock",
            "kotlinWasmStoreYarnLock",
            "kotlinWasmToolingSetup",
            "kotlinWasmUpgradePackageLock",
            "kotlinWasmUpgradeYarnLock",
            "kotlinWasmYarnSetup",
            "kotlinYarnSetup",
            "rootPackageJson",
            "wasmJsPackageJson",
            "wasmJsTestPackageJson",
            "wasmRootPackageJson",
        )

        /**
         * Include a subproject with every JS and Wasm target and environment,
         * so configuring it exercises as much JS and Wasm tooling as possible.
         */
        private fun TestProject.includeJsAndWasmSubproject(name: String) {
            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = name) {
                buildScriptInjection {
                    project.registerWriteAppliedToolingPluginsTask()
                    project.registerWriteRegisteredTasksTask()
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    @OptIn(ExperimentalWasmDsl::class)
                    kotlinMultiplatform.apply {
                        js {
                            browser()
                            nodejs()
                            binaries.executable()
                        }
                        wasmJs {
                            browser()
                            nodejs()
                            d8()
                            binaries.executable()
                        }
                        wasmWasi {
                            @Suppress("DEPRECATION")
                            nodejs()
                            wasmtime()
                            binaries.executable()
                        }
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(npm("is-even", "1.0.0"))
                        }
                    }
                }
            }
        }

        /** Register a task that writes the tooling plugins applied to this project, so the test can check they were configured. */
        @OptIn(ExperimentalWasmDsl::class)
        private fun Project.registerWriteAppliedToolingPluginsTask() {
            tasks.register(WRITE_TOOLING_PLUGINS_TASK) { task ->
                val appliedToolingPlugins = provider {
                    listOf(
                        BinaryenPlugin::class.java,
                        D8Plugin::class.java,
                        SwcPlugin::class.java,
                        WasmtimePlugin::class.java,
                    )
                        .filter { plugins.hasPlugin(it) }
                        .map { it.simpleName }
                }
                task.inputs.property("appliedToolingPlugins", appliedToolingPlugins)

                val outputFile = layout.buildDirectory.file(TOOLING_PLUGINS_FILE)
                task.outputs.file(outputFile)

                task.doLast {
                    outputFile.get().asFile.writeText(appliedToolingPlugins.get().joinToString("\n"))
                }
            }
        }

        /** The tooling plugins written by [WRITE_TOOLING_PLUGINS_TASK]. */
        private fun GradleProject.appliedToolingPlugins(): List<String> =
            projectPath.resolve("build/$TOOLING_PLUGINS_FILE").readLines()

        /** Register a task that writes the names of all tasks registered in this project, without realizing them. */
        private fun Project.registerWriteRegisteredTasksTask() {
            tasks.register(WRITE_REGISTERED_TASKS_TASK) { task ->
                val registeredTasks = provider { tasks.names.sorted() }
                task.inputs.property("registeredTasks", registeredTasks)

                val outputFile = layout.buildDirectory.file(REGISTERED_TASKS_FILE)
                task.outputs.file(outputFile)

                task.doLast {
                    outputFile.get().asFile.writeText(registeredTasks.get().joinToString("\n"))
                }
            }
        }

        /** The obsolete npm tasks in the task names written by [WRITE_REGISTERED_TASKS_TASK]. */
        private fun GradleProject.registeredObsoleteNpmTasks(): List<String> =
            projectPath.resolve("build/$REGISTERED_TASKS_FILE").readLines()
                .filter { it in obsoleteNpmTaskNames }
    }
}
