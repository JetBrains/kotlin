/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.ConfigurationOnDemandNotSupported
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class ConfigurationOnDemandSupportValidationTest {

    @Test
    fun `given kmp project - when configuration on demand is enabled - expect warning for incompatible targets`() {
        val project = buildTestKmpProject(enableConfigurationOnDemand = true)

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            val diagnostic = diagnostics.assertContainsDiagnostic(ConfigurationOnDemandNotSupported)
            assertContains("but root project 'test' has Kotlin targets", diagnostic.message)
            assertContains("Unsupported targets: [js, wasmJs, wasmWasi]", diagnostic.message)
        }
    }

    @Test
    fun `given kmp project - when configuration on demand is disabled - expect NO warning for incompatible targets`() {
        val project = buildTestKmpProject(enableConfigurationOnDemand = false)

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(ConfigurationOnDemandNotSupported)
        }
    }

    @Test
    fun `given js project - when configuration on demand is enabled - expect warning for incompatible targets`() {
        val project = buildTestJsProject(enableConfigurationOnDemand = true)

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            val diagnostic = diagnostics.assertContainsDiagnostic(ConfigurationOnDemandNotSupported)
            assertContains("but root project 'test' has Kotlin targets", diagnostic.message)
            assertContains("Unsupported targets: [js]", diagnostic.message)
        }
    }

    @Test
    fun `given js project - when configuration on demand is disabled - expect NO warning for incompatible targets`() {
        val project = buildTestJsProject(enableConfigurationOnDemand = false)

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(ConfigurationOnDemandNotSupported)
        }
    }


    companion object {
        private fun buildTestKmpProject(
            enableConfigurationOnDemand: Boolean,
        ): Project {
            val project = buildProjectWithMPP(
                preApplyCode = {
                    project.gradle.startParameter.isConfigureOnDemand = enableConfigurationOnDemand
                }
            ) {
                kotlin {
                    enableAllKotlinTargets()
                }
            }
            project.evaluate()
            return project
        }

        private fun buildTestJsProject(
            enableConfigurationOnDemand: Boolean,
        ): Project {
            val project = buildProjectWithJs(
                preApplyCode = {
                    project.gradle.startParameter.isConfigureOnDemand = enableConfigurationOnDemand
                }
            ) {
                extensions.configure(KotlinJsProjectExtension::class.java) { kotlin ->
                    kotlin.js { browser() }
                }
            }
            project.evaluate()
            return project
        }
    }
}
