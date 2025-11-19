/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class WasmDebugRunnerWithPrecompiled(testServices: TestServices) : WasmDebugRunnerBase(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        super.processModule(module, info)
        val outputDir = testServices.getWasmTestOutputDirectory()
        val baseFileName = info.compilerResult.baseFileName
        writeCompilationResult(info.compilerResult, outputDir, baseFileName, null)
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        if (debugMode >= DebugMode.DEBUG) {
            val path = outputDir.absolutePath
            println(" ------ Wat  file://$path/$baseFileName.wat")
            println(" ------ Wasm file://$path/$baseFileName.wasm")
            println(" ------ JS   file://$path/$baseFileName.uninstantiated.mjs")
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val sourceMaps = modulesToArtifact.values.map { it.compilerResult.parsedSourceMaps }
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            writeToFilesAndRunTest(outputDir = outputDirBase, sourceMaps = sourceMaps, "index.wasm")
        }
    }
}