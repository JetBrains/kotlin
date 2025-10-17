/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.wasm.test.tools.WasmVM

class WasmBoxRunnerWithPrecompiled(
    testServices: TestServices
) : WasmBoxRunnerBase(testServices) {

    override val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        super.processModule(module, info)
        val outputDir = testServices.getWasmTestOutputDirectory()
        writeCompilationResult(info.compilerResult, outputDir, info.compilerResult.baseFileName, null)
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        if (debugMode >= DebugMode.DEBUG) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val path = outputDirBase.absolutePath
            val baseFileName = info.compilerResult.baseFileName
            println(" ------ Wat  file://$path/$baseFileName.wat")
            println(" ------ Wasm file://$path/$baseFileName.wasm")
            println(" ------ JS   file://$path/$baseFileName.uninstantiated.mjs")
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val exceptions = saveAdditionalFilesAndRun(
                outputDir = outputDirBase,
                mark = "single",
                failsIn = emptyList(),
                filesToIgnoreInSizeChecks = mutableSetOf()
            )
            processExceptions(exceptions)
        }
    }
}