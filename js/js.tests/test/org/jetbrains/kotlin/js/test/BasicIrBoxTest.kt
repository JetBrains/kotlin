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
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.lang.Boolean.getBoolean

private val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")
private val defaultRuntimeKlib = System.getProperty("kotlin.js.reduced.stdlib.path")
private val kotlinTestKLib = System.getProperty("kotlin.js.kotlin.test.path")

// TODO Cache on FS (requires bootstrap)
private val predefinedKlibHasIcCache = mutableMapOf<String, TestModuleCache?>(
    File(fullRuntimeKlib).absolutePath to null,
    File(kotlinTestKLib).absolutePath to null,
    File(defaultRuntimeKlib).absolutePath to null
)

class TestModuleCache(val moduleName: String, val files: MutableMap<String, FileCache>) {

    constructor(moduleName: String) : this(moduleName, mutableMapOf())

    fun cacheProvider(): PersistentCacheProvider {
        return object : PersistentCacheProvider {
            override fun fileFingerPrint(path: String): Hash {
                return 0L
            }

            override fun serializedParts(path: String): SerializedIcDataForFile {
                error("Is not supported")
            }

            override fun inlineGraphForFile(path: String, sigResolver: (Int) -> IdSignature): Collection<Pair<IdSignature, TransHash>> {
                error("Is not supported")
            }

            override fun inlineHashes(path: String, sigResolver: (Int) -> IdSignature): Map<IdSignature, TransHash> {
                error("Is not supported")
            }

            override fun allInlineHashes(sigResolver: (String, Int) -> IdSignature): Map<IdSignature, TransHash> {
                error("Is not supported")
            }

            override fun binaryAst(path: String): ByteArray? {
                return files[path]?.ast ?: ByteArray(0)
            }

            override fun dts(path: String): ByteArray? {
                return files[path]?.dts
            }

            override fun sourceMap(path: String): ByteArray? {
                return files[path]?.sourceMap
            }
        }
    }

    fun cacheConsumer(): PersistentCacheConsumer {
        return object : PersistentCacheConsumer {
            override fun commitInlineFunctions(
                path: String,
                hashes: Collection<Pair<IdSignature, TransHash>>,
                sigResolver: (IdSignature) -> Int
            ) {

            }

            override fun commitFileFingerPrint(path: String, fingerprint: Hash) {

            }

            override fun commitInlineGraph(
                path: String,
                hashes: Collection<Pair<IdSignature, TransHash>>,
                sigResolver: (IdSignature) -> Int
            ) {

            }

            override fun commitICCacheData(path: String, icData: SerializedIcDataForFile) {

            }

            override fun commitBinaryAst(path: String, astData: ByteArray) {
                val storage = files.getOrPut(path) { FileCache(path, null, null, null) }
                storage.ast = astData
            }

            override fun commitBinaryDts(path: String, dstData: ByteArray) {
                val storage = files.getOrPut(path) { FileCache(path, null, null, null) }
                storage.dts = dstData
            }

            override fun commitSourceMap(path: String, mapData: ByteArray) {
                val storage = files.getOrPut(path) { FileCache(path, null, null, null) }
                storage.sourceMap = mapData
            }

            override fun invalidateForFile(path: String) {
                files.remove(path)
            }

            override fun commitLibraryPath(libraryPath: String) {

            }
        }
    }

    fun createModuleCache(): ModuleCache = ModuleCache(moduleName, files)
}

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
        icCache: MutableMap<String, TestModuleCache>
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
            runtimeKlibs.forEach { createIcCache(it, null, runtimeKlibs, config, icCache) }
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
                jsOutputName = null,
                abiVersion = abiVersion
            )
        }

        val klibCannonPath = File(klibPath).canonicalPath

        if (incrementalCompilation) {
            icCache[klibCannonPath] =
                createIcCache(klibCannonPath, filesToCompile.map { it.virtualFilePath }, allKlibPaths + klibCannonPath, config, icCache)
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
                icCache = emptyMap()
            )

            if (!skipRegularMode) {
//                val dirtyFilesToRecompile = if (recompile) {
//                    units.map { (it as TranslationUnit.SourceFile).file.virtualFilePath }.toSet()
//                } else null
                val dirtyFilesToRecompile: Set<String>? = null

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
                    filesToLower = dirtyFilesToRecompile
                )

//                if (incrementalCompilation) {
//                    // TODO: enable once incremental js generation is done
//                    generateJsFromAst(klibPath, icCache.map { it.key to it.value.createModuleCache() }.toMap())
//                }

                val jsOutputFile = if (recompile) File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")
                else outputFile

                val compiledOutput = if (dirtyFilesToRecompile != null) CompilationOutputs(
                    """
                    var JS_TESTS = function (_) {
                      'use strict';
                      _.box = function() { return 'OK'; };
                        return _;
                    }(typeof JS_TESTS === 'undefined' ? {} : JS_TESTS);
                """.trimIndent()
                ) else compiledModule.outputs!!
                val compiledDCEOutput = if (dirtyFilesToRecompile != null) null else compiledModule.outputsAfterDce

                compiledOutput.writeTo(jsOutputFile, config)

                compiledDCEOutput?.writeTo(dceOutputFile, config)

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
        icCaches: MutableMap<String, TestModuleCache>
    ) {
        val isMainModule = module.name == DEFAULT_MODULE || module.name == TEST_MODULE
        val cacheKey = outputFile.canonicalPath.replace("_v5.js", "")

        val icCache = icCaches[cacheKey] ?: error("No IC data found for module ${module.name}")

        val dirtyFiles = mutableListOf<String>()
        val oldBinaryAsts = mutableMapOf<String, ByteArray>()

        val dataProvider = icCache.cacheProvider()
        val dataConsumer = icCache.cacheConsumer()

        for (testFile in kotlinFiles) {
            if (testFile.recompile) {
                val fileName = testFile.fileName
                oldBinaryAsts[fileName] = dataProvider.binaryAst(fileName) ?: error("No AST found for $fileName")
                dataConsumer.invalidateForFile(fileName)
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
            propertyLazyInitialization = true,
            safeExternalBoolean = false,
            safeExternalBooleanDiagnostic = null,
            skipMangleVerification = false,
            KotlinAbiVersion.CURRENT,
            incrementalCompilation = true,
            recompile = true,
            icCaches
        )

        val cacheProvider = icCache.cacheProvider()
        val newBinaryAsts = dirtyFiles.associateWith { cacheProvider.binaryAst(it) }

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

    private fun createIcCache(
        path: String,
        dirtyFiles: Collection<String>?,
        allKlibPaths: Collection<String>,
        config: JsConfig,
        icCache: MutableMap<String, TestModuleCache>
    ): TestModuleCache {

        val cannonicalPath = File(path).canonicalPath
        var moduleCache = predefinedKlibHasIcCache[cannonicalPath]

        if (moduleCache == null) {
            moduleCache = icCache[cannonicalPath] ?: TestModuleCache(cannonicalPath)

            val allResolvedDependencies = jsResolveLibraries(allKlibPaths, emptyList(), DummyLogger)

            val libs = allResolvedDependencies.getFullList().associateBy { File(it.libraryFile.path).canonicalPath }

            val nameToKotlinLibrary: Map<ModuleName, KotlinLibrary> = libs.values.associateBy { it.moduleName }

            val dependencyGraph = libs.values.associateWith {
                it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                    nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
                }
            }

            val currentLib = libs[File(cannonicalPath).canonicalPath] ?: error("Expected library at $cannonicalPath")

            rebuildCacheForDirtyFiles(currentLib, config.configuration, dependencyGraph, dirtyFiles, moduleCache.cacheConsumer())

            if (cannonicalPath in predefinedKlibHasIcCache) {
                predefinedKlibHasIcCache[cannonicalPath] = moduleCache
            }
        }

        icCache[cannonicalPath] = moduleCache

        return moduleCache
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
