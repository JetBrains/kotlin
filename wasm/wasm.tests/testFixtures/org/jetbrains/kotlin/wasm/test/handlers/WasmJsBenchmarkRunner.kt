/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File
import kotlin.collections.plus


class WasmJsBenchmarkRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    private val vmsToCheck: List<WasmVM> = listOfNotNull(WasmVM.V8)

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
        val collectedJsArtifacts = collectJsArtifacts(originalFile)

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val filesToIgnoreInSizeChecks = mutableSetOf<File>()

        fun File.ignoreInSizeChecks() = also { filesToIgnoreInSizeChecks.add(it) }

        val testJs = """
                    if (globalThis.console == null) {
                        globalThis.console = {};
                    }
                    if (console.log == null) {
                        console.log = print;
                    }
                    let actualResult;
                    try {
                        // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                        let jsModule = await import('./index.mjs');
                        ${if (startUnitTests) "jsModule.startUnitTests();" else ""}
                        actualResult = jsModule.runBenchmark();
                    } catch(e) {
                        console.log('Failed with exception!')

                        if (e instanceof Error) {
                            console.log('Message: ' + e.message)
                            console.log('Name:    ' + e.name)
                            console.log('Stack:')
                            console.log(e.stack)
                        } else {
                            console.log('e: ' + e)
                            console.log('typeof e: ' + typeof e)
                        }
                    }
                """.trimIndent()

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult): List<Throwable> {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, baseFileName)

            File(dir, "test.mjs")
                .ignoreInSizeChecks()
                .writeText(testJs)

            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)

            if (debugMode >= DebugMode.DEBUG) {
                File(dir, "index.html")
                    .ignoreInSizeChecks()
                    .writeText(
                        """
                                <!DOCTYPE html>
                                <html lang="en">
                                <body>
                                    <span id="test">UNKNOWN</span>
                                    <script type="module">
                                        let test = document.getElementById("test")
                                        try {
                                            await import("./${collectedJsArtifacts.entryPath}");
                                            test.style.backgroundColor = "#0f0";
                                            test.textContent = "OK"
                                        } catch(e) {
                                            test.style.backgroundColor = "#f00";
                                            test.textContent = "NOT OK"
                                            throw e;
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                    )

                val path = dir.absolutePath
                println(" ------ $mode Wat  file://$path/index.wat")
                println(" ------ $mode Wasm file://$path/index.wasm")
                println(" ------ $mode JS   file://$path/index.uninstantiated.mjs")
                println(" ------ $mode JS   file://$path/index.mjs")
                println(" ------ $mode Test file://$path/test.mjs")

                val rootStartIndex = path.indexOf("wasm/wasm.tests")
                if (rootStartIndex >= 0) {
                    val pathRelativeToProjectRoot = path.substring(rootStartIndex)
                    val projectName = "kotlin"
                    val baseUrl = System.getProperty("kotlin.wasm.sources.base.url", "http://0.0.0.0:63342/$projectName")
                    println(" ------ $mode HTML $baseUrl/$pathRelativeToProjectRoot/index.html")
                }


                for (mjsFile: AdditionalFile in collectedJsArtifacts.mjsFiles) {
                    println(" ------ $mode External ESM file://$path/${mjsFile.name}")
                }
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            val disableExceptions = DISABLE_WASM_EXCEPTION_HANDLING in testServices.moduleStructure.allDirectives
            val useNewExceptionProposal = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in testServices.moduleStructure.allDirectives

            val exceptions = vmsToCheck
                .mapNotNull { vm ->
                    vm.runWithCaughtExceptions(
                        debugMode = debugMode,
                        useNewExceptionHandling = useNewExceptionProposal,
                        failsIn = failsIn,
                        entryFile = collectedJsArtifacts.entryPath,
                        jsFilePaths = jsFilePaths,
                        workingDirectory = dir,
                    )
                }

            return exceptions + when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
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