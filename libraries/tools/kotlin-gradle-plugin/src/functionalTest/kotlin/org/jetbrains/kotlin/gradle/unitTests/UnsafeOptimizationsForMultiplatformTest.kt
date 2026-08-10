/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

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
            enabledProperty = null,
            expectedJvm = false, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jvmPropertyAffectsOnlyJvmTasks() {
        assertEnabledTargets(
            enabledProperty = "kotlin.internal.jvm.enableUnsafeOptimizationsForMultiplatform",
            expectedJvm = true, expectedJs = false, expectedWasm = false,
        )
    }

    @Test
    fun jsPropertyAffectsOnlyJsTasks() {
        assertEnabledTargets(
            enabledProperty = "kotlin.internal.js.enableUnsafeOptimizationsForMultiplatform",
            expectedJvm = false, expectedJs = true, expectedWasm = false,
        )
    }

    @Test
    fun wasmPropertyAffectsOnlyWasmTasks() {
        assertEnabledTargets(
            enabledProperty = "kotlin.internal.wasm.enableUnsafeOptimizationsForMultiplatform",
            expectedJvm = false, expectedJs = false, expectedWasm = true,
        )
    }

    @Test
    fun deprecatedGlobalPropertySetToTrueIsReportedAsError() {
        assertDeprecationReported(propertyValue = "true")
    }

    @Test
    fun deprecatedGlobalPropertySetToFalseIsReportedAsError() {
        assertDeprecationReported(propertyValue = "false")
    }

    private fun assertDeprecationReported(propertyValue: String) {
        val project = buildProjectWithJvm(preApplyCode = {
            extraProperties.set("kotlin.internal.incremental.enableUnsafeOptimizationsForMultiplatform", propertyValue)
        }).evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedErrorGradleProperties)
    }

    private fun assertEnabledTargets(
        enabledProperty: String?,
        expectedJvm: Boolean,
        expectedJs: Boolean,
        expectedWasm: Boolean,
    ) {
        val project = buildKMPWithAllBackends(preApplyCode = {
            if (enabledProperty != null) extraProperties.set(enabledProperty, "true")
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
