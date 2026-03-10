/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class WasmJsBenchmarkRunner(
    testServices: TestServices
) : WasmBoxRunner(testServices, true, "runBenchmark")

class WasmWasiBenchmarkRunner(
    testServices: TestServices
) : WasiBoxRunner(testServices, "runBenchmark") {
    override fun saveOutput(output: String, dir: File) {
        File(dir, "result.txt").writeText(output)
    }
}