/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.JsBuildMetaInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangesCollector
import org.jetbrains.kotlin.incremental.IncrementalJsCache
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalDataProviderFromCache
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalJsCache
import org.jetbrains.kotlin.jps.model.k2JsCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import org.jetbrains.kotlin.jps.model.productionOutputFilePath
import org.jetbrains.kotlin.jps.model.testOutputFilePath
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils.JS_EXT
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils.META_JS_SUFFIX
import java.io.File
import java.net.URI

private const val JS_BUILD_META_INFO_FILE_NAME = "js-build-meta-info.txt"

class KotlinJsModuleBuildTarget(compileContext: CompileContext, jpsModuleBuildTarget: ModuleBuildTarget) :
    KotlinModuleBuildTarget<JsBuildMetaInfo>(compileContext, jpsModuleBuildTarget) {

    override val buildMetaInfoFactory
        get() = JsBuildMetaInfo

    override val buildMetaInfoFileName: String
        get() = JS_BUILD_META_INFO_FILE_NAME

    val isFirstBuild: Boolean
        get() {
            val targetDataRoot = context.projectDescriptor.dataManager.dataPaths.getTargetDataRoot(jpsModuleBuildTarget)
            return !IncrementalJsCache.hasHeaderFile(targetDataRoot)
        }

    override fun makeServices(
        builder: Services.Builder,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker
    ) {
        super.makeServices(builder, incrementalCaches, lookupTracker, exceptActualTracer)

        with(builder) {
            register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())

            if (IncrementalCompilation.isEnabled() && !isFirstBuild) {
                val cache = incrementalCaches[jpsModuleBuildTarget] as IncrementalJsCache

                register(
                    IncrementalDataProvider::class.java,
                    IncrementalDataProviderFromCache(cache)
                )
            }
        }
    }

    override fun compileModuleChunk(
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment
    ): Boolean {
        require(chunk.representativeTarget() == jpsModuleBuildTarget)
        if (reportAndSkipCircular(chunk, environment)) return false

        val sources = collectSourcesToCompile(dirtyFilesHolder)
        if (!checkShouldCompileAndLog(dirtyFilesHolder, sources)) return false

        val libraries = libraryFiles + dependenciesMetaFiles

        JpsKotlinCompilerRunner().runK2JsCompiler(
            commonArguments,
            module.k2JsCompilerArguments,
            module.kotlinCompilerSettings,
            environment,
            sources,
            sourceMapRoots,
            libraries,
            friendBuildTargetsMetaFiles,
            outputFile
        )

        return true
    }

    override fun doAfterBuild() {
        copyJsLibraryFilesIfNeeded()
    }

    private fun copyJsLibraryFilesIfNeeded() {
        if (module.kotlinCompilerSettings.copyJsLibraryFiles) {
            val outputLibraryRuntimeDirectory = File(outputDir, module.kotlinCompilerSettings.outputDirectoryForJsLibraryFiles).absolutePath
            JsLibraryUtils.copyJsFilesFromLibraries(
                libraryFiles, outputLibraryRuntimeDirectory,
                copySourceMap = module.k2JsCompilerArguments.sourceMap
            )
        }
    }

    private val sourceMapRoots: List<File>
        get() {
            // Compiler starts to produce path relative to base dirs in source maps if at least one statement is true:
            // 1) base dirs are specified;
            // 2) prefix is specified (i.e. non-empty)
            // Otherwise compiler produces paths relative to source maps location.
            // We don't have UI to configure base dirs, but we have UI to configure prefix.
            // If prefix is not specified (empty) in UI, we want to produce paths relative to source maps location
            return if (module.k2JsCompilerArguments.sourceMapPrefix.isNullOrBlank()) emptyList()
            else module.contentRootsList.urls
                .map { URI.create(it) }
                .filter { it.scheme == "file" }
                .map { File(it.path) }
        }

    val friendBuildTargetsMetaFiles
        get() = friendBuildTargets.mapNotNull {
            (it as? KotlinJsModuleBuildTarget)?.outputMetaFile?.absoluteFile?.toString()
        }

    val outputFile
        get() = explicitOutputPath?.let { File(it) } ?: implicitOutputFile

    private val explicitOutputPath
        get() = if (isTests) module.testOutputFilePath else module.productionOutputFilePath

    private val implicitOutputFile: File
        get() {
            val suffix = if (isTests) "_test" else ""

            return File(outputDir, module.name + suffix + JS_EXT)
        }

    private val outputFileBaseName: String
        get() = outputFile.path.substringBeforeLast(".")

    val outputMetaFile: File
        get() = File(outputFileBaseName + META_JS_SUFFIX)

    private val libraryFiles: List<String>
        get() = mutableListOf<String>().also { result ->
            for (library in allDependencies.libraries) {
                for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
                    result.add(JpsPathUtil.urlToPath(root.url))
                }
            }
        }

    private val dependenciesMetaFiles: List<String>
        get() = mutableListOf<String>().also { result ->
            allDependencies.processModules { module ->
                if (isTests) addDependencyMetaFile(module, result, isTests = true)

                // note: production targets should be also added as dependency to test targets
                addDependencyMetaFile(module, result, isTests = false)
            }
        }

    private fun addDependencyMetaFile(
        module: JpsModule,
        result: MutableList<String>,
        isTests: Boolean
    ) {
        val dependencyBuildTarget = context.kotlinBuildTargets[ModuleBuildTarget(module, isTests)]

        if (dependencyBuildTarget != this@KotlinJsModuleBuildTarget &&
            dependencyBuildTarget is KotlinJsModuleBuildTarget &&
            dependencyBuildTarget.sources.isNotEmpty()
        ) {
            val metaFile = dependencyBuildTarget.outputMetaFile
            if (metaFile.exists()) {
                result.add(metaFile.absolutePath)
            }
        }
    }

    override fun createCacheStorage(paths: BuildDataPaths) =
        JpsIncrementalJsCache(jpsModuleBuildTarget, paths)

    override fun updateCaches(
        jpsIncrementalCache: JpsIncrementalCache,
        files: List<GeneratedFile>,
        changesCollector: ChangesCollector,
        environment: JpsCompilerEnvironment
    ) {
        super.updateCaches(jpsIncrementalCache, files, changesCollector, environment)

        val incrementalResults = environment.services[IncrementalResultsConsumer::class.java] as IncrementalResultsConsumerImpl

        val jsCache = jpsIncrementalCache as IncrementalJsCache
        jsCache.header = incrementalResults.headerMetadata

        jsCache.compareAndUpdate(incrementalResults, changesCollector)
        jsCache.clearCacheForRemovedClasses(changesCollector)
    }
}