/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.CompilerOutputSink
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationOptions
import org.jetbrains.kotlin.ir.backend.js.codegen.generateEsModules
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.ic.SerializedIcData
import org.jetbrains.kotlin.ir.backend.js.ic.prepareSingleLibraryIcCache
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
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
        skipEsModules: Boolean,
        skipOldModuleSystems: Boolean,
        splitPerModule: Boolean,
        propertyLazyInitialization: Boolean,
        safeExternalBoolean: Boolean,
        safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
        skipMangleVerification: Boolean,
        abiVersion: KotlinAbiVersion,
        icCache: MutableMap<String, SerializedIcData>,
        customTestModule: String?,
    ) {
        val filesToCompile = units.map { (it as TranslationUnit.SourceFile).file }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(defaultRuntimeKlib)

        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        val allKlibPaths = (runtimeKlibs + transitiveLibraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }).map { File(it).absolutePath }.toMutableList()

        val klibPath = outputFile.absolutePath.replace("_v5.js", "/")

        prepareRuntimePirCaches(config, icCache)

        if (isMainModule && klibMainModule) {
            generateKLib(
                project = config.project,
                files = filesToCompile,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                dependencies = allKlibPaths,
                friendDependencies = emptyList(),
                irFactory = IrFactoryImpl,
                outputKlibPath = klibPath,
                nopack = true,
                jsOutputName = null
            )

            allKlibPaths += File(klibPath).absolutePath
        }

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
                MainModule.Klib(klibPath)
            }

            val mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null }


            fun compileToLoweredIr(dceDriven: Boolean, granularity: JsGenerationGranularity): LoweredIr =
                compile(
                    project = config.project,
                    mainModule = mainModule,
                    analyzer = AnalyzerWithCompilerReport(config.configuration),
                    configuration = config.configuration,
                    phaseConfig = phaseConfig,
                    irFactory = if (lowerPerModule || dceDriven) PersistentIrFactory() else IrFactoryImpl,
                    dependencies = allKlibPaths,
                    friendDependencies = emptyList(),
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    dceDriven = dceDriven,
                    es6mode = runEs6Mode,
                    propertyLazyInitialization = propertyLazyInitialization,
                    lowerPerModule = lowerPerModule,
                    safeExternalBoolean = safeExternalBoolean,
                    safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
                    verifySignatures = !skipMangleVerification,
                    useStdlibCache = runIcMode,
                    icCache = icCache,
                    granularity = granularity
                )


            fun generateTestFile(outputDir: File) {
                val moduleName = config.configuration[CommonConfigurationKeys.MODULE_NAME]
                val esmTestFile = File(outputDir, "test.mjs")
                logger.logFile("ES module test file", esmTestFile)
                val defaultTestModule =
                    """                     
                                import { box } from './${moduleName}/index.js';
                                let res = box();
                                if (res !== "OK") {
                                    throw "Wrong result: " + String(res);
                                }
                                """.trimIndent()

                esmTestFile.writeText(customTestModule ?: defaultTestModule)
            }

            val options = JsGenerationOptions(generatePackageJson = true, generateTypeScriptDefinitions = generateDts)

            fun generateEsModules(ir: LoweredIr, outputDir: File, granularity: JsGenerationGranularity) {
                if (skipEsModules) return
                outputDir.deleteRecursively()
                generateEsModules(ir, jsOutputSink(outputDir), mainArguments = mainArguments, granularity = granularity, options = options)
                generateTestFile(outputDir)
            }

            fun generateOldModuleSystems(
                ir: LoweredIr,
                outputFile: File,
                granularity: JsGenerationGranularity
            ) {
                if (skipOldModuleSystems) return
                outputFile.deleteRecursively()

                check(granularity != PER_FILE) { "Per file granularity is not supported for old module systems" }
                val transformer = IrModuleToJsTransformer(
                    ir.context,
                    mainArguments,
                    fullJs = true,
                    dceJs = false,
                    multiModule = granularity == PER_MODULE,
                    relativeRequirePath = false
                )
                val generatedModule: CompilerResult = transformer.generateModule(ir.allModules)

                generatedModule.outputs!!.writeTo(
                    outputFile,
                    config
                )

                if (generateDts) {
                    val dtsFile = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")!!
                    logger.logFile("Output d.ts", dtsFile)
                    dtsFile.write(generatedModule.tsDefinitions ?: error("No ts definitions"))
                }
            }

            val granularity = if (splitPerModule || perModule) PER_MODULE else WHOLE_PROGRAM

            if (!skipRegularMode) {
                val ir = compileToLoweredIr(dceDriven = false, granularity)
                generateEsModules(ir, outputFile.esModulesSubDir, granularity)
                generateOldModuleSystems(ir, outputFile, granularity)

                if (runIrDce) {
                    eliminateDeadDeclarations(ir.allModules, ir.context)
                    generateEsModules(ir, dceOutputFile.esModulesSubDir, granularity)
                    generateOldModuleSystems(ir, dceOutputFile, granularity)
                }
            }

            if (runIrPir && !skipDceDriven) {
                val ir = compileToLoweredIr(dceDriven = true, granularity)
                generateEsModules(ir, pirOutputFile.esModulesSubDir, granularity)
                generateOldModuleSystems(ir, pirOutputFile, granularity)
            }
        } else {
            generateKLib(
                project = config.project,
                files = filesToCompile,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                dependencies = allKlibPaths,
                friendDependencies = emptyList(),
                irFactory = IrFactoryImpl,
                outputKlibPath = actualOutputFile,
                nopack = true,
                verifySignatures = !skipMangleVerification,
                abiVersion = abiVersion,
                null
            )

            if (runIcMode) {
                icCache[actualOutputFile] = createPirCache(actualOutputFile, allKlibPaths + actualOutputFile, config, icCache)
            }

            logger.logFile("Output klib", File(actualOutputFile))

            compilationCache[outputFile.name.replace(".js", ".meta.js")] = actualOutputFile
        }
    }

    private fun jsOutputSink(perFileOutputDir: File): CompilerOutputSink {
        perFileOutputDir.deleteRecursively()
        perFileOutputDir.mkdirs()

        return object : CompilerOutputSink {
            override fun write(module: String, path: String, content: String) {
                val file = File(File(perFileOutputDir, module), path)
                file.parentFile.mkdirs()
                file.writeText(content)
            }
        }
    }


    private fun createPirCache(path: String, allKlibPaths: Collection<String>, config: JsConfig, icCache: Map<String, SerializedIcData>): SerializedIcData {
        val icData = predefinedKlibHasIcCache[path] ?: prepareSingleLibraryIcCache(
            project = project,
            analyzer = AnalyzerWithCompilerReport(config.configuration),
            configuration = config.configuration,
            libPath = path,
            dependencies = allKlibPaths,
            icCache = icCache
        )

        if (path in predefinedKlibHasIcCache) {
            predefinedKlibHasIcCache[path] = icData
        }

        return icData
    }

    private fun prepareRuntimePirCaches(config: JsConfig, icCache: MutableMap<String, SerializedIcData>) {
        if (!runIcMode) return

        val defaultRuntimePath = File(defaultRuntimeKlib).canonicalPath
        icCache[defaultRuntimePath] = createPirCache(defaultRuntimePath, listOf(defaultRuntimePath), config, icCache)

        val fullRuntimePath = File(fullRuntimeKlib).canonicalPath
        icCache[fullRuntimePath] = createPirCache(fullRuntimePath, listOf(fullRuntimePath), config, icCache)

        val testKlibPath = File(kotlinTestKLib).canonicalPath
        icCache[testKlibPath] = createPirCache(testKlibPath, listOf(fullRuntimePath, testKlibPath), config, icCache)
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
            val moduleWrappedCode = wrapWithModuleEmulationMarkers(outputs.jsCode, config.moduleKind, moduleId)
            val dependencyPath = outputFile.absolutePath.replace("_v5.js", "-${moduleId}_v5.js")
            dependencyPaths += dependencyPath
            File(dependencyPath).write(moduleWrappedCode)
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

val File.esModulesSubDir: File
    get() = File(absolutePath + "_esm")
