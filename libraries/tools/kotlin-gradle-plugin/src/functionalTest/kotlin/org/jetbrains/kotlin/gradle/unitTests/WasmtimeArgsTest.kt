/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WasmtimeArgsTest {

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun checkAdditionalWasmtimeRunArgsAreAppended() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime {
                        wasmtimeRunArgs.add("--dir=.")
                    }
                    binaries.executable()
                }
            }
        }.evaluate()

        val expected = listOf("--dir=.")
        assertEquals(expected, project.wasmtimeRunArgs(DEVELOPMENT_RUN_TASK))
        assertEquals(expected, project.wasmtimeRunArgs(PRODUCTION_RUN_TASK))
    }

    @OptIn(ExperimentalWasmDsl::class)
    private fun Project.wasmtimeRunArgs(taskName: String): List<String> =
        tasks.named(taskName, WasmtimeExec::class.java).get().wasmtimeArgs.get()

    private companion object {
        const val DEVELOPMENT_RUN_TASK = "wasmWasiWasmtimeDevelopmentRun"
        const val PRODUCTION_RUN_TASK = "wasmWasiWasmtimeProductionRun"
    }
}
