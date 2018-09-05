/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.ProjectBuildException
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
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangesCollector
import org.jetbrains.kotlin.incremental.ExpectActualTrackerImpl
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.storage.version.CacheAttributesDiff
import org.jetbrains.kotlin.incremental.storage.version.loadDiff
import org.jetbrains.kotlin.incremental.storage.version.localCacheVersionManager
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
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
abstract class KotlinModuleBuildTarget<BuildMetaInfoType : BuildMetaInfo> internal constructor(
    val kotlinContext: KotlinCompileContext,
    val jpsModuleBuildTarget: ModuleBuildTarget
) {
    /**
     * Note: beware of using this context for getting compilation round dependent data:
     * for example groovy can provide temp source roots with stubs, and it will be visible
     * only in round local compile context.
     *
     * TODO(1.2.80): got rid of jpsGlobalContext and replace it with kotlinContext
     */
    val jpsGlobalContext: CompileContext
        get() = kotlinContext.jpsContext

    // Initialized in KotlinCompileContext.loadTargets
    lateinit var chunk: KotlinChunk

    abstract val globalLookupCacheId: String

    abstract val isIncrementalCompilationEnabled: Boolean

    @Suppress("LeakingThis")
    val localCacheVersionManager = localCacheVersionManager(
        kotlinContext.dataPaths.getTargetDataRoot(jpsModuleBuildTarget),
        isIncrementalCompilationEnabled
    )

    val initialLocalCacheAttributesDiff: CacheAttributesDiff<*> = localCacheVersionManager.loadDiff()

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
                result.addIfNotNull(kotlinContext.targetsBinding[module.productionBuildTarget])
                result.addIfNotNull(kotlinContext.targetsBinding[relatedProductionModule?.productionBuildTarget])
            }

            return result.filter { it.sources.isNotEmpty() }
        }

    val friendOutputDirs: List<File>
        get() = friendBuildTargets.mapNotNull {
            JpsJavaExtensionService.getInstance().getOutputDirectory(it.module, false)
        }

    private val relatedProductionModule: JpsModule?
        get() = JpsJavaExtensionService.getInstance().getTestModuleProperties(module)?.productionModule

    data class Dependency(
        val src: KotlinModuleBuildTarget<*>,
        val target: KotlinModuleBuildTarget<*>,
        val exported: Boolean
    )

    // TODO(1.2.80): try replace allDependencies with KotlinChunk.collectDependentChunksRecursivelyExportedOnly
    @Deprecated("Consider using precalculated KotlinChunk.collectDependentChunksRecursivelyExportedOnly")
    val allDependencies by lazy {
        JpsJavaExtensionService.dependencies(module).recursively().exportedOnly()
            .includedIn(JpsJavaClasspathKind.compile(isTests))
    }

    /**
     * All sources of this target (including non dirty).
     * Initialized lazily based on global context and will be updated on each round based on round local context.
     *
     * Update required since source roots can be changed, for example groovy can provide new temporary source roots with stubs.
     * Lazy initialization is required for friend build targets, when friends are not compiled in this build run.
     */
    val sources: Map<File, Source>
        get() = _sources ?: synchronized(this) {
            _sources ?: updateSourcesList(jpsGlobalContext)
        }

    @Volatile
    private var _sources: Map<File, Source>? = null

    fun nextRound(localContext: CompileContext) {
        updateSourcesList(localContext)
    }

    private fun updateSourcesList(localContext: CompileContext): Map<File, Source> {
        val result = mutableMapOf<File, Source>()
        val moduleExcludes = module.excludeRootsList.urls.mapTo(java.util.HashSet(), JpsPathUtil::urlToFile)

        val compilerExcludes = JpsJavaExtensionService.getInstance()
            .getOrCreateCompilerConfiguration(module.project)
            .compilerExcludes

        val buildRootIndex = localContext.projectDescriptor.buildRootIndex
        val roots = buildRootIndex.getTargetRoots(jpsModuleBuildTarget, localContext)
        roots.forEach { rootDescriptor ->
            val isIncludedSourceRoot = rootDescriptor is KotlinIncludedModuleSourceRoot

            rootDescriptor.root.walkTopDown()
                .onEnter { file -> file !in moduleExcludes }
                .forEach { file ->
                    if (!compilerExcludes.isExcluded(file) && file.isFile && file.isKotlinSourceFile) {
                        result[file] = Source(file, isIncludedSourceRoot)
                    }
                }

        }

        this._sources = result
        return result
    }

    /**
     * @property isIncludedSourceRoot for reporting errors during cross-compilation common module sources
     */
    class Source(
        val file: File,
        val isIncludedSourceRoot: Boolean
    )

    fun isFromIncludedSourceRoot(file: File): Boolean = sources[file]?.isIncludedSourceRoot == true

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

    open fun doAfterBuild() {
    }

    open val hasCaches: Boolean = true

    abstract fun createCacheStorage(paths: BuildDataPaths): JpsIncrementalCache

    /**
     * Called for `ModuleChunk.representativeTarget`
     */
    open fun updateChunkMappings(
        localContext: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        outputItems: Map<ModuleBuildTarget, Iterable<GeneratedFile>>,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>
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
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker
    ) {
        with(builder) {
            register(LookupTracker::class.java, lookupTracker)
            register(ExpectActualTracker::class.java, exceptActualTracer)
            register(CompilationCanceledStatus::class.java, object : CompilationCanceledStatus {
                override fun checkCanceled() {
                    if (jpsGlobalContext.cancelStatus.isCanceled) throw CompilationCanceledException()
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
        return when {
            isIncrementalCompilationEnabled -> dirtyFilesHolder.getDirtyFiles(jpsModuleTarget)
            else -> target.sourceFiles
        }
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
            val logger = jpsGlobalContext.loggingManager.projectBuilderLogger
            if (logger.isEnabled) {
                logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:")
            }
        }

        return hasDirtyOrRemovedSources
    }

    abstract val buildMetaInfoFactory: BuildMetaInfoFactory<BuildMetaInfoType>

    abstract val buildMetaInfoFileName: String

    fun isVersionChanged(chunk: KotlinChunk, buildMetaInfo: BuildMetaInfo): Boolean {
        val file = chunk.buildMetaInfoFile(jpsModuleBuildTarget)
        if (!file.exists()) return false

        val prevBuildMetaInfo =
            try {
                buildMetaInfoFactory.deserializeFromString(file.readText()) ?: return false
            } catch (e: Exception) {
                KotlinBuilder.LOG.error("Could not deserialize build meta info", e)
                return false
            }

        val prevLangVersion = LanguageVersion.fromVersionString(prevBuildMetaInfo.languageVersionString)
        val prevApiVersion = ApiVersion.parse(prevBuildMetaInfo.apiVersionString)

        val reasonToRebuild = when {
            chunk.langVersion != prevLangVersion -> "Language version was changed ($prevLangVersion -> ${chunk.langVersion})"
            chunk.apiVersion != prevApiVersion -> "Api version was changed ($prevApiVersion -> ${chunk.apiVersion})"
            prevLangVersion != LanguageVersion.KOTLIN_1_0 && prevBuildMetaInfo.isEAP && !buildMetaInfo.isEAP -> {
                // If EAP->Non-EAP build with IC, then rebuild all kotlin
                "Last build was compiled with EAP-plugin"
            }
            else -> null
        }

        if (reasonToRebuild != null) {
            KotlinBuilder.LOG.info("$reasonToRebuild. Performing non-incremental rebuild (kotlin only)")
            return true
        }

        return false
    }

    private fun checkRepresentativeTarget(chunk: KotlinChunk) {
        check(chunk.representativeTarget == this)
    }

    private fun checkRepresentativeTarget(chunk: ModuleChunk) {
        check(chunk.representativeTarget() == jpsModuleBuildTarget)
    }

    private fun checkRepresentativeTarget(chunk: List<KotlinModuleBuildTarget<*>>) {
        check(chunk.first() == this)
    }
}