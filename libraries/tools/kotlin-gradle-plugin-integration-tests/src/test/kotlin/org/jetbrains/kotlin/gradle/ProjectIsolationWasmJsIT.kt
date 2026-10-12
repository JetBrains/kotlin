/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.PRINT_NPM_SHARED_DEPS_MODE_TASK
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.assertNpmSharedDepsModeIsPerProject
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.configurePlaywrightBrowsers
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.realizeAllTasks
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.registerPrintNpmSharedDepsModeTask
import org.jetbrains.kotlin.gradle.ProjectIsolationJsIT.Companion.sharedNpmDependenciesPerProject
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@GradleTestVersions(
    // Temp smoke test until KT-80311. Only check the latest Gradle for faster CI and simpler tests.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
class ProjectIsolationWasmJsIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.sharedNpmDependenciesPerProject()

    @DisplayName("Composite multi-module WasmJS project with per-project npm tooling should be compatible with project isolation")
    @GradleTest
    fun testMultiModuleWasmJs(gradleVersion: GradleVersion) {
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
         * - included build `shared`: WasmJS library for browser, Node.js and D8.
         * - `:lib`: library without a bundler, depends on `shared` and the npm package `is-odd`.
         * - `:app`: webpack executable, depends on `:lib`.
         *
         * `:lib` and `:app` enable every environment and option that changes the project model,
         * including Playwright browser tests, so configuring them exercises as much JS/Wasm tooling as possible.
         */
        @OptIn(
            ExperimentalWasmDsl::class,
            ExperimentalMainFunctionArgumentsDsl::class,
        )
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
                        wasmJs {
                            browser()
                            nodejs()
                            d8()
                        }
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
                            wasmJs {
                                browser(KotlinBrowserBundler.NONE) {
                                    configurePlaywrightBrowsers()
                                }
                                nodejs()
                                d8()
                                binaries.library()
                                generateTypeScriptDefinitions()
                            }
                            sourceSets.getByName("wasmJsMain").dependencies {
                                implementation("test:shared:1.0")
                                implementation(npm("is-odd", "3.0.1"))
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
                            wasmJs {
                                browser {
                                    configurePlaywrightBrowsers()
                                }
                                nodejs {
                                    passProcessArgvToMainFunction()
                                }
                                d8()
                                binaries.executable()
                                generateTypeScriptDefinitions()
                            }
                            sourceSets.getByName("wasmJsMain").dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    }
                }
            }
        }
    }
}
