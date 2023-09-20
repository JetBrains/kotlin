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
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

class WasmBoxRunner(
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
        val collectedJsArtifacts = collectJsArtifacts(originalFile)

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val testJsQuiet = """
                    let actualResult;
                    try {
                        // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                        let jsModule = await import('./index.mjs');
                        let wasmExports = jsModule.default;
                        ${if (startUnitTests) "wasmExports.startUnitTests();" else ""}
                        actualResult = wasmExports.box();
                    } catch(e) {
                        console.log('Failed with exception!')
                        console.log('Message: ' + e.message)
                        console.log('Name:    ' + e.name)
                        console.log('Stack:')
                        console.log(e.stack)
                    }
    
                    if (actualResult !== "OK")
                        throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;
                """.trimIndent()

        val testJsVerbose = testJsQuiet + """
            
            
                    console.log('test passed');
                """.trimIndent()

        val testJs = if (debugMode >= DebugMode.DEBUG) testJsVerbose else testJsQuiet

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, baseFileName)
            File(dir, "test.mjs").writeText(testJs)

            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)

            if (debugMode >= DebugMode.DEBUG) {
                File(dir, "index.html").writeText(
                    """
                                <!DOCTYPE html>
                                <html lang="en">
                                <body>
                                    <span id="test">UNKNOWN</span>
                                    <script type="module">
                                        let test = document.getElementById("test")
                                        try {
                                            await import("./test.mjs");
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
                val projectName = "kotlin"
                println(" ------ $mode HTML http://0.0.0.0:63342/$projectName/${dir.path}/index.html")
                for (mjsFile: AdditionalFile in collectedJsArtifacts.mjsFiles) {
                    println(" ------ $mode External ESM file://$path/${mjsFile.name}")
                }
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            val disableExceptions = DISABLE_WASM_EXCEPTION_HANDLING in testServices.moduleStructure.allDirectives

            val exceptions = listOf(WasmVM.V8, WasmVM.SpiderMonkey).mapNotNull { vm ->
                vm.runWithCathedExceptions(
                    debugMode = debugMode,
                    disableExceptions = disableExceptions,
                    failsIn = failsIn,
                    entryMjs = collectedJsArtifacts.entryPath,
                    jsFilePaths = jsFilePaths,
                    workingDirectory = dir,
                )
            }

            processExceptions(exceptions)

            if (mode == "dce") {
                checkExpectedOutputSize(debugMode, testFileText, dir)
            }
        }

        writeToFilesAndRunTest("dev", artifacts.compilerResult)
        writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE)
    }
}

internal fun WasmVM.runWithCathedExceptions(
    debugMode: DebugMode,
    disableExceptions: Boolean,
    failsIn: List<String>,
    entryMjs: String?,
    jsFilePaths: List<String>,
    workingDirectory: File,
): Throwable? {
    try {
        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ Run in ${name}" + if (shortName in failsIn) " (expected to fail)" else "")
        }
        run(
            "./${entryMjs}",
            jsFilePaths,
            workingDirectory = workingDirectory,
            disableExceptionHandlingIfPossible = disableExceptions,
        )
        if (shortName in failsIn) {
            return AssertionError("The test expected to fail in ${name}. Please update the testdata.")
        }
    } catch (e: Throwable) {
        if (shortName !in failsIn) {
            return e
        }
    }
    return null
}

fun checkExpectedOutputSize(debugMode: DebugMode, testFileContent: String, testDir: File) {
    val expectedSizes =
        InTextDirectivesUtils.findListWithPrefixes(testFileContent, "// WASM_DCE_EXPECTED_OUTPUT_SIZE: ")
            .map {
                val i = it.indexOf(' ')
                val extension = it.substring(0, i)
                val size = it.substring(i + 1)
                extension.trim().lowercase() to size.filter(Char::isDigit).toInt()
            }

    val filesByExtension = testDir.listFiles()?.groupBy { it.extension }.orEmpty()

    val errors = expectedSizes.mapNotNull { (extension, expectedSize) ->
        val totalSize = filesByExtension[extension].orEmpty().sumOf { it.length() }

        val thresholdPercent = 1
        val thresholdInBytes = expectedSize * thresholdPercent / 100

        val expectedMinSize = expectedSize - thresholdInBytes
        val expectedMaxSize = expectedSize + thresholdInBytes

        val diff = totalSize - expectedSize

        val message = "Total size of $extension files is $totalSize," +
                " but expected $expectedSize ∓ $thresholdInBytes [$expectedMinSize .. $expectedMaxSize]." +
                " Diff: $diff (${diff * 100 / expectedSize}%)"

        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ $message")
        }

        if (totalSize !in expectedMinSize..expectedMaxSize) message else null
    }

    if (errors.isNotEmpty()) throw AssertionError(errors.joinToString("\n"))
}
