/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.*

class TargetPresetDeprecationTest {

    @Test
    fun `targetFromPreset usage - emits TargetFromPreset diagnostic`() = checkDiagnostics(
        listOf(KotlinToolingDiagnostics.TargetPresets(KotlinToolingDiagnostics.TargetPresets.API.TargetFromPreset))
    ) {
        targetFromPreset(presets.getByName("jvm"))
    }

    @Test
    fun `targets fromPreset usage - emits FromPreset diagnostic`() = checkDiagnostics(
        listOf(KotlinToolingDiagnostics.TargetPresets(KotlinToolingDiagnostics.TargetPresets.API.FromPreset))
    ) {
        targets {
            fromPreset(presets.getByName("jvm"), "jvm")
        }
    }

    @Test
    fun `presets createTarget usage - emits CreateTarget diagnostic`() = checkDiagnostics(
        listOf(KotlinToolingDiagnostics.TargetPresets(KotlinToolingDiagnostics.TargetPresets.API.CreateTarget))
    ) {
        targets.add(presets.getByName("jvm").createTarget("jvm"))
    }

    @Test
    fun `creating target properly - doesn't emit any diagnostics`() = checkDiagnostics(
        emptyList()
    ) {
        jvm()
    }

    private fun checkDiagnostics(
        diagnostics: List<ToolingDiagnostic>,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ) {
        val project = buildProjectWithMPP {
            kotlin {
                configure()
            }
        }
        project.evaluate()
        assertEquals(
            project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project),
            diagnostics,
        )
    }

}