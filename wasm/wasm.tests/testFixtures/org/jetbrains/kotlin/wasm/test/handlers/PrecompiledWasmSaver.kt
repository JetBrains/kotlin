/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class PrecompiledWasmSaver(
    testServices: TestServices,
) : AbstractWasmArtifactsCollector(testServices) {

    companion object {
        val precompiledOutputDir = File("wasm/wasm.tests/build/out/precompile")
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        super.processModule(module, info)
        val outputDir = File(precompiledOutputDir, info.compilerResult.baseFileName)
        writeCompilationResult(info.compilerResult, outputDir, info.compilerResult.baseFileName, null)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) { }
}