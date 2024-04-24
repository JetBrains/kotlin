/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.prettyName
import kotlin.test.Test
import kotlin.test.fail

@OptIn(ExperimentalWasmDsl::class)
class MultiplatformIncorrectCompileOnlyDependenciesValidationTest {

    private fun setupKmpProject(
        preApplyCode: Project.() -> Unit = {},
        configure: Project.() -> Unit
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
    fun `when compileOnly dependency is not defined anywhere, expect no warning`() {
        val project = setupKmpProject {}

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    /**
     * The `compileOnly()` warning is only relevant for 'published' compilations.
     *
     * Verify `compileOnly()` dependencies in test sources do not trigger the warning.
     */
    @Test
    fun `when compileOnly dependency is defined in commonTest, expect no warning`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonTest {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined as compileOnly but not api, expect warning`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = project.kotlinToolingDiagnosticsCollector
                .getDiagnosticsForProject(project)

            val actualWarning = diagnostics.assertContainsSingleDiagnostic(IncorrectCompileOnlyDependencyWarning)

            assertContains(
                expected = "A compileOnly dependency is used in targets: Kotlin/JS, Kotlin/Native, Kotlin/Wasm.",
                actual = actualWarning.message,
            )
            // JVM is allowed to have compileOnly dependencies, so verify it's not listed in the warning
            assertNotContains(
                expected = KotlinPlatformType.jvm.prettyName,
                actual = actualWarning.message,
                ignoreCase = true,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:atomicfu:latest.release",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "(source sets: jsMain, linuxX64Main, macosX64Main, mingwX64Main, wasmJsMain, wasmWasiMain)",
                actual = actualWarning.message,
            )
        }
    }

    @Test
    fun `when multiple dependencies are defined as compileOnly but not api, expect single warning, with aggregated dependencies`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                    nativeMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
                        }
                    }
                    jsMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:kotlinx-html:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = project.kotlinToolingDiagnosticsCollector
                .getDiagnosticsForProject(project)

            val actualWarning = diagnostics.assertContainsSingleDiagnostic(IncorrectCompileOnlyDependencyWarning)

            assertContains(
                expected = "- org.jetbrains.kotlinx:atomicfu:latest.release (source sets: jsMain, linuxX64Main, macosX64Main, mingwX64Main, wasmJsMain, wasmWasiMain)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-html:latest.release (source sets: jsMain)",
                actual = actualWarning.message,
            )
            assertContains(
                expected = "- org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release (source sets: linuxX64Main, macosX64Main, mingwX64Main)",
                actual = actualWarning.message,
            )
        }
    }

    @Test
    fun `when commonMain dependency is defined as compileOnly and api, expect no warning`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined as compileOnly in commonMain, and api in target main sources, expect no warning`() {
        val project = setupKmpProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }

                    val allNativeTargets = listOf(
                        jvmMain,
                        jsMain,
                        nativeMain,
                        wasmJsMain,
                        wasmWasiMain,
                    )

                    allNativeTargets.forEach { nativeTarget ->
                        nativeTarget.dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }

    }

    @Test
    fun `when dependency is defined as compileOnly but not api, and kotlin-mpp warning is disabled, expect no warning`() {
        val project = setupKmpProject(
            preApplyCode = {
                propertiesExtension.set("kotlin.suppressGradlePluginWarnings", "IncorrectCompileOnlyDependencyWarning")
            }
        ) {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined in nativeMain as compileOnly but not api, and kotlin-native warning is disabled, expect no warning`() {
        val project = setupKmpProject(
            preApplyCode = {
                propertiesExtension.set("kotlin.native.ignoreIncorrectDependencies", "true")
            }
        ) {
            kotlin {
                sourceSets.apply {
                    nativeMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)

            val deprecatedPropertyWarning = diagnostics.filter { it.id == KotlinToolingDiagnostics.DeprecatedGradleProperties.id }
                .firstOrNull { it.message.contains("kotlin.native.ignoreIncorrectDependencies") }
            if (deprecatedPropertyWarning == null) {
                fail("Expected warning regarding deprecated property `kotlin.native.ignoreIncorrectDependencies`, but found none.")
                // Note for future devs: If this assertion starts failing because the property has been removed,
                // then this entire test can probably be removed.
            }
        }
    }
}
