/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.assertFails

class WasmSourceSetsNotFoundErrorTest {

    @Test
    fun `wasmMain SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("wasmMain")
            .assertContainsDiagnostic(KotlinToolingDiagnostics.WasmSourceSetsNotFoundError("wasmMain"))
    }

    @Test
    fun `wasmTest SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("wasmTest")
            .assertContainsDiagnostic(KotlinToolingDiagnostics.WasmSourceSetsNotFoundError("wasmTest"))
    }

    @Test
    fun `wasmJsMain SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("wasmJsMain")
            .assertNoDiagnostics(KotlinToolingDiagnostics.WasmSourceSetsNotFoundError)
    }

    @Test
    fun `wasmJsTest SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("wasmJsTest")
            .assertNoDiagnostics(KotlinToolingDiagnostics.WasmSourceSetsNotFoundError)
    }

    private fun diagnosticsForTestProjectRequestingMissingSourceSet(sourceSetName: String): List<ToolingDiagnostic> {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
            kotlin.sourceSets.getByName(sourceSetName)
        }

        assertFails { project.evaluate() }
        return project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project).toList()
    }
}
