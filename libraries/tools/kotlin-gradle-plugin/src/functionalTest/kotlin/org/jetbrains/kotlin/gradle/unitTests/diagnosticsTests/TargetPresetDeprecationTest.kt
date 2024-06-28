/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.*

class TargetPresetDeprecationTest {

    @Test
    fun `targetFromPreset usage - emits TargetFromPreset diagnostic`() = checkDiagnostics(
        "PresetDeprecation-targetFromPreset"
    ) {
        targetFromPreset(presets.getByName("jvm"))
    }

    @Test
    fun `targets fromPreset usage - emits FromPreset diagnostic`() = checkDiagnostics(
        "PresetDeprecation-fromPreset"
    ) {
        targets {
            fromPreset(presets.getByName("jvm"), "jvm")
        }
    }

    @Test
    fun `presets createTarget usage - emits CreateTarget diagnostic`() = checkDiagnostics(
        "PresetDeprecation-createTarget"
    ) {
        targets.add(presets.getByName("jvm").createTarget("jvm"))
    }

    @Test
    fun `creating target properly - doesn't emit any diagnostics`() = checkDiagnostics(
        null
    ) {
        jvm()
    }

    private fun checkDiagnostics(
        diagnostic: String?,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ) {
        val project = buildProjectWithMPP {
            kotlin {
                configure()
            }
        }
        project.evaluate()
        if (diagnostic != null) {
            project.checkDiagnostics(diagnostic)
        } else {
            project.assertNoDiagnostics()
        }
    }

}