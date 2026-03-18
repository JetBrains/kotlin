/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

// TODO reduce amount of duplicated code between this class and WasmBoxRunner
class WasiBoxRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    private val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.values.single() as BinaryArtifacts.Wasm.CompilationSets
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val testWasiQuiet = """
            let boxTestPassed = false;
            try {
                let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
                ${if (startUnitTests) "jsModule.startUnitTests();" else ""}
                boxTestPassed = jsModule.runBoxTest();
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

            res.writeTo(dir, WASM_BASE_FILE_NAME, debugMode)

            File(dir, "test.mjs").writeText(testWasi)
            val collectedJsArtifacts = collectJsArtifacts(originalFile, mode)
            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)
            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ $mode Test file://${dir.absolutePath}/test.mjs")
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(modulesToArtifact.keys.first())
            val useNewExceptionProposal = configuration.getNotNull(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL)

            val exceptions = vmsToCheck.mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    failsIn = failsIn,
                    entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else collectedJsArtifacts.entryPath ?: "test.mjs",
                    jsFilePaths = jsFilePaths,
                    workingDirectory = dir
                )
            }

            processExceptions(exceptions)

            // TODO KT-71504: support size tests for WASI target and ignoring utility files
            val filesToIgnoreInSizeChecks = emptySet<File>()
            when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
            }
        }

        writeToFilesAndRunTest("dev", artifacts.compilation.compilerResult)
        artifacts.dceCompilation?.let {
            writeToFilesAndRunTest("dce", it.compilerResult)
        }
        artifacts.optimisedCompilation?.let {
            writeToFilesAndRunTest("optimized", it.compilerResult)
        }
    }
}
