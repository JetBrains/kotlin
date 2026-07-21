/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMap
import org.jetbrains.kotlin.test.model.WasmCompilationSet
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

open class WasmDebugRunner(testServices: TestServices, includeLocalVariableInformation: Boolean = false) :
    WasmDebugRunnerBase(testServices, includeLocalVariableInformation) {
    private fun processCompilationSet(compilationSet: WasmCompilationSet, mode: String) {
        val outputDirBase = testServices.getWasmTestOutputDirectory()
        val devDir = File(outputDirBase, mode)
        devDir.mkdirs()

        val sourceMaps = mutableListOf<SourceMap>()

        compilationSet.compilerResult.writeTo(devDir, "index", debugMode, mode)
        sourceMaps.add(compilationSet.compilerResult.parsedSourceMaps)

        compilationSet.compilationDependencies.forEach {
            it.compilerResult.writeTo(devDir, it.compilerResult.baseFileName, debugMode, mode)
            sourceMaps.add(it.compilerResult.parsedSourceMaps)
        }
        writeToFilesAndRunTest(outputDir = devDir, sourceMaps = sourceMaps, "index.wasm")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val artifacts = modulesToArtifact.values.single() as WasmCompilationSetsBinaryArtifact
            // run debug tests only in dev (non-DCE) compilation, as DCE:
            // - is not a supported debug mode in general, and
            // - can slightly degrade source maps, which can lead to different stepping behavior between dev and dce, which doesn't work with unified EXPECTATIONS blocks
            processCompilationSet(artifacts.compilation, "dev")
        }
    }
}

class WasmLocalVariableDebugRunner(testServices: TestServices) :
        WasmDebugRunner(testServices, includeLocalVariableInformation = true)
