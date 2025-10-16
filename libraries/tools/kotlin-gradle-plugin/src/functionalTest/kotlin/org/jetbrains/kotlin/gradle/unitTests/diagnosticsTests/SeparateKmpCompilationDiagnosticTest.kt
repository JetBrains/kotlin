/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyKotlinJvmPlugin
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnosticsWithMppProject
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.named
import org.junit.Test

/**
 * Verifies we report that the separate KMP compilation scheme is reported as experimental.
 * It does not test the case when the scheme is disabled because it's expected other tests to fail on unexpected diagnostics reported.
 */
class SeparateKmpCompilationDiagnosticTest {
    @Test
    fun testSingleTargetKmpProject() {
        checkDiagnosticsWithMppProject("SeparateKmpCompilation") {
            project.extra[PropertiesProvider.PropertyNames.KOTLIN_KMP_SEPARATE_COMPILATION] = "true"
            kotlin {
                jvm()
            }
            triggerFragmentDependenciesConfigurationLogic("compileKotlinJvm")
        }
    }

    @Test
    fun testMultitargetKmpProjectJvm() = checkSpecificTaskOfMultitargetKmpProject("compileKotlinJvm")

    @Test
    fun testMultitargetKmpProjectJs() = checkSpecificTaskOfMultitargetKmpProject("compileKotlinJs")

    @Test
    fun testMultitargetKmpProjectNative() = checkSpecificTaskOfMultitargetKmpProject("compileKotlinLinuxX64")

    @Test
    fun testMultitargetKmpProjectWasmJs() = checkSpecificTaskOfMultitargetKmpProject("compileKotlinWasmJs")

    @Test
    fun testMultitargetKmpProjectWasmWasi() = checkSpecificTaskOfMultitargetKmpProject("compileKotlinWasmWasi")

    @Test
    fun testMultitargetKmpProjectAndroid() = checkSpecificTaskOfMultitargetKmpProject("compileReleaseKotlinAndroid")

    @Test
    fun testNoDiagnosticInJvm() {
        with(buildProject()) {
            extra[PropertiesProvider.PropertyNames.KOTLIN_KMP_SEPARATE_COMPILATION] = "true"
            applyKotlinJvmPlugin()
            evaluate()
            triggerFragmentDependenciesConfigurationLogic("compileKotlin")
            assertNoDiagnostics()
        }
    }

    private fun checkSpecificTaskOfMultitargetKmpProject(taskName: String) {
        checkDiagnosticsWithMppProject("SeparateKmpCompilation") {
            project.androidLibrary { compileSdk = 33 }
            kotlin {
                project.extra[PropertiesProvider.PropertyNames.KOTLIN_KMP_SEPARATE_COMPILATION] = "true"
                jvm()
                js {
                    nodejs()
                }
                linuxX64()
                @Suppress("DEPRECATION")
                androidTarget()

                @OptIn(ExperimentalWasmDsl::class)
                run {
                    wasmWasi {
                        nodejs()
                    }
                    wasmJs {
                        d8()
                    }
                }
                afterEvaluate {
                    // the android task is task created lately
                    triggerFragmentDependenciesConfigurationLogic(taskName)
                }
            }
        }
    }

    private fun Project.triggerFragmentDependenciesConfigurationLogic(taskName: String) {
        tasks.named<K2MultiplatformCompilationTask>(taskName).get().multiplatformStructure.fragments.get()
    }
}