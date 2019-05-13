/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.ir.backend.js.KlibModuleRef
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

private val fullRuntimeKlib = KlibModuleRef("JS_IR_RUNTIME", "compiler/ir/serialization.js/build/fullRuntime/klib")
private val defaultRuntimeKlib = KlibModuleRef("JS_IR_RUNTIME", "compiler/ir/serialization.js/build/reducedRuntime/klib")
private val kotlinTestKLib = KlibModuleRef("kotlin.test", "compiler/ir/serialization.js/build/kotlin.test/klib")

abstract class BasicIrBoxTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = BasicBoxTest.TEST_DATA_DIR_PATH,
    generateSourceMap: Boolean = false,
    generateNodeJsRunner: Boolean = false
) : BasicBoxTest(
    pathToTestDir,
    testGroupOutputDirPrefix,
    pathToRootOutputDir = pathToRootOutputDir,
    typedArraysEnabled = true,
    generateSourceMap = generateSourceMap,
    generateNodeJsRunner = generateNodeJsRunner,
    targetBackend = TargetBackend.JS_IR
) {

    override val skipMinification = true

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = false

    private val compilationCache = mutableMapOf<String, KlibModuleRef>()

    override fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters, coroutinesPackage: String) {
        compilationCache.clear()
        super.doTest(filePath, expectedResult, mainCallParameters, coroutinesPackage)
    }

    override val testChecker get() = if (runTestInNashorn) NashornIrJsTestChecker() else V8IrJsTestChecker

    override fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        config: JsConfig,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        remap: Boolean,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        isMainModule: Boolean
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            // TODO: split input files to some parts (global common, local common, test)
            .filterNot { it.virtualFilePath.contains(BasicBoxTest.COMMON_FILES_DIR_PATH) }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(defaultRuntimeKlib)

        val libraries = config.configuration[JSConfigurationKeys.LIBRARIES]!!.map { File(it).name }
        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        // TODO: Add proper depencencies
        val dependencies = runtimeKlibs + libraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }

        val allDependencies = runtimeKlibs + transitiveLibraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) it.replace("_v5.js", "/") else it
        }

        if (isMainModule) {
            val debugMode = false

            val phaseConfig = if (debugMode) {
                val allPhasesSet = jsPhases.toPhaseMap().values.toSet()
                val dumpOutputDir = File(outputFile.parent, outputFile.nameWithoutExtension + "-irdump")
                println("\n ------ Dumping phases to file://$dumpOutputDir")
                PhaseConfig(
                    jsPhases,
                    dumpToDirectory = dumpOutputDir.path,
                    toDumpStateAfter = allPhasesSet,
                    toValidateStateAfter = allPhasesSet,
                    dumpOnlyFqName = null
                )
            } else {
                PhaseConfig(jsPhases)
            }

            val jsCode = compile(
                project = config.project,
                files = filesToCompile,
                configuration = config.configuration,
                phaseConfig = phaseConfig,
                immediateDependencies = dependencies,
                allDependencies = allDependencies,
                friendDependencies = emptyList(),
                mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null }
            )

            val wrappedCode = wrapWithModuleEmulationMarkers(jsCode, moduleId = config.moduleId, moduleKind = config.moduleKind)
            outputFile.write(wrappedCode)

        } else {
            val module = generateKLib(
                project = config.project,
                files = filesToCompile,
                configuration = config.configuration,
                immediateDependencies = dependencies,
                allDependencies = allDependencies,
                friendDependencies = emptyList(),
                outputKlibPath = actualOutputFile
            )

            compilationCache[outputFile.name.replace(".js", ".meta.js")] = module
        }
    }

    override fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        // TODO: should we do anything special for module systems?
        // TODO: return list of js from translateFiles and provide then to this function with other js files

        testChecker.check(jsFiles, testModuleName, null, testFunction, expectedResult, withModuleSystem)
    }
}


private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
