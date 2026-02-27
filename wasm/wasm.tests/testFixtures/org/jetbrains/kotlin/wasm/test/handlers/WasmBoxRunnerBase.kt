/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_NO_JS_TAG
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

abstract class WasmBoxRunnerBase(
    testServices: TestServices,
    executeWithV8Only: Boolean = false,
) : AbstractWasmArtifactsCollector(testServices) {
    private val wasmEngines = if (executeWithV8Only) {
        // JavaScriptCore may glitch on Linux CI: `libglib-2.0.so.0: file too short`
        // however this engine can be avoided for some testrunners like klib compatibility tests,
        // where it's enough to execute an image only on any one of engine, which is the reliable and simple to setup, like V8.
        listOfNotNull(WasmVM.V8)
    } else {
        // KT-82392 [Wasm] Investigate and fix JSC test run on windows
        val jscOfNotWindows = WasmVM.JavaScriptCore.takeIf {
            !System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        }
        listOfNotNull(WasmVM.V8, WasmVM.SpiderMonkey, jscOfNotWindows)
    }

    protected fun saveAdditionalFilesAndRun(
        outputDir: File,
        mark: String,
        failsIn: List<String>,
        filesToIgnoreInSizeChecks: MutableSet<File>
    ): List<Throwable> {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val collectedJsArtifacts = collectJsArtifacts(originalFile, mark)

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        fun File.ignoreInSizeChecks() = also { filesToIgnoreInSizeChecks.add(it) }

        val isNoJsTag = WASM_NO_JS_TAG in testServices.moduleStructure.allDirectives

        val testJs = """
                    ${if (isNoJsTag) "import './tag.mjs'" else ""}
                    import * as jsModule from './$WASM_BASE_FILE_NAME.mjs'
                    if (globalThis.console == null) {
                        globalThis.console = {};
                    }
                    if (console.log == null) {
                        console.log = print;
                    }
                    let actualResult;
                    try {
                        ${if (startUnitTests) "jsModule.startUnitTests();" else ""}
                        actualResult = jsModule.box();
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
    
                    if (actualResult !== "OK")
                        throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;

                    ${if (debugMode >= DebugMode.DEBUG) "console.log('test passed');" else ""}                        
                """.trimIndent()

        if (isNoJsTag) {
            File(outputDir, "tag.mjs")
                .ignoreInSizeChecks()
                .writeText("delete WebAssembly.JSTag;")
        }

        File(outputDir, "test.mjs")
            .ignoreInSizeChecks()
            .writeText(testJs)

        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ $mark Test file://${outputDir.absolutePath}/test.mjs")
        }

        val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(outputDir)

        if (debugMode >= DebugMode.DEBUG) {
            File(outputDir, "index.html")
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

            val outputPath = outputDir.absolutePath
            val rootStartIndex = outputPath.indexOf("wasm/wasm.tests")
            if (rootStartIndex >= 0) {
                val pathRelativeToProjectRoot = outputPath.substring(rootStartIndex)
                val projectName = "kotlin"
                val baseUrl = System.getProperty("kotlin.wasm.sources.base.url", "http://0.0.0.0:63342/$projectName")
                println(" ------ $mark HTML $baseUrl/$pathRelativeToProjectRoot/index.html")
            }


            for (mjsFile: WasmArtifactsCollector.AdditionalFile in collectedJsArtifacts.mjsFiles) {
                println(" ------ $mark External ESM file://$outputPath/${mjsFile.name}")
            }
        }

        val useNewExceptionProposal = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in testServices.moduleStructure.allDirectives

        return wasmEngines
            .mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    failsIn = failsIn,
                    entryFile = collectedJsArtifacts.entryPath,
                    jsFilePaths = jsFilePaths,
                    workingDirectory = outputDir,
                )
            }
    }
}

internal fun WasmVM.runWithCaughtExceptions(
    debugMode: DebugMode,
    useNewExceptionHandling: Boolean,
    failsIn: List<String>,
    entryFile: String?,
    jsFilePaths: List<String>,
    workingDirectory: File,
): Throwable? {
    val vmName = javaClass.simpleName

    try {
        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ Run in $vmName" + if (shortName in failsIn) " (expected to fail)" else "")
        }
        val str = run(
            "./${entryFile}",
            jsFilePaths,
            workingDirectory = workingDirectory,
            useNewExceptionHandling = useNewExceptionHandling,
        )
        if (shortName in failsIn) {
            return AssertionError("The test expected to fail in ${vmName}. Please update the testdata.")
        }
    } catch (e: Throwable) {
        if (shortName !in failsIn) {
            return e
        }
    }
    return null
}

fun checkExpectedDceOutputSize(debugMode: DebugMode, testFileContent: String, testDir: File, filesToIgnore: Set<File>): List<Throwable> {
    val expectedDceSizes =
        InTextDirectivesUtils.findListWithPrefixes(testFileContent, "// WASM_DCE_EXPECTED_OUTPUT_SIZE: ")
            .map {
                val i = it.indexOf(' ')
                val extension = it.substring(0, i)
                val size = it.substring(i + 1)
                extension.trim().lowercase() to size.filter(Char::isDigit).toInt()
            }
    return assertExpectedSizesMatchActual(debugMode, testDir, expectedDceSizes, filesToIgnore)
}

fun checkExpectedOptimizedOutputSize(debugMode: DebugMode, testFileContent: String, testDir: File, filesToIgnore: Set<File>): List<Throwable> {
    val expectedOptimizeSizes = InTextDirectivesUtils
        .findListWithPrefixes(testFileContent, "// WASM_OPT_EXPECTED_OUTPUT_SIZE: ")
        .lastOrNull()
        ?.filter(Char::isDigit)
        ?.toInt() ?: return emptyList()

    return assertExpectedSizesMatchActual(debugMode, testDir, listOf("wasm" to expectedOptimizeSizes), filesToIgnore)
}

private fun assertExpectedSizesMatchActual(
    debugMode: DebugMode,
    testDir: File,
    fileExtensionToItsExpectedSize: Iterable<Pair<String, Int>>,
    filesToIgnore: Set<File>
): List<Throwable> {
    val filesByExtension = testDir.listFiles()?.filterNot { it in filesToIgnore }?.groupBy { it.extension }.orEmpty()

    val errors = fileExtensionToItsExpectedSize.mapNotNull { (extension, expectedSize) ->
        val totalSize = filesByExtension[extension].orEmpty().sumOf { it.length() }

        val thresholdPercent = 1
        val thresholdInBytes = expectedSize * thresholdPercent / 100

        val expectedMinSize = expectedSize - thresholdInBytes
        val expectedMaxSize = expectedSize + thresholdInBytes

        val diff = totalSize - expectedSize

        val message = "Total size of $extension files in ${testDir.name} dir is ${totalSize.toFormattedString()}," +
                " but expected $expectedSize ∓ $thresholdInBytes [$expectedMinSize .. $expectedMaxSize]." +
                " Diff: $diff (${diff * 100 / expectedSize}%)"

        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ $message")
        }

        if (totalSize !in expectedMinSize..expectedMaxSize) message else null
    }

    if (errors.isNotEmpty()) return listOf(AssertionError(errors.joinToString("\n")))

    return emptyList()
}

private fun Long.toFormattedString(): String {
    return this.toString().reversed().chunked(3).joinToString("_").reversed()
}
