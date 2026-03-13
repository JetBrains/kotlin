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
    
    companion object {
        private const val BENCHMARK_REPORT_FILE = "benchmark_results.csv"
    }

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
    
//                    if (actualResult !== "OK")
//                        throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;

                    if (${debugMode >= DebugMode.DEBUG})
                        console.log('test passed');
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

            // Capture benchmark output
            val vmsOutputs = mutableMapOf<String, String>()
            
            val exceptions = vmsToCheck
                .mapNotNull { vm ->
                    try {
                        if (debugMode >= DebugMode.DEBUG) {
                            println(" ------ Run benchmark in ${vm.shortName}")
                        }
                        val output = vm.run(
                            entryFile = "./${collectedJsArtifacts.entryPath}",
                            jsFiles = jsFilePaths,
                            workingDirectory = dir,
                            useNewExceptionHandling = useNewExceptionProposal
                        )
                        
                        // Capture benchmark output
                        vmsOutputs[vm.shortName] = output
                        
                        if (vm.shortName in failsIn) {
                            AssertionError("The test expected to fail in ${vm.javaClass.simpleName}. Please update the testdata.")
                        } else {
                            null
                        }
                    } catch (e: Throwable) {
                        if (vm.shortName !in failsIn) {
                            e
                        } else {
                            null
                        }
                    }
                }
            
            // Write results to report file
            if (vmsOutputs.isNotEmpty()) {
                writeBenchmarkReport(originalFile, mode, vmsOutputs, debugMode)
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
    
    private fun writeBenchmarkReport(testFile: File, mode: String, vmsOutputs: Map<String, String>, debugMode: DebugMode) {
        val projectRoot = File(System.getProperty("user.dir"))
        val reportFile = File(projectRoot, BENCHMARK_REPORT_FILE)
        
        val timestamp = java.time.LocalDateTime.now().toString()
        val testPath = testFile.absolutePath

        if (!reportFile.exists() || reportFile.length() == 0L) {
            reportFile.writeText("Timestamp,Test,Mode,VM,Iterations,Min,Max,Average,Median\n")
        }
        
        vmsOutputs.forEach { (vmName, output) ->
            val iterations = parseValue(output, "Iterations")
            val min = parseValue(output, "Min")
            val max = parseValue(output, "Max")
            val average = parseValue(output, "Average")
            val median = parseValue(output, "Median")
            
            val csvRow = "$timestamp,$testPath,$mode,$vmName,$iterations,$min,$max,$average,$median\n"
            reportFile.appendText(csvRow)
        }
        
        if (debugMode >= DebugMode.DEBUG) {
            println("Benchmark results written to: ${reportFile.absolutePath}")
        }
    }

    private fun parseValue(output: String, label: String): String {
        val regex = Regex("$label: (\\d+)")
        return regex.find(output)?.groupValues?.get(1) ?: ""
    }
}