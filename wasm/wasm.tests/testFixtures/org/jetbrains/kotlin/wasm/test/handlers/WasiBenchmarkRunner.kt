/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

class WasiBenchmarkRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    private val vmsToCheck: List<WasmVM> = listOf(WasmVM.ReferenceInterpreter)
    
    companion object {
        private const val BENCHMARK_REPORT_FILE = "benchmark_results.csv"
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmBenchmark()
        }
    }

    private fun runWasmBenchmark() {
        val artifacts = modulesToArtifact.values.single()
        val baseFileName = "index"
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val collectedJsArtifacts = collectJsArtifacts(originalFile)

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        fun writeToFilesAndRunBenchmark(mode: String, res: WasmCompilerResult) {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, baseFileName)

            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)

            if (debugMode >= DebugMode.DEBUG) {
                val path = dir.absolutePath
                println(" ------ $mode Wat  file://$path/index.wat")
                println(" ------ $mode Wasm file://$path/index.wasm")
                println(" ------ $mode JS   file://$path/index.mjs")
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            // Run benchmark and capture output
            val vmsOutputs = mutableMapOf<String, String>()
            
            val exceptions = vmsToCheck.mapNotNull { vm ->
                try {
                    if (debugMode >= DebugMode.DEBUG) {
                        println(" ------ Run benchmark in ${vm.shortName}")
                    }
                    val output = vm.run(
                        entryFile = "$baseFileName.wasm",
                        jsFiles = jsFilePaths,
                        workingDirectory = dir,
                        useNewExceptionHandling = true
                    )
                    
                    // Capture benchmark output
                    vmsOutputs[vm.shortName] = output
                    null
                } catch (e: Throwable) {
                    e
                }
            }

            processExceptions(exceptions)
            
            // Write results to report file
            if (vmsOutputs.isNotEmpty()) {
                writeBenchmarkReport(originalFile, mode, vmsOutputs, debugMode)
            }
        }

        writeToFilesAndRunBenchmark("dev", artifacts.compilerResult)
        writeToFilesAndRunBenchmark("dce", artifacts.compilerResultWithDCE)
        artifacts.compilerResultWithOptimizer?.let { writeToFilesAndRunBenchmark("optimized", it) }
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
