/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

internal class WasmBoxRunner(
    testServices: TestServices
) : WasmBoxRunnerBase(testServices) {
    override val vmsToCheck: List<WasmVM> =
        listOf(WasmVM.V8, WasmVM.SpiderMonkey, WasmVM.JavaScriptCore)

    override val jsModuleImport: String =
        "let jsModule = await import('./index.mjs');"

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.values.single()
        val outputDirBase = testServices.getWasmTestOutputDirectory()
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val testFileText = originalFile.readText()
        val failsIn = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

        fun writeToFilesAndRunTest(mode: String, result: WasmCompilerResult): List<Throwable> {
            val outputDir = File(outputDirBase, mode)

            outputDir.mkdirs()
            writeCompilationResult(result, outputDir, "index")
            if (debugMode >= DebugMode.DEBUG) {
                val outputPath = outputDir.absolutePath
                println(" ------ $mode Wat  file://$outputPath/index.wat")
                println(" ------ $mode Wasm file://$outputPath/index.wasm")
                println(" ------ $mode JS   file://$outputPath/index.uninstantiated.mjs")
                println(" ------ $mode JS   file://$outputPath/index.mjs")
            }

            val filesToIgnoreInSizeChecks = mutableSetOf<File>()
            val exceptions = saveAdditionalFilesAndRun(
                outputDir = outputDir,
                mark = mode,
                failsIn = failsIn,
                filesToIgnoreInSizeChecks = filesToIgnoreInSizeChecks
            )

            return exceptions + when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, outputDir, filesToIgnoreInSizeChecks)
                "dev" -> emptyList() // no additional checks required
                else -> error("Unknown mode: $mode")
            }
        }

        val allExceptions =
            writeToFilesAndRunTest("dev", artifacts.compilerResult) +
                    writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE) +
                    (artifacts.compilerResultWithOptimizer?.let { writeToFilesAndRunTest("optimized", it) } ?: emptyList())

        processExceptions(allExceptions)
    }
}
