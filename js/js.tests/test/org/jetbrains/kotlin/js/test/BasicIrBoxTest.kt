/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.ir.backend.js.ic.SerializedIcData
import org.jetbrains.kotlin.ir.backend.js.ic.prepareSingleLibraryIcCache
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.lang.Boolean.getBoolean

private val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")
private val defaultRuntimeKlib = System.getProperty("kotlin.js.reduced.stdlib.path")
private val kotlinTestKLib = System.getProperty("kotlin.js.kotlin.test.path")

// TODO Cache on FS (requires bootstrap)
private val predefinedKlibHasIcCache = mutableMapOf<String, SerializedIcData?>(
    File(fullRuntimeKlib).absolutePath to null,
    File(kotlinTestKLib).absolutePath to null,
    File(defaultRuntimeKlib).absolutePath to null
)

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

    private val runIcMode: Boolean = getBoolean("kotlin.js.ir.icMode")

    private val lowerPerModule: Boolean = runIcMode || getBoolean("kotlin.js.ir.lowerPerModule")

    private val klibMainModule: Boolean = false || getBoolean("kotlin.js.ir.klibMainModule")

    override val skipRegularMode: Boolean = getBoolean("kotlin.js.ir.skipRegularMode")

    override val runIrDce: Boolean = getBoolean("kotlin.js.ir.dce", true)

    override val runIrPir: Boolean = !lowerPerModule && getBoolean("kotlin.js.ir.pir", true)

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
        safeExternalBoolean: Boolean,
        safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
    ) {
        val filesToCompile = units.map { (it as TranslationUnit.SourceFile).file }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(defaultRuntimeKlib)

        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        val allKlibPaths = (runtimeKlibs + transitiveLibraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }).map { File(it).absolutePath }.toMutableList()

        val klibPath = outputFile.absolutePath.replace("_v5.js", "/")

        if (isMainModule && klibMainModule) {
            val resolvedLibraries = jsResolveLibraries(allKlibPaths, emptyList(), messageCollectorLogger(MessageCollector.NONE))

            generateKLib(
                project = config.project,
                files = filesToCompile,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                allDependencies = resolvedLibraries,
                friendDependencies = emptyList(),
                irFactory = IrFactoryImpl,
                outputKlibPath = klibPath,
                nopack = true,
                null
            )

            allKlibPaths += File(klibPath).absolutePath
        }

        val resolvedLibraries = jsResolveLibraries(allKlibPaths, emptyList(), messageCollectorLogger(MessageCollector.NONE))

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) klibPath else it
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

            val mainModule = if (!klibMainModule) {
                MainModule.SourceFiles(filesToCompile)
            } else {
                val mainLib = resolvedLibraries.getFullList().find { it.libraryFile.absolutePath == File(klibPath).absolutePath }!!
                MainModule.Klib(mainLib)
            }

            if (!skipRegularMode) {
                val icCache: Map<String, SerializedIcData> = if (!runIcMode) emptyMap() else {
                    val map = mutableMapOf<String, SerializedIcData>()
                    for (klibPath in allKlibPaths) {
                        val icData = predefinedKlibHasIcCache[klibPath] ?: prepareSingleLibraryIcCache(
                            project = project,
                            analyzer = AnalyzerWithCompilerReport(config.configuration),
                            configuration = config.configuration,
                            library = resolvedLibraries.getFullList()
                                .single { it.libraryFile.absolutePath == File(klibPath).absolutePath },
                            dependencies = resolvedLibraries.filterRoots { it.library.libraryFile.absolutePath == File(klibPath).absolutePath },
                            icCache = map
                        )

                        if (klibPath in predefinedKlibHasIcCache) {
                            predefinedKlibHasIcCache[klibPath] = icData
                        }

                        map[klibPath] = icData
                    }

                    map
                }


                val irFactory = if (lowerPerModule) PersistentIrFactory() else IrFactoryImpl
                val compiledModule = compile(
                    project = config.project,
                    mainModule = mainModule,
                    analyzer = AnalyzerWithCompilerReport(config.configuration),
                    configuration = config.configuration,
                    phaseConfig = phaseConfig,
                    irFactory = irFactory,
                    allDependencies = resolvedLibraries,
                    friendDependencies = emptyList(),
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    generateFullJs = true,
                    generateDceJs = runIrDce,
                    es6mode = runEs6Mode,
                    multiModule = splitPerModule || perModule,
                    propertyLazyInitialization = propertyLazyInitialization,
                    lowerPerModule = lowerPerModule,
                    safeExternalBoolean = safeExternalBoolean,
                    safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
                    useStdlibCache = runIcMode,
                    icCache = icCache,
                )

                compiledModule.outputs!!.writeTo(outputFile, config)

                compiledModule.outputsAfterDce?.writeTo(dceOutputFile, config)

                if (generateDts) {
                    val dtsFile = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")!!
                    logger.logFile("Output d.ts", dtsFile)
                    dtsFile.write(compiledModule.tsDefinitions ?: error("No ts definitions"))
                }
            }

            if (runIrPir && !skipDceDriven) {
                compile(
                    project = config.project,
                    mainModule = mainModule,
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
                    propertyLazyInitialization = propertyLazyInitialization,
                    safeExternalBoolean = safeExternalBoolean,
                    safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
                ).outputs!!.writeTo(pirOutputFile, config)
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
                null
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

    private fun CompilationOutputs.writeTo(outputFile: File, config: JsConfig) {
        val wrappedCode =
            wrapWithModuleEmulationMarkers(jsCode, moduleId = config.moduleId, moduleKind = config.moduleKind)
        outputFile.write(wrappedCode)

        val dependencyPaths = mutableListOf<String>()

        dependencies.forEach { (moduleId, outputs) ->
            val wrappedCode = wrapWithModuleEmulationMarkers(outputs.jsCode, config.moduleKind, moduleId)
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
