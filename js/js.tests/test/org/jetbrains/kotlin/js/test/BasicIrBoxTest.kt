/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.lang.Boolean.getBoolean

private val fullRuntimeKlib = loadKlib("compiler/ir/serialization.js/build/fullRuntime/klib")
private val defaultRuntimeKlib = loadKlib("compiler/ir/serialization.js/build/reducedRuntime/klib")
private val kotlinTestKLib = loadKlib("compiler/ir/serialization.js/build/kotlin.test/klib")

fun <T> doMeasure(iterations: Int, fn: () -> T): T {
    cleanPsi2IrTimes()
    cleanKlibTimes()
    cleanCompilerTimes()

    val start = System.currentTimeMillis()


    fun printStats(i: Int) {
        val total = System.currentTimeMillis() - start

        fun Long.fmt(): String {
            return "${this * 100 / i / 100.0}"
        }

        println("#${i}")
        println("Total time: ${total.fmt()}")
        println("BE: ${totalBe.fmt()}")
        println()
        println("FE: ${feTime.fmt()}")
        println("Misc: ${miscTime.fmt()}")
        println("Header: ${headerDeserTime.fmt()}")
        println("Psi2Ir: ${psi2IrTime.fmt()}")
        println("Deserializer: ${deserTime.fmt()} + ${additionalDeserialization.fmt()} = ${(deserTime + additionalDeserialization).fmt()}")
        println("Post processing: ${postProcessingTime.fmt()}")
        println("Create context: ${createContextTime.fmt()}")
        println("Compiler prep: ${prepTime.fmt()}")
        println("Lowerings: ${loweringTime.fmt()}")
        println("Ir2Js: ${ir2JsTime.fmt()}")
        println()
        println()
    }

    var result: T? = null
    for (i in 1..iterations) {
        result = fn()

        if (i % 10 == 0) {
            printStats(i)
        }
    }

    printStats(iterations)

    return result!!
}

fun <T> measure(fn: () -> T): T {
    // Warmup
    println("WARMUP")
    println()

    doMeasure(150, fn)

    println("MEASURE")
    return doMeasure(50, fn)
}

abstract class BasicIrBoxTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = TEST_DATA_DIR_PATH,
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
    open val generateDts = false

    override val skipMinification = true

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = false

    private val compilationCache = mutableMapOf<String, String>()

    override fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters, coroutinesPackage: String) {
        compilationCache.clear()
        super.doTest(filePath, expectedResult, mainCallParameters, coroutinesPackage)
    }

    override val testChecker get() = if (runTestInNashorn) NashornIrJsTestChecker() else V8IrJsTestChecker

    @Suppress("ConstantConditionIf")
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
            .filterNot { it.virtualFilePath.contains(COMMON_FILES_DIR_PATH) }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(fullRuntimeKlib)

        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        val allDependencies = runtimeKlibs + transitiveLibraries.map {
            loadKlib(compilationCache[it] ?: error("Can't find compiled module for dependency $it"))
        }

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) it.replace("_v5.js", "/") else it
        }

        if (isMainModule) {
            val debugMode = getBoolean("kotlin.js.debugMode")

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

            val compiledModule = measure {
                compile(
                    project = config.project,
                    files = filesToCompile,
                    configuration = config.configuration,
                    phaseConfig = phaseConfig,
                    allDependencies = allDependencies,
                    friendDependencies = emptyList(),
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction)))
                )
            }

            val wrappedCode = wrapWithModuleEmulationMarkers(compiledModule.jsCode, moduleId = config.moduleId, moduleKind = config.moduleKind)
            outputFile.write(wrappedCode)

            if (generateDts) {
                val dtsFile = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")
                dtsFile?.write(compiledModule.tsDefinitions ?: error("No ts definitions"))
            }

        } else {
            generateKLib(
                project = config.project,
                files = filesToCompile,
                configuration = config.configuration,
                allDependencies = allDependencies,
                friendDependencies = emptyList(),
                outputKlibPath = actualOutputFile,
                nopack = true
            )

            compilationCache[outputFile.name.replace(".js", ".meta.js")] = actualOutputFile
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

        testChecker.check(jsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
    }
}


private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
