/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
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
private val predefinedKlibHasIcCache = mutableMapOf<String, ICCache?>(
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

    private val klibMainModule: Boolean = getBoolean("kotlin.js.ir.klibMainModule")

    override val skipRegularMode: Boolean = getBoolean("kotlin.js.ir.skipRegularMode")

    override val runIrDce: Boolean = getBoolean("kotlin.js.ir.dce", true)

    override val runIrPir: Boolean = !lowerPerModule && getBoolean("kotlin.js.ir.pir", true)

    val runEs6Mode: Boolean = getBoolean("kotlin.js.ir.es6", false)

    val perModule: Boolean = getBoolean("kotlin.js.ir.perModule")

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = true

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
        splitPerModule: Boolean,
        propertyLazyInitialization: Boolean,
        safeExternalBoolean: Boolean,
        safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
        skipMangleVerification: Boolean,
        abiVersion: KotlinAbiVersion,
        incrementalCompilation: Boolean,
        recompile: Boolean,
        icCache: MutableMap<String, ICCache>
    ) {
        val filesToCompile = units.mapNotNull { (it as? TranslationUnit.SourceFile)?.file }

        val runtimeKlibs = if (needsFullIrRuntime) listOf(fullRuntimeKlib, kotlinTestKLib) else listOf(defaultRuntimeKlib)

        val transitiveLibraries = config.configuration[JSConfigurationKeys.TRANSITIVE_LIBRARIES]!!.map { File(it).name }

        val allKlibPaths = (runtimeKlibs + transitiveLibraries.map {
            compilationCache[it] ?: error("Can't find compiled module for dependency $it")
        }).map { File(it).absolutePath }.toMutableList()

        val klibPath = outputFile.canonicalPath.replace("_v5.js", "")

        val actualOutputFile = outputFile.canonicalPath.let {
            if (!isMainModule) klibPath else it
        }

        if (incrementalCompilationChecksEnabled && incrementalCompilation) {
            runtimeKlibs.forEach { createIcCache(it, runtimeKlibs, config, icCache) }
        }

        if (!recompile) { // In case of incremental recompilation we only rebuild caches, not klib itself
            val module = prepareAnalyzedSourceModule(
                config.project,
                filesToCompile,
                config.configuration,
                allKlibPaths,
                emptyList(),
                AnalyzerWithCompilerReport(config.configuration),
            )
            generateKLib(
                module,
                irFactory = IrFactoryImpl,
                outputKlibPath = klibPath,
                nopack = true,
                jsOutputName = null
            )
        }

        val klibCannonPath = File(klibPath).canonicalPath

        if (incrementalCompilation) {
            icCache[klibCannonPath] = createIcCache(klibCannonPath, allKlibPaths + klibCannonPath, config, icCache)
        }

        compilationCache[outputFile.name.replace(".js", ".meta.js")] = actualOutputFile

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

            val module = ModulesStructure(
                config.project,
                MainModule.Klib(klibPath),
                config.configuration,
                allKlibPaths + klibCannonPath,
                emptyList(),
                icUseGlobalSignatures = runIcMode,
                icUseStdlibCache = runIcMode,
                icCache = icCache
            )

            if (!skipRegularMode) {
                val compiledModule = compile(
                    module,
                    phaseConfig = phaseConfig,
                    irFactory = IrFactoryImpl,
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    generateFullJs = true,
                    generateDceJs = runIrDce,
                    dceDriven = false,
                    es6mode = runEs6Mode,
                    multiModule = splitPerModule || perModule,
                    propertyLazyInitialization = propertyLazyInitialization,
                    lowerPerModule = lowerPerModule,
                    safeExternalBoolean = safeExternalBoolean,
                    safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
                    verifySignatures = !skipMangleVerification,
                )

                val jsOutputFile = if (recompile) File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")
                else outputFile

                compiledModule.outputs!!.writeTo(jsOutputFile, config)

                compiledModule.outputsAfterDce?.writeTo(dceOutputFile, config)

                if (generateDts) {
                    val dtsFile = outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")!!
                    logger.logFile("Output d.ts", dtsFile)
                    dtsFile.write(compiledModule.tsDefinitions ?: error("No ts definitions"))
                }
            }

            if (runIrPir) {
                val compiledModule = compile(
                    module,
                    phaseConfig = phaseConfig,
                    irFactory = PersistentIrFactory(),
                    mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                    exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction))),
                    generateFullJs = true,
                    generateDceJs = runIrDce,
                    dceDriven = true,
                    es6mode = runEs6Mode,
                    multiModule = splitPerModule || perModule,
                    propertyLazyInitialization = propertyLazyInitialization,
                    lowerPerModule = lowerPerModule,
                    safeExternalBoolean = safeExternalBoolean,
                    safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
                    verifySignatures = !skipMangleVerification,
                )
                compiledModule.outputs!!.writeTo(pirOutputFile, config)
            }
        }
    }

    override fun checkIncrementalCompilation(
        sourceDirs: List<String>,
        module: TestModule,
        kotlinFiles: List<TestFile>,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean,
        tmpDir: File,
        remap: Boolean,
        outputFile: File,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        expectActualLinker: Boolean,
        icCaches: MutableMap<String, ICCache>
    ) {
        val isMainModule = module.name == DEFAULT_MODULE || module.name == TEST_MODULE
        val cacheKey = outputFile.canonicalPath.replace("_v5.js", "")

        val icCache = icCaches[cacheKey] ?: error("No IC data found for module ${module.name}")

        val dirtyFiles = mutableListOf<String>()
        val oldBinaryAsts = mutableMapOf<String, ByteArray>()

        for (testFile in kotlinFiles) {
            if (testFile.recompile) {
                val fileName = testFile.fileName
                oldBinaryAsts[fileName] = icCache.dataProvider.binaryAst(fileName)
                icCache.dataConsumer.invalidateForFile(fileName)
                dirtyFiles.add(fileName)
            }
        }

        val recompiledOutputFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")
        val recompiledMinOutputFile = File(recompiledOutputFile.parentFile, recompiledOutputFile.nameWithoutExtension + "-min.js")
        val recompiledPirOutputFile = File(recompiledOutputFile.parentFile, recompiledOutputFile.nameWithoutExtension + "-pir.js")
        val recompiledConfig = createConfig(
            sourceDirs,
            module,
            dependencies,
            allDependencies,
            friends,
            multiModule,
            tmpDir,
            null,
            expectActualLinker,
            ErrorTolerancePolicy.DEFAULT
        )

        translateFiles(
            dirtyFiles.map { TranslationUnit.SourceFile(createPsiFile(it)) },
            outputFile,
            recompiledMinOutputFile,
            recompiledPirOutputFile,
            recompiledConfig,
            outputPrefixFile,
            outputPostfixFile,
            mainCallParameters,
            incrementalData,
            remap,
            testPackage,
            testFunction,
            needsFullIrRuntime,
            isMainModule,
            skipDceDriven = true,
            splitPerModule = false, // TODO??
            propertyLazyInitialization = false, // ??
            safeExternalBoolean = false,
            safeExternalBooleanDiagnostic = null,
            skipMangleVerification = false,
            KotlinAbiVersion.CURRENT,
            incrementalCompilation = true,
            recompile = true,
            icCaches
        )

        val newBinaryAsts = dirtyFiles.associateWith { icCache.dataProvider.binaryAst(it) }

        for (file in dirtyFiles) {
            val oldBinaryAst = oldBinaryAsts[file]
            val newBinaryAst = newBinaryAsts[file]

            assert(oldBinaryAst.contentEquals(newBinaryAst)) { "Binary AST changed after recompilation for file $file" }
        }

        if (isMainModule) {
            val originalOutput = FileUtil.loadFile(outputFile)
            val recompiledOutput = FileUtil.loadFile(recompiledOutputFile)
            assertEquals("Output file changed after recompilation", originalOutput, recompiledOutput)
        }
    }

    private fun createPirCache(path: String, allKlibPaths: Collection<String>, config: JsConfig, icCache: Map<String, ICCache>): ICCache {
        val icData = predefinedKlibHasIcCache[path] ?: prepareSingleLibraryIcCache(
            project = project,
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

    @Suppress("UNUSED_PARAMETER")
    private fun createIcCache(path: String, allKlibPaths: Collection<String>, config: JsConfig, icCache: Map<String, ICCache>): ICCache {

        fun prepare(): ICCache {
            return ICCache(PersistentCacheProvider.EMPTY, PersistentCacheConsumer.EMPTY, SerializedIcData(emptyList()))
        }

        val icData = predefinedKlibHasIcCache[path] ?: prepare()

        if (path in predefinedKlibHasIcCache) {
            predefinedKlibHasIcCache[path] = icData
        }

        return icData
    }

    private fun prepareRuntimePirCaches(config: JsConfig, icCache: MutableMap<String, ICCache>) {
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
