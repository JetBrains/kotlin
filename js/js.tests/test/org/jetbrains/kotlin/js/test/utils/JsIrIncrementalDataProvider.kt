/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.deserializeJsIrProgramFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.ByteArrayOutputStream
import java.io.File

private class TestArtifactCache(val moduleName: String, val binaryAsts: MutableMap<String, ByteArray> = mutableMapOf()) {
    fun fetchArtifacts(): JsModuleArtifact {
        return JsModuleArtifact(
            moduleName = moduleName,
            fileArtifacts = binaryAsts.entries.map {
                JsSrcFileArtifact(
                    srcFilePath = it.key,
                    // TODO: It will be better to use saved fragments, but it doesn't work
                    //  Merger.merge() + JsNode.resolveTemporaryNames() modify fragments,
                    //  therefore the sequential calls produce different results
                    fragments = deserializeJsIrProgramFragment(it.value)
                )
            }
        )
    }
}

class JsIrIncrementalDataProvider(private val testServices: TestServices) : TestService {
    private val fullRuntimeKlib = testServices.standardLibrariesPathProvider.fullJsStdlib()
    private val defaultRuntimeKlib = testServices.standardLibrariesPathProvider.defaultJsStdlib()
    private val kotlinTestKLib = testServices.standardLibrariesPathProvider.kotlinTestJsKLib()

    private val predefinedKlibHasIcCache = mutableMapOf<String, TestArtifactCache?>(
        fullRuntimeKlib.absolutePath to null,
        kotlinTestKLib.absolutePath to null,
        defaultRuntimeKlib.absolutePath to null
    )

    private val icCache: MutableMap<String, TestArtifactCache> = mutableMapOf()

    fun getCaches() = icCache.map { it.value.fetchArtifacts() }

    fun getCacheForModule(module: TestModule): Map<String, ByteArray> {
        val path = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)
        val canonicalPath = path.canonicalPath
        val moduleCache = icCache[canonicalPath] ?: error("No cache found for $path")

        val oldBinaryAsts = mutableMapOf<String, ByteArray>()

        for (testFile in module.files) {
            if (JsEnvironmentConfigurationDirectives.RECOMPILE in testFile.directives) {
                val fileName = "/${testFile.name}"
                oldBinaryAsts[fileName] = moduleCache.binaryAsts[fileName] ?: error("No AST found for $fileName")
                moduleCache.binaryAsts.remove(fileName)
            }
        }

        return oldBinaryAsts
    }

    private fun recordIncrementalDataForRuntimeKlib(module: TestModule) {
        val runtimeKlibPath = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val libs = runtimeKlibPath.map {
            val descriptor = testServices.libraryProvider.getDescriptorByPath(it)
            testServices.libraryProvider.getCompiledLibraryByDescriptor(descriptor)
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
        val path = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name).path
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val mainArguments = JsEnvironmentConfigurator.getMainCallParametersForModule(module)
            .run { if (shouldBeGenerated()) arguments() else null }

        val allDependencies = JsEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).keys.toList()

        recordIncrementalData(
            path,
            dirtyFiles,
            allDependencies + library,
            configuration,
            mainArguments,
        )
    }

    private fun recordIncrementalData(
        path: String,
        dirtyFiles: List<String>?,
        allDependencies: List<KotlinLibrary>,
        configuration: CompilerConfiguration,
        mainArguments: List<String>?,
    ) {
        val canonicalPath = File(path).canonicalPath
        val predefinedModuleCache = predefinedKlibHasIcCache[canonicalPath]
        if (predefinedModuleCache != null) {
            icCache[canonicalPath] = predefinedModuleCache
            return
        }

        val libs = allDependencies.associateBy { File(it.libraryFile.path).canonicalPath }

        val nameToKotlinLibrary: Map<String, KotlinLibrary> = libs.values.associateBy { it.moduleName }

        val dependencyGraph = libs.values.associateWith {
            it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
            }
        }

        val currentLib = libs[File(canonicalPath).canonicalPath] ?: error("Expected library at $canonicalPath")

        val testPackage = extractTestPackage(testServices)

        val (mainModuleIr, rebuiltFiles) = rebuildCacheForDirtyFiles(
            currentLib,
            configuration,
            dependencyGraph,
            dirtyFiles,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            setOf(FqName.fromSegments(listOfNotNull(testPackage, JsBoxRunner.TEST_FUNCTION))),
            mainArguments,
        )

        val moduleCache = icCache[canonicalPath] ?: TestArtifactCache(mainModuleIr.name.asString())

        for (rebuiltFile in rebuiltFiles) {
            if (rebuiltFile.first.module == mainModuleIr) {
                val output = ByteArrayOutputStream()
                rebuiltFile.second.serialize(output)
                moduleCache.binaryAsts[rebuiltFile.first.fileEntry.name] = output.toByteArray()
            }
        }

        if (canonicalPath in predefinedKlibHasIcCache) {
            predefinedKlibHasIcCache[canonicalPath] = moduleCache
        }

        icCache[canonicalPath] = moduleCache
    }
}

val TestServices.jsIrIncrementalDataProvider: JsIrIncrementalDataProvider by TestServices.testServiceAccessor()

