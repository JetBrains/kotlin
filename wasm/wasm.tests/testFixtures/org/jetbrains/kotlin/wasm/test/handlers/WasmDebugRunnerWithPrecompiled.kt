/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class WasmDebugRunnerWithPrecompiled(testServices: TestServices) : WasmDebugRunnerBase(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        require(info is BinaryArtifacts.Wasm.CompilationSets)
        super.processModule(module, info)
        val outputDir = testServices.getWasmTestOutputDirectory()
        val baseFileName = info.compilation.compilerResult.baseFileName
        info.compilation.compilerResult.writeTo(outputDir, baseFileName, debugMode)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val sourceMaps = modulesToArtifact.values.map { (it as BinaryArtifacts.Wasm.CompilationSets).compilation.compilerResult.parsedSourceMaps }
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            writeToFilesAndRunTest(outputDir = outputDirBase, sourceMaps = sourceMaps, "index.wasm")
        }
    }
}