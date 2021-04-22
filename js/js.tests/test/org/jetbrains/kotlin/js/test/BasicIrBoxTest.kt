/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.messageCollectorLogger
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.lang.Boolean.getBoolean

private val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")
private val defaultRuntimeKlib = System.getProperty("kotlin.js.reduced.stdlib.path")
private val kotlinTestKLib = System.getProperty("kotlin.js.kotlin.test.path")

abstract class BasicIrBoxTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    generateSourceMap: Boolean = false,
    generateNodeJsRunner: Boolean = false,
    targetBackend: TargetBackend = TargetBackend.JS_IR
) : BasicBoxTest(
    pathToTestDir,
    testGroupOutputDirPrefix,
    typedArraysEnabled = true,
    generateSourceMap = generateSourceMap,
    generateNodeJsRunner = generateNodeJsRunner,
    targetBackend = targetBackend
) {
    open val generateDts = false

    override val skipMinification = true

    private fun getBoolean(s: String, default: Boolean) = System.getProperty(s)?.let { parseBoolean(it) } ?: default

    override val skipRegularMode: Boolean = getBoolean("kotlin.js.ir.skipRegularMode")

    override val runIrDce: Boolean = getBoolean("kotlin.js.ir.dce", true)

    override val runIrPir: Boolean = getBoolean("kotlin.js.ir.pir", true)

    val runEs6Mode: Boolean = getBoolean("kotlin.js.ir.es6", false)

    val perModule: Boolean = getBoolean("kotlin.js.ir.perModule")

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = false

    private val compilationCache = mutableMapOf<String, String>()

    private val cachedDependencies = mutableMapOf<String, Collection<String>>()

    override fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters) {
        compilationCache.clear()
        cachedDependencies.clear()
        super.doTest(filePath, expectedResult, mainCallParameters)
    }

    override val testChecker get() = if (runTestInNashorn) NashornIrJsTestChecker() else V8IrJsTestChecker

    @Suppress("ConstantConditionIf")
    override fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        dceOutputFile: File,
        pirOutputFile: File,
        config: JsConfig,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        remap: Boolean,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        isMainModule: Boolean,
        skipDceDriven: Boolean,
        splitPerModule: Boolean,
        propertyLazyInitialization: Boolean,
        abiVersion: KotlinAbiVersion
    ) {
        val filesToCompile = units.map { (it as TranslationUnit.SourceFile).file }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(defaultRuntimeKlib)

        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        val allKlibPaths = (runtimeKlibs + transitiveLibraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }).map { File(it).absolutePath }

        val resolvedLibraries = jsResolveLibraries(allKlibPaths, emptyList(), messageCollectorLogger(MessageCollector.NONE))

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) it.replace("_v5.js", "/") else it
        }

        if (isMainModule) {
            logger.logFile("Output JS", outputFile)

            val debugMode = getBoolean("kotlin.js.debugMode")

            val phaseConfig = if (debugMode) {
                val allPhasesSet = jsPhases.toPhaseMap().values.toSet()
                val dumpOutputDir = File(outputFile.parent, outputFile.nameWithoutExtension + "-irdump")
                logger.logFile("Dumping phasesTo", dumpOutputDir)
                PhaseConfig(
                    jsPhases,
                    dumpToDirectory = dumpOutputDir.path,
                    toDumpStateAfter = fromSysPropertyOrAll("kotlin.js.test.phasesToDumpAfter", allPhasesSet),
                    toValidateStateAfter = fromSysPropertyOrAll("kotlin.js.test.phasesToValidateAfter", allPhasesSet),
                    dumpOnlyFqName = null
                )
            } else {
                PhaseConfig(jsPhases)
            }

            if (!skipRegularMode) {
                val compiledModule = compile(
                    project = config.project,
                    mainModule = MainModule.SourceFiles(filesToCompile),
                    analyzer = AnalyzerWithCompilerReport(config.configuration),
                    configuration = config.configuration,
                    phaseConfig = phaseConfig,
                    irFactory = IrFactoryImpl,
                    allDependencies = resolvedLibraries,
                    friendDependencies = emptyList(),
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    generateFullJs = true,
                    generateDceJs = runIrDce,
                    es6mode = runEs6Mode,
                    multiModule = splitPerModule || perModule,
                    propertyLazyInitialization = propertyLazyInitialization,
                )

                compiledModule.jsCode!!.writeTo(outputFile, config)

                compiledModule.dceJsCode?.writeTo(dceOutputFile, config)

                if (generateDts) {
                    val dtsFile = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")!!
                    logger.logFile("Output d.ts", dtsFile)
                    dtsFile.write(compiledModule.tsDefinitions ?: error("No ts definitions"))
                }
            }

            if (runIrPir && !skipDceDriven) {
                compile(
                    project = config.project,
                    mainModule = MainModule.SourceFiles(filesToCompile),
                    analyzer = AnalyzerWithCompilerReport(config.configuration),
                    configuration = config.configuration,
                    phaseConfig = phaseConfig,
                    irFactory = PersistentIrFactory(),
                    allDependencies = resolvedLibraries,
                    friendDependencies = emptyList(),
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    dceDriven = true,
                    es6mode = runEs6Mode,
                    multiModule = splitPerModule || perModule,
                    propertyLazyInitialization = propertyLazyInitialization
                ).jsCode!!.writeTo(pirOutputFile, config)
            }
        } else {
            generateKLib(
                project = config.project,
                files = filesToCompile,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                allDependencies = resolvedLibraries,
                friendDependencies = emptyList(),
                irFactory = IrFactoryImpl,
                outputKlibPath = actualOutputFile,
                nopack = true,
                abiVersion = abiVersion
            )

            logger.logFile("Output klib", File(actualOutputFile))

            compilationCache[outputFile.name.replace(".js", ".meta.js")] = actualOutputFile
        }
    }

    private fun fromSysPropertyOrAll(key: String, all: Set<AnyNamedPhase>): Set<AnyNamedPhase> {
        val phases = System.getProperty(key)?.split(',')?.toSet() ?: emptySet()
        if (phases.isEmpty()) return all

        return all.filter { it.name in phases }.toSet()
    }

    private fun JsCode.writeTo(outputFile: File, config: JsConfig) {
        val wrappedCode =
            wrapWithModuleEmulationMarkers(mainModule, moduleId = config.moduleId, moduleKind = config.moduleKind)
        outputFile.write(wrappedCode)

        val dependencyPaths = mutableListOf<String>()

        dependencies.forEach { (moduleId, code) ->
            val wrappedCode = wrapWithModuleEmulationMarkers(code, config.moduleKind, moduleId)
            val dependencyPath = outputFile.absolutePath.replace("_v5.js", "-${moduleId}_v5.js")
            dependencyPaths += dependencyPath
            File(dependencyPath).write(wrappedCode)
        }

        cachedDependencies[outputFile.absolutePath] = dependencyPaths
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

        val allFiles = jsFiles.flatMap { file -> cachedDependencies[File(file).absolutePath]?.let { deps -> deps + file } ?: listOf(file) }
        testChecker.check(allFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
    }
}


private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
