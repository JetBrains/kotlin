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

private class TestArtifactCache(var moduleName: String? = null) : ArtifactCache() {
    override fun fetchArtifacts() = KLibArtifact(
        moduleName = moduleName ?: error("Module name is not set"),
        fileArtifacts = binaryAsts.entries.map {
            SrcFileArtifact(it.key, "", it.value)
        }
    )

    fun invalidateForFile(srcPath: String) = binaryAsts.remove(srcPath)
    fun getAst(srcPath: String) = binaryAsts[srcPath]
}

class JsIrIncrementalDataProvider(private val testServices: TestServices) : TestService {
    private val fullRuntimeKlib: String = System.getProperty("kotlin.js.full.stdlib.path")
    private val defaultRuntimeKlib = System.getProperty("kotlin.js.reduced.stdlib.path")
    private val kotlinTestKLib = System.getProperty("kotlin.js.kotlin.test.path")

    private val predefinedKlibHasIcCache = mutableMapOf<String, TestArtifactCache?>(
        File(fullRuntimeKlib).absolutePath to null,
        File(kotlinTestKLib).absolutePath to null,
        File(defaultRuntimeKlib).absolutePath to null
    )

    private val icCache: MutableMap<String, TestArtifactCache> = mutableMapOf()

    fun getCaches() = icCache.map { it.value.fetchArtifacts() }

    fun getCacheForModule(module: TestModule): Map<String, ByteArray> {
        val path = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)
        val canonicalPath = File(path).canonicalPath
        val moduleCache = icCache[canonicalPath] ?: error("No cache found for $path")

        val oldBinaryAsts = mutableMapOf<String, ByteArray>()

        for (testFile in module.files) {
            if (JsEnvironmentConfigurationDirectives.RECOMPILE in testFile.directives) {
                val fileName = "/${testFile.name}"
                oldBinaryAsts[fileName] = moduleCache.getAst(fileName) ?: error("No AST found for $fileName")
                moduleCache.invalidateForFile(fileName)
            }
        }

        return oldBinaryAsts
    }

    private fun recordIncrementalDataForRuntimeKlib(module: TestModule) {
        val runtimeKlibPath = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val libs = runtimeKlibPath.filterNotNull().map {
            val descriptor = testServices.jsLibraryProvider.getDescriptorByPath(it)
            testServices.jsLibraryProvider.getCompiledLibraryByDescriptor(descriptor)
        }
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }

        runtimeKlibPath.forEach {
            if (it != null) {
                recordIncrementalData(it, null, libs, configuration, mainArguments)
            }
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
            moduleCache = icCache[canonicalPath] ?: TestArtifactCache()

            val libs = allDependencies.associateBy { File(it.libraryFile.path).canonicalPath }

            val nameToKotlinLibrary: Map<String, KotlinLibrary> = libs.values.associateBy { it.moduleName }

            val dependencyGraph = libs.values.associateWith {
                it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                    nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
                }
            }

            val currentLib = libs[File(canonicalPath).canonicalPath] ?: error("Expected library at $canonicalPath")

            val testPackage = extractTestPackage(testServices)

            moduleCache.moduleName = rebuildCacheForDirtyFiles(
                currentLib,
                configuration,
                dependencyGraph,
                dirtyFiles,
                moduleCache,
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
