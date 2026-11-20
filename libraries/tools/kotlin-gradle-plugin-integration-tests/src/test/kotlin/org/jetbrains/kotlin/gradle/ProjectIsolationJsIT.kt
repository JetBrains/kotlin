/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JsGradlePluginTests
@GradleTestVersions(
    // Temp smoke test until KT-80311. Only check the latest Gradle for faster CI and simpler tests.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
class ProjectIsolationJsIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.sharedNpmDependenciesPerProject()

    @DisplayName("Composite multi-module JS project with per-project npm tooling should be compatible with project isolation")
    @GradleTest
    fun testMultiModuleJs(gradleVersion: GradleVersion) {
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
        internal fun BuildOptions.sharedNpmDependenciesPerProject(): BuildOptions =
            copy(freeArgs = freeArgs + "-Pkotlin.internal.npm.sharedNpmDependenciesProjectMode=PER-PROJECT")

        internal const val PRINT_NPM_SHARED_DEPS_MODE_TASK = "printNpmSharedDepsMode"
        private const val NPM_SHARED_DEPS_MODE_PREFIX = "npmSharedDepsMode: "

        /** Registers a task that prints this project's build tree path and npm shared dependencies mode. */
        internal fun Project.registerPrintNpmSharedDepsModeTask() {
            val buildTreePath = buildTreePath
            tasks.register(PRINT_NPM_SHARED_DEPS_MODE_TASK) { task ->
                val mode = providers.gradleProperty("kotlin.internal.npm.sharedNpmDependenciesProjectMode")
                task.doLast {
                    println("$NPM_SHARED_DEPS_MODE_PREFIX$buildTreePath=${mode.orNull}")
                }
            }
        }

        /** Realise all tasks, so the test can check if their configuration breaks IP. */
        internal fun Project.realizeAllTasks() {
            // Must use afterEvaluate to workaround KT-90483
            afterEvaluate {
                tasks.all { }
            }
        }

        /** Check that PER-PROJECT mode is enabled in all projects. */
        internal fun BuildResult.assertNpmSharedDepsModeIsPerProject() {
            val modes = output.lines()
                .filter { it.startsWith(NPM_SHARED_DEPS_MODE_PREFIX) }
                .associate { it.removePrefix(NPM_SHARED_DEPS_MODE_PREFIX).substringBefore("=") to it.substringAfter("=") }
            assertTrue(modes.isNotEmpty(), "No modes found")
            assertEquals(setOf("PER-PROJECT"), modes.values.toSet(), "Unexpected modes: $modes")
        }

        /** Enable Playwright browser test runners for every supported browser. */
        @OptIn(ExperimentalJsTestDsl::class)
        internal fun KotlinJsBrowserDsl.configurePlaywrightBrowsers() {
            test.apply {
                chromium()
                firefox()
                webkit()
            }
        }

        /**
         * A composite build:
         * - included build `shared`: JS library for browser and Node.js.
         * - `:lib`: ES modules library, depends on `shared` and the npm package `is-odd`.
         * - `:app`: CommonJS executable, depends on `:lib`.
         *
         * `:lib` and `:app` enable every environment and option that changes the project model,
         * including Playwright browser tests, so configuring them exercises as much JS tooling as possible.
         */
        @OptIn(ExperimentalMainFunctionArgumentsDsl::class)
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
                        js {
                            browser()
                            nodejs()
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
                            js {
                                browser {
                                    configurePlaywrightBrowsers()
                                }
                                nodejs()
                                useEsModules()
                                passAsArgumentToMainFunction("process.argv")
                                binaries.library()
                                generateTypeScriptDefinitions()
                            }
                            sourceSets.getByName("jsMain").dependencies {
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
                            js {
                                browser {
                                    configurePlaywrightBrowsers()
                                }
                                nodejs {
                                    passProcessArgvToMainFunction()
                                }
                                useCommonJs()
                                binaries.executable()
                                generateTypeScriptDefinitions()
                            }
                            sourceSets.getByName("jsMain").dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    }
                }
            }
        }
    }
}
