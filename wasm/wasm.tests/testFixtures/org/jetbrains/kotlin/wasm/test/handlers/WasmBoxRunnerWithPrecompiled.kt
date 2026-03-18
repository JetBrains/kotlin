/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class WasmBoxRunnerWithPrecompiled(
    testServices: TestServices
) : WasmBoxRunnerBase(testServices) {

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        super.processModule(module, info)
        val outputDir = testServices.getWasmTestOutputDirectory()
        outputDir.mkdirs()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val compilation = (info as BinaryArtifacts.Wasm.CompilationSets).compilation
        compilation.compilerResult.writeTo(outputDir, compilation.compilerResult.baseFileName, debugMode)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val exceptions = saveAdditionalFilesAndRun(
                outputDir = outputDirBase,
                mark = "",
                failsIn = emptyList(),
                filesToIgnoreInSizeChecks = mutableSetOf()
            )
            processExceptions(exceptions)
        }
    }
}