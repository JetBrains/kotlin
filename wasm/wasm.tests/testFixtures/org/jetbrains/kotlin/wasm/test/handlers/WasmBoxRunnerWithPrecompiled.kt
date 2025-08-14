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
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.wasm.test.handlers.PrecompiledWasmSaver.Companion.precompileDir
import java.io.File

class WasmBoxRunnerWithPrecompiled(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        super.processModule(module, info)
        val outputDir = testServices.getWasmTestOutputDirectory()
        writeCompilationResult(info.compilerResult, outputDir, info.compilerResult.baseFileName, null)
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        if (debugMode >= DebugMode.DEBUG) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val path = outputDirBase.absolutePath
            println(" ------ Wat  file://$path/${info.compilerResult.baseFileName}.wat")
            println(" ------ Wasm file://$path/${info.compilerResult.baseFileName}.wasm")
            println(" ------ JS   file://$path/${info.compilerResult.baseFileName}.uninstantiated.mjs")
        }
    }


    private fun File.toJsPath(): String =
        if (File.separatorChar == '\\') path.replace("\\", "\\\\") else path

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.entries
            .first { WasmEnvironmentConfigurator.isMainModule(it.key, testServices) }
            .value

        val outputDirBase = testServices.getWasmTestOutputDirectory()
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val collectedJsArtifacts = collectJsArtifacts(originalFile)

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val precompiledDir = testServices.precompileDir()

        val stdlibPath = File(precompiledDir, "_kotlin_")
        val stdlibInitFile = File(stdlibPath, "_kotlin_.uninstantiated.mjs")

        val kotlinTestPath = File(precompiledDir, "_kotlin-test_")
        val kotlinTestInitFile = File(kotlinTestPath, "_kotlin-test_.uninstantiated.mjs")

        val testJs = """
                    let actualResult;
                    try {
                        // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                        os.chdir('${stdlibPath.toJsPath()}');
                        let stdlib = await import('${stdlibInitFile.toJsPath()}');
                        let stdlibInstantiate = await stdlib.instantiate();
                
                        os.chdir('${kotlinTestPath.toJsPath()}');
                        let test = await import('${kotlinTestInitFile.toJsPath()}');
                        let testInstantiate = await test.instantiate({ '<kotlin>': stdlibInstantiate.exports });
                
                        os.chdir('${outputDirBase.toJsPath()}');
                        let index = await import('./${artifacts.compilerResult.baseFileName}.uninstantiated.mjs');
                        let indexInstantiate = await index.instantiate({ '<kotlin>': stdlibInstantiate.exports,  '<kotlin-test>': testInstantiate.exports });
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
        val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(outputDirBase)

        if (debugMode >= DebugMode.DEBUG) {
            val path = outputDirBase.absolutePath
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
