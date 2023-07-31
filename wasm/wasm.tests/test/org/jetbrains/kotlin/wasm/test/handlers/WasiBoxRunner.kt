/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

class WasiBoxRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.values.single()
        val baseFileName = "index"
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val testWasiQuiet = """
            let boxTestPassed = false;
            try {
                let jsModule = await import('./index.mjs');
                let wasmExports = jsModule.default;
                ${if (startUnitTests) "wasmExports.startUnitTests();" else ""}
                boxTestPassed = wasmExports.runBoxTest();
            } catch(e) {
                console.log('Failed with exception!');
                console.log(e);
            }

            if (!boxTestPassed)
                process.exit(1);
            """.trimIndent()

        val testWasiVerbose = testWasiQuiet + """
            
            
                    console.log('test passed');
                """.trimIndent()

        val testWasi = if (debugMode >= DebugMode.DEBUG) testWasiVerbose else testWasiQuiet

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, baseFileName)
            File(dir, "test.mjs").writeText(testWasi)

            if (debugMode >= DebugMode.DEBUG) {
                val path = dir.absolutePath
                println(" ------ $mode Wat  file://$path/index.wat")
                println(" ------ $mode Wasm file://$path/index.wasm")
                println(" ------ $mode JS   file://$path/index.mjs")
                println(" ------ $mode Test file://$path/test.mjs")
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            val exception = WasmVM.NodeJs.runWithCathedExceptions(
                debugMode = debugMode,
                disableExceptions = false,
                failsIn = failsIn,
                entryMjs = "test.mjs",
                jsFilePaths = emptyList(),
                workingDirectory = dir
            )

            if (exception != null) {
                throw exception
            }

            if (mode == "dce") {
                checkExpectedOutputSize(debugMode, testFileText, dir)
            }
        }

        writeToFilesAndRunTest("dev", artifacts.compilerResult)
        writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE)
    }
}