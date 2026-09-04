/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WasmCompilationModesTest {
    @Test
    fun `given all compilation sets then modes keep their execution order and payloads`() {
        val modes = wasmCompilationModes("dev payload", "dce payload", "optimized payload")

        assertEquals(
            listOf("dev", "dce", "optimized"),
            modes.map { it.directoryName },
        )
        assertEquals(
            listOf("dev payload", "dce payload", "optimized payload"),
            modes.map { it.compilation },
        )
    }

    @Test
    fun `given optional compilation sets are absent then only available modes are returned`() {
        val modes = wasmCompilationModes("dev payload", dceCompilation = null, optimisedCompilation = null)

        assertEquals(listOf(WasmCompilationMode("dev", "dev payload")), modes)
    }
}
