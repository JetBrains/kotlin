/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
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

        val projectDirBase = System.getProperty("user.dir")

        val stdlibRelativePath = System.getProperty("kotlin.wasm-js.kotlin.stdlib.executable.path") ?: error("precompiled stdlib not found")
        val stdlibPath = File(projectDirBase, stdlibRelativePath)
        val stdlibInitFile = File(stdlibPath, "kotlin-kotlin-stdlib.uninstantiated.mjs")

        val kotlinTestRelativeTestPath = System.getProperty("kotlin.wasm-js.kotlin.test.executable.path") ?: error("precompiled test not found")
        val kotlinTestPath = File(projectDirBase, kotlinTestRelativeTestPath)
        val kotlinTestInitFile = File(kotlinTestPath, "kotlin-kotlin-test.uninstantiated.mjs")

        val testJs = """
                    let actualResult;
                    try {
                        // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                        os.chdir('$stdlibPath');
                        let stdlib = await import('$stdlibInitFile');
                        let stdlibInstantiate = await stdlib.instantiate();
                
                        os.chdir('$kotlinTestPath');
                        let test = await import('$kotlinTestInitFile');
                        let testInstantiate = await test.instantiate({ "<kotlin>": stdlibInstantiate.exports });
                
                        os.chdir('$outputDirBase');
                        let index = await import('./index.uninstantiated.mjs');
                        let indexInstantiate = await index.instantiate({ "<kotlin>": stdlibInstantiate.exports,  "<kotlin-test>": testInstantiate.exports });
                        let jsModule = indexInstantiate.exports;

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

                    if (${debugMode >= DebugMode.DEBUG})
                        console.log('test passed');
                """.trimIndent()

        outputDirBase.mkdirs()
        File(outputDirBase, "test.mjs").writeText(testJs)
        writeCompilationResult(artifacts.compilerResult, outputDirBase, baseFileName)
        val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(outputDirBase)

        if (debugMode >= DebugMode.DEBUG) {
            val path = outputDirBase.absolutePath
            println(" ------ Wat  file://$path/index.wat")
            println(" ------ Wasm file://$path/index.wasm")
            println(" ------ JS   file://$path/index.uninstantiated.mjs")
            println(" ------ Test file://$path/test.mjs")

            for (mjsFile: AdditionalFile in collectedJsArtifacts.mjsFiles) {
                println(" ------ External ESM file://$path/${mjsFile.name}")
            }
        }

        val testFileText = originalFile.readText()
        val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

        val useNewExceptionProposal = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in testServices.moduleStructure.allDirectives

        val exceptions = WasmVM.V8.runWithCaughtExceptions(
            debugMode = debugMode,
            useNewExceptionHandling = useNewExceptionProposal,
            failsIn = failsIn,
            entryFile = collectedJsArtifacts.entryPath,
            jsFilePaths = jsFilePaths,
            workingDirectory = outputDirBase,
        )

        processExceptions(listOfNotNull(exceptions))
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
        run(
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

        val message = "Total size of $extension files in ${testDir.name} dir is $totalSize," +
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