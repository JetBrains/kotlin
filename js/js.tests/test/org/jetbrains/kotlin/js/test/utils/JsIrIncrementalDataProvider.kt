/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.jsLibraryProvider
import java.io.File

class TestModuleCache(val files: MutableMap<String, FileCache>) {

    constructor() : this(mutableMapOf())

    private lateinit var storedModuleName: String

    fun cacheProvider(): PersistentCacheProvider {
        return object : PersistentCacheProvider {
            override fun fileFingerPrint(path: String): Hash {
                return 0L
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

            override fun filePaths(): Iterable<String> {
                return files.keys
            }

            override fun moduleName(): String {
                return storedModuleName
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

            override fun commitLibraryInfo(libraryPath: String, flatHash: ULong, transHash: ULong, configHash: ULong, moduleName: String) {
                storedModuleName = moduleName
            }
        }
    }

    fun createModuleCache(): ModuleCache = ModuleCache(storedModuleName, files)
}

class JsIrIncrementalDataProvider(private val testServices: TestServices) : TestService {
    private val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")
    private val defaultRuntimeKlib = System.getProperty("kotlin.js.reduced.stdlib.path")
    private val kotlinTestKLib = System.getProperty("kotlin.js.kotlin.test.path")

    private val predefinedKlibHasIcCache = mutableMapOf<String, TestModuleCache?>(
        File(fullRuntimeKlib).absolutePath to null,
        File(kotlinTestKLib).absolutePath to null,
        File(defaultRuntimeKlib).absolutePath to null
    )

    private val icCache: MutableMap<String, TestModuleCache> = mutableMapOf()

    fun getCaches(): Map<String, ModuleCache> {
        return icCache.map { it.key to it.value.createModuleCache() }.toMap()
    }

    fun getCacheForModule(module: TestModule): Map<String, ByteArray> {
        val path = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)
        val canonicalPath = File(path).canonicalPath
        val moduleCache = icCache[canonicalPath] ?: error("No cache found for $path")

        val oldBinaryAsts = mutableMapOf<String, ByteArray>()
        val dataProvider = moduleCache.cacheProvider()
        val dataConsumer = moduleCache.cacheConsumer()

        for (testFile in module.files) {
            if (JsEnvironmentConfigurationDirectives.RECOMPILE in testFile.directives) {
                val fileName = "/${testFile.name}"
                oldBinaryAsts[fileName] = dataProvider.binaryAst(fileName) ?: error("No AST found for $fileName")
                dataConsumer.invalidateForFile(fileName)
            }
        }

        return oldBinaryAsts
    }

    private fun recordIncrementalDataForRuntimeKlib(module: TestModule) {
        val runtimeKlibPath = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val libs = runtimeKlibPath.map {
            val descriptor = testServices.jsLibraryProvider.getDescriptorByPath(it)
            testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(descriptor)
        }
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }

        runtimeKlibPath.forEach {
            recordIncrementalData(it, null, libs, configuration, mainArguments)
        }
    }

    fun recordIncrementalData(module: TestModule, library: KotlinLibrary) {
        recordIncrementalDataForRuntimeKlib(module)

        val dirtyFiles = module.files.map { "/${it.relativePath}" }
        val path = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }

        val allDependencies = JsEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).keys.toList()
        recordIncrementalData(path, dirtyFiles, allDependencies + library, configuration, mainArguments)
    }

    private fun recordIncrementalData(
        path: String,
        dirtyFiles: List<String>?,
        allDependencies: List<KotlinLibrary>,
        configuration: CompilerConfiguration,
        mainArguments: List<String>?
    ) {
        val canonicalPath = File(path).canonicalPath
        var moduleCache = predefinedKlibHasIcCache[canonicalPath]

        if (moduleCache == null) {
            moduleCache = icCache[canonicalPath] ?: TestModuleCache()

            val libs = allDependencies.associateBy { File(it.libraryFile.path).canonicalPath }

            val nameToKotlinLibrary: Map<ModuleName, KotlinLibrary> = libs.values.associateBy { it.moduleName }

            val dependencyGraph = libs.values.associateWith {
                it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                    nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
                }
            }

            val currentLib = libs[File(canonicalPath).canonicalPath] ?: error("Expected library at $canonicalPath")

            val testPackage = extractTestPackage(testServices)

            rebuildCacheForDirtyFiles(
                currentLib,
                configuration,
                dependencyGraph,
                dirtyFiles,
                moduleCache.cacheConsumer(),
                IrFactoryImplForJsIC(WholeWorldStageController()),
                setOf(FqName.fromSegments(listOfNotNull(testPackage, JsBoxRunner.TEST_FUNCTION))),
                mainArguments,
            )

            if (canonicalPath in predefinedKlibHasIcCache) {
                predefinedKlibHasIcCache[canonicalPath] = moduleCache
            }
        }

        icCache[canonicalPath] = moduleCache
    }
}

val TestServices.jsIrIncrementalDataProvider: JsIrIncrementalDataProvider by TestServices.testServiceAccessor()
