/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.PRINT_NPM_SHARED_DEPS_MODE_TASK
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.assertNpmSharedDepsModeIsPerProject
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.realizeAllTasks
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.registerPrintNpmSharedDepsModeTask
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.sharedNpmDependenciesPerProject
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@GradleTestVersions(
    // Temp smoke test until KT-80311. Only check the latest Gradle for faster CI and simpler tests.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
class ProjectIsolationWasmWasiIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.sharedNpmDependenciesPerProject()

    @DisplayName("Composite multi-module WasmWASI project with per-project tooling should be compatible with project isolation")
    @GradleTest
    fun testMultiModuleWasmWasi(gradleVersion: GradleVersion) {
        setupProject(gradleVersion).apply {
            val tasks = arrayOf(
                "help",
                ":shared:$PRINT_NPM_SHARED_DEPS_MODE_TASK",
                ":lib:$PRINT_NPM_SHARED_DEPS_MODE_TASK",
                ":app:$PRINT_NPM_SHARED_DEPS_MODE_TASK",
            )

            build(*tasks) {
                assertConfigurationCacheStored()
                assertNpmSharedDepsModeIsPerProject()
            }

            build(*tasks) {
                assertConfigurationCacheReused()
                assertNpmSharedDepsModeIsPerProject()
            }
        }
    }

    companion object {
        /**
         * A composite build:
         * - included build `shared`: WasmWASI library.
         * - `:lib`: depends on `shared`.
         * - `:app`: depends on `:lib`, Node.js and Wasmtime executable.
         */
        private fun KGPBaseTest.setupProject(gradleVersion: GradleVersion): TestProject {
            val shared = project("empty", gradleVersion) {
                settingsBuildScriptInjection {
                    settings.rootProject.name = "shared"
                }
                addKgpToBuildScriptCompilationClasspath()
                buildScriptInjection {
                    project.registerPrintNpmSharedDepsModeTask()
                    project.realizeAllTasks()
                    project.group = "test"
                    project.version = "1.0"
                    project.applyMultiplatform {
                        wasmWasi { wasmtime() }
                    }
                }
            }

            return project("emptyKts", gradleVersion) {
                includeBuild(shared) {
                    name = "shared"
                }
                plugins {
                    kotlin("multiplatform").apply(false)
                }

                includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib") {
                    buildScriptInjection {
                        project.registerPrintNpmSharedDepsModeTask()
                        project.realizeAllTasks()
                        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                        kotlinMultiplatform.apply {
                            wasmWasi {
                                // Node.js is deprecated for WASI, but its npm-based subtarget is an additional
                                // `jsToolingProject()` path that Wasmtime doesn't cover
                                @Suppress("DEPRECATION")
                                nodejs()
                                wasmtime()
                            }
                            sourceSets.getByName("wasmWasiMain").dependencies {
                                implementation("test:shared:1.0")
                            }
                        }
                    }
                }

                includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "app") {
                    buildScriptInjection {
                        project.registerPrintNpmSharedDepsModeTask()
                        project.realizeAllTasks()
                        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                        kotlinMultiplatform.apply {
                            wasmWasi {
                                @Suppress("DEPRECATION")
                                nodejs()
                                wasmtime()
                                binaries.executable()
                            }
                            sourceSets.getByName("wasmWasiMain").dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    }
                }
            }
        }
    }
}
