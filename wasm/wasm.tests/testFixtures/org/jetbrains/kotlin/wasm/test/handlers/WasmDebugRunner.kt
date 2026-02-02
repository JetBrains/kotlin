/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

// TODO(review) move WasmDebugRunner{Base,WithPrecompiled} to this file? Multiple files are confusing/not helpful

open class WasmDebugRunner(testServices: TestServices, includeLocalVariableInformation: Boolean = false) :
    WasmDebugRunnerBase(testServices, includeLocalVariableInformation) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val artifacts = modulesToArtifact.values.single()
            val outputDirBase = testServices.getWasmTestOutputDirectory()

            val devDir = File(outputDirBase, "dev")
            devDir.mkdirs()
            writeCompilationResult(artifacts.compilerResult, devDir, "index")
            writeToFilesAndRunTest(outputDir = devDir, sourceMaps = listOf(artifacts.compilerResult.parsedSourceMaps), "index.wasm")

            val dceDir = File(outputDirBase, "dce")
            dceDir.mkdirs()
            writeCompilationResult(artifacts.compilerResultWithDCE, dceDir, "index")
            writeToFilesAndRunTest(outputDir = dceDir, sourceMaps = listOf(artifacts.compilerResultWithDCE.parsedSourceMaps), "index.wasm")
        }
    }
}

class WasmLocalVariableDebugRunner(testServices: TestServices) :
        WasmDebugRunner(testServices, includeLocalVariableInformation = true)