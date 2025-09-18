/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.plugins.JavaTestFixturesPlugin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.TestApiDependencyWarning
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class TestApiDependenciesCheckerTest {

    private fun setupKmpProject(
        preApplyCode: Project.() -> Unit = {},
        configure: Project.() -> Unit,
    ): Project {
        val project = buildProjectWithMPP(preApplyCode = preApplyCode) {
            kotlin {
                jvm()

                linuxX64()
                mingwX64()
                macosX64()

                js { browser() }

                wasmJs { browser() }
                wasmWasi { nodejs() }
            }

            configure()
        }

        return project.evaluate()
    }

    @Test
    fun `when no api dependency is defined in any target, expect no warning`() {
        val project = setupKmpProject {}

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(TestApiDependencyWarning)
        }
    }

    /**
     * The `api()` warning is only relevant for 'test' compilations.
     *
     * Verify `api()` dependencies in main sources do not trigger the warning.
     */
    @Test
    fun `when api dependency is defined in commonMain, expect no warning`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(TestApiDependencyWarning)
        }
    }

    @Test
    fun `when multiple test dependencies are defined as api, expect single warning, with aggregated dependencies`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                    nativeTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
                        }
                    }
                    jsTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:kotlinx-html:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = project.kotlinToolingDiagnosticsCollector
                .getDiagnosticsForProject(project)

            val actualWarning = diagnostics.assertContainsSingleDiagnostic(TestApiDependencyWarning)

            assertContains(
                expected = "- org.jetbrains.kotlinx:atomicfu:latest.release (source sets: jsTest, jvmTest, linuxX64Test, macosX64Test, mingwX64Test, wasmJsTest, wasmWasiTest)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release (source sets: linuxX64Test, macosX64Test, mingwX64Test)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-html:latest.release (source sets: jsTest)",
                actual = actualWarning.message,
            )
        }
    }

    @Test
    fun `when multiple test and main dependencies are defined as api, expect single warning, with aggregated dependencies`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        // also define the dependencies in commonMain, to verify the test API dependencies still trigger warnings.
                        dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                            api("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
                            api("org.jetbrains.kotlinx:kotlinx-html:latest.release")
                        }
                    }
                    commonTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                    nativeTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
                        }
                    }
                    jsTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:kotlinx-html:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = project.kotlinToolingDiagnosticsCollector
                .getDiagnosticsForProject(project)

            val actualWarning = diagnostics.assertContainsSingleDiagnostic(TestApiDependencyWarning)

            assertContains(
                expected = "- org.jetbrains.kotlinx:atomicfu:latest.release (source sets: jsTest, jvmTest, linuxX64Test, macosX64Test, mingwX64Test, wasmJsTest, wasmWasiTest)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release (source sets: linuxX64Test, macosX64Test, mingwX64Test)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-html:latest.release (source sets: jsTest)",
                actual = actualWarning.message,
            )
        }
    }

    @Test
    fun `when commonTest has api dependency, and kotlin-mpp warning is disabled, expect no warning`() {
        val project = setupKmpProject(
            preApplyCode = {
                propertiesExtension.set("kotlin.suppressGradlePluginWarnings", "TestApiDependencyWarning")
            }
        ) {
            kotlin {
                sourceSets.apply {
                    commonTest {
                        dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(TestApiDependencyWarning)
        }
    }

    @Test
    fun `when project has java-test-fixtures, and testFixturesApi has dependency, expect no warning`() {
        val project = setupKmpProject(
            preApplyCode = {
                pluginManager.apply(JavaTestFixturesPlugin::class.java)
            }
        ) {
            dependencies.add("testFixturesApi", "org.jetbrains.kotlinx:atomicfu:latest.release")
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(TestApiDependencyWarning)
        }
    }
}
