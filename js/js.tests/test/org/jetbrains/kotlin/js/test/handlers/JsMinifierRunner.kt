/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.dce.DeadCodeElimination
import org.jetbrains.kotlin.js.dce.InputFile
import org.jetbrains.kotlin.js.dce.InputResource
import org.jetbrains.kotlin.js.engine.loadFiles
import org.jetbrains.kotlin.js.test.utils.extractTestPackage
import org.jetbrains.kotlin.js.test.utils.getOnlyJsFilesForRunner
import org.jetbrains.kotlin.js.test.utils.getTestModuleName
import org.jetbrains.kotlin.js.test.utils.testWithModuleSystem
import org.jetbrains.kotlin.js.testOld.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsMinifierRunner(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    private val distDirJsPath = "dist/js/"
    private val overwriteReachableNodesProperty = "kotlin.js.overwriteReachableNodes"
    private val overwriteReachableNodes = java.lang.Boolean.getBoolean(overwriteReachableNodesProperty)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val globalDirectives = testServices.moduleStructure.allDirectives
        val dontRunGeneratedCode = globalDirectives[JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE]
            .contains(testServices.defaultsProvider.defaultTargetBackend?.name)
        val esModules = JsEnvironmentConfigurationDirectives.ES_MODULES in globalDirectives

        if (dontRunGeneratedCode || esModules) return

        val allJsFiles = getOnlyJsFilesForRunner(testServices, modulesToArtifact)

        val withModuleSystem = testWithModuleSystem(testServices)
        val testModuleName = getTestModuleName(testServices)
        val testPackage = extractTestPackage(testServices)
        val testFunction = JsBoxRunner.TEST_FUNCTION

        val dontSkipMinification = JsEnvironmentConfigurationDirectives.SKIP_MINIFICATION !in globalDirectives
        val runMinifierByDefault = JsEnvironmentConfigurationDirectives.RUN_MINIFIER_BY_DEFAULT in globalDirectives
        val expectedReachableNodes = globalDirectives[JsEnvironmentConfigurationDirectives.EXPECTED_REACHABLE_NODES].firstOrNull()

        if (dontSkipMinification && (runMinifierByDefault || expectedReachableNodes != null)) {
            val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
            minifyAndRun(
                originalFile,
                expectedReachableNodes,
                workDir = JsEnvironmentConfigurator.getMinificationJsArtifactsOutputDir(testServices),
                allJsFiles = allJsFiles,
                generatedJsFiles = modulesToArtifact.map { it.value.outputFile.absolutePath to it.key.name },
                expectedResult = JsBoxRunner.DEFAULT_EXPECTED_RESULT,
                testModuleName = testModuleName,
                testPackage = testPackage,
                testFunction = testFunction,
                withModuleSystem = withModuleSystem
            )
        }
    }

    private fun minificationThresholdChecker(expectedReachableNodes: Int?, actualReachableNodes: Int, file: File) {
        val fileContent = file.readText()
        val replacement = "// ${JsEnvironmentConfigurationDirectives.EXPECTED_REACHABLE_NODES.name}: $actualReachableNodes"
        val enablingMessage = "To set expected reachable nodes use '$replacement'\n" +
                "To enable automatic overwriting reachable nodes use property '-Pfd.${overwriteReachableNodesProperty}=true'"
        if (expectedReachableNodes == null) {
            val baseMessage = "The number of expected reachable nodes was not set. Actual reachable nodes: $actualReachableNodes."
            return when {
                overwriteReachableNodes -> {
                    file.writeText("$replacement\n$fileContent")
                    throw AssertionError(baseMessage)
                }
                else -> println("$baseMessage\n$enablingMessage")
            }
        }

        val minThreshold = expectedReachableNodes * 9 / 10
        val maxThreshold = expectedReachableNodes * 11 / 10
        if (actualReachableNodes < minThreshold || actualReachableNodes > maxThreshold) {
            val message = "Number of reachable nodes ($actualReachableNodes) does not fit into expected range " +
                    "[$minThreshold; $maxThreshold]"
            val additionalMessage: String =
                if (overwriteReachableNodes) {
                    val oldValue = "// ${JsEnvironmentConfigurationDirectives.EXPECTED_REACHABLE_NODES.name}: $expectedReachableNodes"
                    val newText = fileContent.replaceFirst(oldValue, replacement)
                    file.writeText(newText)
                    ""
                } else {
                    "\n$enablingMessage"
                }

            throw AssertionError("$message$additionalMessage")
        }
    }

    fun minifyAndRun(
        file: File,
        expectedReachableNodes: Int?,
        workDir: File,
        allJsFiles: List<String>,
        generatedJsFiles: List<Pair<String, String>>,
        expectedResult: String,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        withModuleSystem: Boolean
    ) {
        val kotlinJsLib = distDirJsPath + "kotlin.js"
        val kotlinTestJsLib = distDirJsPath + "kotlin-test.js"
        val kotlinJsLibOutput = File(workDir, "kotlin.min.js").path
        val kotlinTestJsLibOutput = File(workDir, "kotlin-test.min.js").path

        val kotlinJsInputFile = InputFile(InputResource.file(kotlinJsLib), null, kotlinJsLibOutput, "kotlin")
        val kotlinTestJsInputFile = InputFile(InputResource.file(kotlinTestJsLib), null, kotlinTestJsLibOutput, "kotlin-test")

        val filesToMinify = generatedJsFiles.associate { (fileName, moduleName) ->
            val inputFileName = File(fileName).nameWithoutExtension
            fileName to InputFile(InputResource.file(fileName), null, File(workDir, inputFileName + ".min.js").absolutePath, moduleName)
        }

        val testFunctionFqn = testModuleName + (if (testPackage.isNullOrEmpty()) "" else ".$testPackage") + ".$testFunction"
        val additionalReachableNodes = setOf(
            testFunctionFqn, "kotlin.kotlin.io.BufferedOutput", "kotlin.kotlin.io.output.flush",
            "kotlin.kotlin.io.output.buffer", "kotlin-test.kotlin.test.overrideAsserter_wbnzx$",
            "kotlin-test.kotlin.test.DefaultAsserter"
        )
        val allFilesToMinify = filesToMinify.values + kotlinJsInputFile + kotlinTestJsInputFile
        val dceResult = DeadCodeElimination.run(allFilesToMinify, additionalReachableNodes, true) { _, _ -> }

        val reachableNodes = dceResult.reachableNodes
        minificationThresholdChecker(expectedReachableNodes, reachableNodes.count { it.reachable }, file)

        val runList = mutableListOf<String>()
        runList += kotlinJsLibOutput
        runList += kotlinTestJsLibOutput
        runList += "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/nashorn-polyfills.js"
        runList += allJsFiles.map { filesToMinify[it]?.outputPath ?: it }

        val engineForMinifier = createScriptEngine()
        val result = engineForMinifier.runAndRestoreContext {
            loadFiles(runList)
            overrideAsserter()
            eval(SETUP_KOTLIN_OUTPUT)
            eval(SETUP_CLASSICAL_BACKEND_FLAG)
            runTestFunction(testModuleName, testPackage, testFunction, withModuleSystem)
        }
        engineForMinifier.release()
        assertions.assertEquals(expectedResult, result)
    }
}