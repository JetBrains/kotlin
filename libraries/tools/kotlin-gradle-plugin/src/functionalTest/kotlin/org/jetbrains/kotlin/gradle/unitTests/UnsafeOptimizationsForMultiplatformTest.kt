/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JS_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_WASM_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildKMPWithAllBackends
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.withType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnsafeOptimizationsForMultiplatformTest {

    @Test
    fun disabledByDefaultForEveryTarget() {
        assertEnabledTargets(
            properties = emptyMap(),
            expectedJvm = false, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jvmPropertyAffectsOnlyJvmTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to true),
            expectedJvm = true, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jsPropertyAffectsOnlyJsTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_JS_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to true),
            expectedJvm = false, expectedJs = true, expectedWasm = false,
        )
    }

    @Test
    fun wasmPropertyAffectsOnlyWasmTasks() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_WASM_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to true),
            expectedJvm = false, expectedJs = false, expectedWasm = true,
        )
    }

    @Test
    fun deprecatedGlobalPropertyStillEnablesEveryTarget() {
        assertEnabledTargets(
            properties = mapOf(KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to true),
            expectedJvm = true, expectedJs = true, expectedWasm = true,
        )
    }

    @Test
    fun perTargetPropertyOverridesDeprecatedGlobalProperty() {
        assertEnabledTargets(
            properties = mapOf(
                KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to true,
                KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to false,
                KOTLIN_JS_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to false,
                KOTLIN_WASM_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION to false,
            ),
            expectedJvm = false, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun deprecatedGlobalPropertySetToTrueIsReportedAsWarning() {
        assertDeprecationReported(propertyValue = "true")
    }

    @Test
    fun deprecatedGlobalPropertySetToFalseIsReportedAsWarning() {
        assertDeprecationReported(propertyValue = "false")
    }

    private fun assertDeprecationReported(propertyValue: String) {
        val project = buildProjectWithJvm(preApplyCode = {
            extraProperties.set(KOTLIN_UNSAFE_MULTIPLATFORM_INCREMENTAL_COMPILATION, propertyValue)
        }).evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties)
    }

    private fun assertEnabledTargets(
        properties: Map<String, Boolean>,
        expectedJvm: Boolean,
        expectedJs: Boolean,
        expectedWasm: Boolean,
    ) {
        val project = buildKMPWithAllBackends(preApplyCode = {
            properties.forEach { (name, value) -> extraProperties.set(name, value.toString()) }
        })

        project.evaluate()

        val jvmTasks = project.tasks.withType<KotlinCompile>().toList()
        val (wasmTasks, jsTasks) = project.tasks.withType<Kotlin2JsCompile>().partition { it.getIsWasmPlatform.get() }
        assertTrue(jvmTasks.isNotEmpty() && jsTasks.isNotEmpty() && wasmTasks.isNotEmpty(), "Missing compile tasks to check")

        val expected = jvmTasks.associate { it.path to expectedJvm } +
                jsTasks.associate { it.path to expectedJs } +
                wasmTasks.associate { it.path to expectedWasm }
        val actual = jvmTasks.associate { it.path to it.enableUnsafeIncrementalCompilationForMultiplatform.get() } +
                (jsTasks + wasmTasks).associate { it.path to it.enableUnsafeIncrementalCompilationForMultiplatform.get() }

        assertEquals(expected, actual)
    }
}
