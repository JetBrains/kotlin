/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.ProjectBuildException
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.build.BuildMetaInfo
import org.jetbrains.kotlin.build.BuildMetaInfoFactory
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.ChangesCollector
import org.jetbrains.kotlin.incremental.ExpectActualTrackerImpl
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.build.KotlinCommonModuleSourceRoot
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import org.jetbrains.kotlin.jps.build.isKotlinSourceFile
import org.jetbrains.kotlin.jps.incremental.CacheVersionProvider
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.model.kotlinCompilerArguments
import org.jetbrains.kotlin.jps.model.productionOutputFilePath
import org.jetbrains.kotlin.jps.model.testOutputFilePath
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

/**
 * Properties and actions for Kotlin test / production module build target.
 */
abstract class KotlinModuleBuildTarget<BuildMetaInfoType : BuildMetaInfo>(
    val context: CompileContext,
    val jpsModuleBuildTarget: ModuleBuildTarget
) {
    val module: JpsModule
        get() = jpsModuleBuildTarget.module

    val isTests: Boolean
        get() = jpsModuleBuildTarget.isTests

    val targetId: TargetId
        get() {
            // Since IDEA 2016 each gradle source root is imported as a separate module.
            // One gradle module X is imported as two JPS modules:
            // 1. X-production with one production target;
            // 2. X-test with one test target.
            // This breaks kotlin code since internal members' names are mangled using module name.
            // For example, a declaration of a function 'f' in 'X-production' becomes 'fXProduction', but a call 'f' in 'X-test' becomes 'fXTest()'.
            // The workaround is to replace a name of such test target with the name of corresponding production module.
            // See KT-11993.
            val name = relatedProductionModule?.name ?: jpsModuleBuildTarget.id
            return TargetId(name, jpsModuleBuildTarget.targetType.typeId)
        }

    val outputDir by lazy {
        val explicitOutputPath = if (isTests) module.testOutputFilePath else module.productionOutputFilePath
        val explicitOutputDir = explicitOutputPath?.let { File(it).absoluteFile.parentFile }
        return@lazy explicitOutputDir
                ?: jpsModuleBuildTarget.outputDir
                ?: throw ProjectBuildException("No output directory found for " + this)
    }

    val friendBuildTargets: List<KotlinModuleBuildTarget<*>>
        get() {
            val result = mutableListOf<KotlinModuleBuildTarget<*>>()

            if (isTests) {
                result.addIfNotNull(context.kotlinBuildTargets[module.productionBuildTarget])
                result.addIfNotNull(context.kotlinBuildTargets[relatedProductionModule?.productionBuildTarget])
            }

            return result.filter { it.sources.isNotEmpty() }
        }

    val friendOutputDirs: List<File>
        get() = friendBuildTargets.mapNotNull {
            JpsJavaExtensionService.getInstance().getOutputDirectory(it.module, false)
        }

    private val relatedProductionModule: JpsModule?
        get() = JpsJavaExtensionService.getInstance().getTestModuleProperties(module)?.productionModule

    val allDependencies by lazy {
        JpsJavaExtensionService.dependencies(module).recursively().exportedOnly()
            .includedIn(JpsJavaClasspathKind.compile(isTests))
    }

    val sources: Map<File, Source> by lazy {
        mutableMapOf<File, Source>().also { result ->
            collectSources(result)
        }
    }

    private fun collectSources(receiver: MutableMap<File, Source>) {
        val moduleExcludes = module.excludeRootsList.urls.mapTo(java.util.HashSet(), JpsPathUtil::urlToFile)

        val compilerExcludes = JpsJavaExtensionService.getInstance()
            .getOrCreateCompilerConfiguration(module.project)
            .compilerExcludes

        val buildRootIndex = context.projectDescriptor.buildRootIndex
        val roots = buildRootIndex.getTargetRoots(jpsModuleBuildTarget, context)
        roots.forEach { rootDescriptor ->
            val isCommonRoot = rootDescriptor is KotlinCommonModuleSourceRoot

            rootDescriptor.root.walkTopDown()
                .onEnter { file -> file !in moduleExcludes }
                .forEach { file ->
                    if (!compilerExcludes.isExcluded(file) && file.isFile && file.isKotlinSourceFile) {
                        receiver[file] = Source(file, isCommonRoot)
                    }
                }

        }
    }

    /**
     * @property isCommonModule for reporting errors during cross-compilation common module sources
     */
    class Source(
        val file: File,
        val isCommonModule: Boolean
    )

    fun isCommonModuleFile(file: File): Boolean = sources[file]?.isCommonModule == true

    val sourceFiles: Collection<File>
        get() = sources.values.map { it.file }

    override fun toString() = jpsModuleBuildTarget.toString()

    /**
     * Called for `ModuleChunk.representativeTarget`
     */
    abstract fun compileModuleChunk(
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        environment: JpsCompilerEnvironment
    ): Boolean

    protected fun reportAndSkipCircular(
        chunk: ModuleChunk,
        environment: JpsCompilerEnvironment
    ): Boolean {
        if (chunk.modules.size > 1) {
            // We do not support circular dependencies, but if they are present, we do our best should not break the build,
            // so we simply yield a warning and report NOTHING_DONE
            environment.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Circular dependencies are not supported. The following modules depend on each other: "
                        + chunk.modules.joinToString(", ") { it.name } + " "
                        + "Kotlin is not compiled for these modules"
            )

            return true
        }

        return false
    }

    fun compilerArgumentsForChunk(chunk: ModuleChunk): CommonCompilerArguments =
        chunk.representativeTarget().module.kotlinCompilerArguments

    open fun doAfterBuild() {
    }

    open val hasCaches: Boolean = true

    abstract fun createCacheStorage(paths: BuildDataPaths): JpsIncrementalCache

    /**
     * Called for `ModuleChunk.representativeTarget`
     */
    open fun updateChunkMappings(
        chunk: ModuleChunk,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        outputItems: Map<ModuleBuildTarget, Iterable<GeneratedFile>>,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>
    ) {
        // by default do nothing
    }

    open fun updateCaches(
        jpsIncrementalCache: JpsIncrementalCache,
        files: List<GeneratedFile>,
        changesCollector: ChangesCollector,
        environment: JpsCompilerEnvironment
    ) {
        val expectActualTracker = environment.services[ExpectActualTracker::class.java] as ExpectActualTrackerImpl
        jpsIncrementalCache.registerComplementaryFiles(expectActualTracker)
    }

    open fun makeServices(
        builder: Services.Builder,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker
    ) {
        with(builder) {
            register(LookupTracker::class.java, lookupTracker)
            register(ExpectActualTracker::class.java, exceptActualTracer)
            register(CompilationCanceledStatus::class.java, object : CompilationCanceledStatus {
                override fun checkCanceled() {
                    if (context.cancelStatus.isCanceled) throw CompilationCanceledException()
                }
            })
        }
    }

    protected fun collectSourcesToCompile(dirtyFilesHolder: KotlinDirtySourceFilesHolder) =
        collectSourcesToCompile(this, dirtyFilesHolder)

    /**
     * Should be used only for particular target in chunk (jvm)
     */
    protected fun collectSourcesToCompile(
        target: KotlinModuleBuildTarget<BuildMetaInfoType>,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder
    ): Collection<File> {
        // Should not be cached since may be vary in different rounds

        val jpsModuleTarget = target.jpsModuleBuildTarget
        return if (IncrementalCompilation.isEnabled()) dirtyFilesHolder.getDirtyFiles(jpsModuleTarget)
        else target.sourceFiles
    }

    protected fun checkShouldCompileAndLog(dirtyFilesHolder: KotlinDirtySourceFilesHolder, moduleSources: Collection<File>) =
        checkShouldCompileAndLog(this, dirtyFilesHolder, moduleSources)

    /**
     * Should be used only for particular target in chunk (jvm)
     */
    protected fun checkShouldCompileAndLog(
        target: KotlinModuleBuildTarget<BuildMetaInfoType>,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        moduleSources: Collection<File>
    ): Boolean {
        val hasRemovedSources = dirtyFilesHolder.getRemovedFiles(target.jpsModuleBuildTarget).isNotEmpty()
        val hasDirtyOrRemovedSources = moduleSources.isNotEmpty() || hasRemovedSources
        if (hasDirtyOrRemovedSources) {
            val logger = context.loggingManager.projectBuilderLogger
            if (logger.isEnabled) {
                logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:")
            }
        }

        return hasDirtyOrRemovedSources
    }

    abstract val buildMetaInfoFactory: BuildMetaInfoFactory<BuildMetaInfoType>

    abstract val buildMetaInfoFileName: String

    fun buildMetaInfoFile(target: ModuleBuildTarget, dataManager: BuildDataManager): File =
        File(dataManager.dataPaths.getTargetDataRoot(target), buildMetaInfoFileName)

    fun saveVersions(context: CompileContext, chunk: ModuleChunk, commonArguments: CommonCompilerArguments) {
        val dataManager = context.projectDescriptor.dataManager
        val targets = chunk.targets
        val cacheVersionsProvider = CacheVersionProvider(dataManager.dataPaths)
        cacheVersionsProvider.allVersions(targets).forEach { it.saveIfNeeded() }

        val buildMetaInfo = buildMetaInfoFactory.create(commonArguments)
        val serializedMetaInfo = buildMetaInfoFactory.serializeToString(buildMetaInfo)

        for (target in chunk.targets) {
            buildMetaInfoFile(target, dataManager).writeText(serializedMetaInfo)
        }
    }

    fun checkCachesVersions(chunk: ModuleChunk, dataManager: BuildDataManager, actions: MutableSet<CacheVersion.Action>) {
        val args = compilerArgumentsForChunk(chunk)
        val currentBuildMetaInfo = buildMetaInfoFactory.create(args)

        for (target in chunk.targets) {
            val file = buildMetaInfoFile(target, dataManager)
            if (!file.exists()) continue

            val lastBuildMetaInfo =
                try {
                    buildMetaInfoFactory.deserializeFromString(file.readText()) ?: continue
                } catch (e: Exception) {
                    KotlinBuilder.LOG.error("Could not deserialize build meta info", e)
                    continue
                }

            val lastBuildLangVersion = LanguageVersion.fromVersionString(lastBuildMetaInfo.languageVersionString)
            val lastBuildApiVersion = ApiVersion.parse(lastBuildMetaInfo.apiVersionString)
            val currentLangVersion =
                args.languageVersion?.let { LanguageVersion.fromVersionString(it) } ?: LanguageVersion.LATEST_STABLE
            val currentApiVersion =
                args.apiVersion?.let { ApiVersion.parse(it) } ?: ApiVersion.createByLanguageVersion(currentLangVersion)

            val reasonToRebuild = when {
                currentLangVersion != lastBuildLangVersion -> {
                    "Language version was changed ($lastBuildLangVersion -> $currentLangVersion)"
                }

                currentApiVersion != lastBuildApiVersion -> {
                    "Api version was changed ($lastBuildApiVersion -> $currentApiVersion)"
                }

                lastBuildLangVersion != LanguageVersion.KOTLIN_1_0 && lastBuildMetaInfo.isEAP && !currentBuildMetaInfo.isEAP -> {
                    // If EAP->Non-EAP build with IC, then rebuild all kotlin
                    "Last build was compiled with EAP-plugin"
                }
                else -> null
            }

            if (reasonToRebuild != null) {
                KotlinBuilder.LOG.info("$reasonToRebuild. Performing non-incremental rebuild (kotlin only)")
                actions.add(CacheVersion.Action.REBUILD_ALL_KOTLIN)
            }
        }
    }
}