/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMap
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.BinaryArtifacts.WasmCompilationSet
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class WasmDebugRunner(testServices: TestServices) : WasmDebugRunnerBase(testServices) {
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
            val artifacts = modulesToArtifact.values.single() as BinaryArtifacts.Wasm.CompilationSets
            processCompilationSet(artifacts.compilation, "dev")
            artifacts.dceCompilation?.let {
                processCompilationSet(it, "dce")
            }
        }
    }
}