/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.targets.wasm.WasmCompilationMode
import org.jetbrains.kotlin.gradle.targets.wasm.WasmCompilationMode.Companion.toArgument
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class WasmCompilationModeDiagnosticTest {

    @Test
    fun testInvalidWasmCompilationMode() {
        val project = buildProjectWithMPP {
            project.extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_WASM_COMPILATION_MODE, "foo")
            kotlin {
                wasmJs()
            }
        }.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.WasmCompilationModeInvalidValue)
    }

    @Test
    fun testValidWasmCompilationMode() {
        val project = buildProjectWithMPP {
            project.extraProperties.set(
                PropertiesProvider.PropertyNames.KOTLIN_WASM_COMPILATION_MODE,
                WasmCompilationMode.MULTIMODULE_CLOSED_WORLD_ONLY_IN_DEV.toArgument()
            )
            kotlin {
                wasmJs()
            }
        }.evaluate()

        project.assertNoDiagnostics(KotlinToolingDiagnostics.WasmCompilationModeInvalidValue)
    }
}
