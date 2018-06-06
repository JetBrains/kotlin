/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.FileProcessor
import org.jetbrains.jps.builders.impl.DirtyFilesHolderBase
import org.jetbrains.jps.builders.java.JavaBuilderUtil
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode.*
import org.jetbrains.jps.incremental.java.JavaBuilder
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.JvmBuildMetaInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.incremental.*
import org.jetbrains.kotlin.jps.platforms.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.preloading.ClassCondition
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import java.io.File
import java.util.*
import kotlin.collections.HashSet

class KotlinBuilder : ModuleLevelBuilder(BuilderCategory.SOURCE_PROCESSOR) {
    companion object {
        const val KOTLIN_BUILDER_NAME: String = "Kotlin Builder"

        val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")
        const val JVM_BUILD_META_INFO_FILE_NAME = "jvm-build-meta-info.txt"
        const val SKIP_CACHE_VERSION_CHECK_PROPERTY = "kotlin.jps.skip.cache.version.check"
        const val JPS_KOTLIN_HOME_PROPERTY = "jps.kotlin.home"

        val classesToLoadByParent: ClassCondition
            get() = ClassCondition { className ->
                className.startsWith("org.jetbrains.kotlin.load.kotlin.incremental.components.")
                        || className.startsWith("org.jetbrains.kotlin.incremental.components.")
                        || className.startsWith("org.jetbrains.kotlin.incremental.js")
                        || className == "org.jetbrains.kotlin.config.Services"
                        || className.startsWith("org.apache.log4j.") // For logging from compiler
                        || className == "org.jetbrains.kotlin.progress.CompilationCanceledStatus"
                        || className == "org.jetbrains.kotlin.progress.CompilationCanceledException"
                        || className == "org.jetbrains.kotlin.modules.TargetId"
                        || className == "org.jetbrains.kotlin.cli.common.ExitCode"
            }
    }

    private val statisticsLogger = TeamcityStatisticsLogger()

    override fun getPresentableName() = KOTLIN_BUILDER_NAME

    override fun getCompilableFileExtensions() = arrayListOf("kt")

    override fun buildStarted(context: CompileContext) {
        LOG.debug("==========================================")
        LOG.info("is Kotlin incremental compilation enabled: ${IncrementalCompilation.isEnabled()}")
        LOG.info("is Kotlin compiler daemon enabled: ${isDaemonEnabled()}")

        val historyLabel = context.getBuilderParameter("history label")
        if (historyLabel != null) {
            LOG.info("Label in local history: $historyLabel")
        }
    }

    override fun buildFinished(context: CompileContext?) {
        statisticsLogger.reportTotal()
    }

    override fun chunkBuildStarted(context: CompileContext, chunk: ModuleChunk) {
        super.chunkBuildStarted(context, chunk)

        if (chunk.isDummy(context)) return

        val buildLogger = context.testingContext?.buildLogger
        buildLogger?.buildStarted(context, chunk)

        if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) return

        val targets = chunk.targets
        val dataManager = context.projectDescriptor.dataManager
        val hasKotlin = HasKotlinMarker(dataManager)

        if (targets.none { hasKotlin[it] == true }) return

        val roundDirtyFiles = KotlinDirtySourceFilesHolder(
            chunk,
            context,
            object : DirtyFilesHolderBase<JavaSourceRootDescriptor, ModuleBuildTarget>(context) {
                override fun processDirtyFiles(processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>) {
                    FSOperations.processFilesToRecompile(context, chunk, processor)
                }
            }
        )
        val fsOperations = FSOperationsHelper(context, chunk, roundDirtyFiles, LOG)

        if (System.getProperty(SKIP_CACHE_VERSION_CHECK_PROPERTY) == null) {
            val cacheVersionsProvider = CacheVersionProvider(dataManager.dataPaths)
            val actions = checkCachesVersions(context, cacheVersionsProvider, chunk)
            applyActionsOnCacheVersionChange(actions, cacheVersionsProvider, context, dataManager, targets, fsOperations)
            if (CacheVersion.Action.REBUILD_ALL_KOTLIN in actions) {
                return
            }
        }

        // try to perform a lookup
        // request rebuild if storage is corrupted
        try {
            dataManager.withLookupStorage {
                it.get(LookupSymbol("<#NAME#>", "<#SCOPE#>"))
            }
        } catch (e: Exception) {
            // todo: report to Intellij when IDEA-187115 is implemented
            LOG.info(e)
            markAllKotlinForRebuild(context, fsOperations, "Lookup storage is corrupted")
            return
        }

        markAdditionalFilesForInitialRound(chunk, context, fsOperations, roundDirtyFiles)
        buildLogger?.afterBuildStarted(context, chunk)
    }

    private fun markAdditionalFilesForInitialRound(
        chunk: ModuleChunk,
        context: CompileContext,
        fsOperations: FSOperationsHelper,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder
    ) {
        val representativeTarget = context.kotlinBuildTargets[chunk.representativeTarget()] ?: return

        val incrementalCaches = getIncrementalCaches(chunk, context)
        val messageCollector = MessageCollectorAdapter(context, representativeTarget)
        val environment = createCompileEnvironment(
            representativeTarget,
            incrementalCaches,
            LookupTracker.DO_NOTHING,
            ExpectActualTracker.DoNothing,
            chunk,
            messageCollector
        ) ?: return

        val removedClasses = HashSet<String>()
        for (target in chunk.targets) {
            val cache = incrementalCaches[target] ?: continue
            val dirtyFiles = dirtyFilesHolder.getDirtyFiles(target)
            val removedFiles = dirtyFilesHolder.getRemovedFiles(target)

            val existingClasses = JpsKotlinCompilerRunner().classesFqNamesByFiles(environment, dirtyFiles)
            val previousClasses = cache.classesFqNamesBySources(dirtyFiles + removedFiles)
            for (jvmClassName in previousClasses) {
                val fqName = jvmClassName.asString()
                if (fqName !in existingClasses) {
                    removedClasses.add(fqName)
                }
            }
        }

        val changesCollector = ChangesCollector()
        removedClasses.forEach { changesCollector.collectSignature(FqName(it), areSubclassesAffected = true) }
        val affectedByRemovedClasses = changesCollector.getDirtyFiles(incrementalCaches.values, context.projectDescriptor.dataManager)

        fsOperations.markFilesForCurrentRound(affectedByRemovedClasses)
    }

    private fun checkCachesVersions(
        context: CompileContext,
        cacheVersionsProvider: CacheVersionProvider,
        chunk: ModuleChunk
    ): Set<CacheVersion.Action> {
        val targets = chunk.targets
        val dataManager = context.projectDescriptor.dataManager

        val allVersions = cacheVersionsProvider.allVersions(targets)
        val actions = allVersions.map { it.checkVersion() }.toMutableSet()

        val kotlinModuleBuilderTarget = context.kotlinBuildTargets[chunk.representativeTarget()]!!
        kotlinModuleBuilderTarget.checkCachesVersions(chunk, dataManager, actions)

        return actions
    }

    override fun chunkBuildFinished(context: CompileContext, chunk: ModuleChunk) {
        super.chunkBuildFinished(context, chunk)

        if (chunk.isDummy(context)) return

        LOG.debug("------------------------------------------")
    }

    override fun build(
        context: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        outputConsumer: ModuleLevelBuilder.OutputConsumer
    ): ModuleLevelBuilder.ExitCode {
        if (chunk.isDummy(context))
            return NOTHING_DONE

        val kotlinTarget = context.kotlinBuildTargets[chunk.representativeTarget()] ?: return OK

        val messageCollector = MessageCollectorAdapter(context, kotlinTarget)
        val kotlinDirtyFilesHolder = KotlinDirtySourceFilesHolder(chunk, context, dirtyFilesHolder)
        val fsOperations = FSOperationsHelper(context, chunk, kotlinDirtyFilesHolder, LOG)

        try {
            val proposedExitCode = doBuild(chunk, kotlinTarget, context, kotlinDirtyFilesHolder, messageCollector, outputConsumer, fsOperations)

            val actualExitCode = if (proposedExitCode == OK && fsOperations.hasMarkedDirty) ADDITIONAL_PASS_REQUIRED else proposedExitCode

            LOG.debug("Build result: $actualExitCode")

            context.testingContext?.buildLogger?.buildFinished(actualExitCode)

            return actualExitCode
        } catch (e: StopBuildException) {
            LOG.info("Caught exception: $e")
            throw e
        } catch (e: Throwable) {
            LOG.info("Caught exception: $e")
            MessageCollectorUtil.reportException(messageCollector, e)
            return ABORT
        }
    }

    private fun doBuild(
        chunk: ModuleChunk,
        representativeTarget: KotlinModuleBuildTarget,
        context: CompileContext,
        kotlinDirtyFilesHolder: KotlinDirtySourceFilesHolder,
        messageCollector: MessageCollectorAdapter,
        outputConsumer: OutputConsumer,
        fsOperations: FSOperationsHelper
    ): ModuleLevelBuilder.ExitCode {
        // Workaround for Android Studio
        if (representativeTarget is KotlinJvmModuleBuildTarget && !JavaBuilder.IS_ENABLED[context, true]) {
            messageCollector.report(INFO, "Kotlin JPS plugin is disabled")
            return NOTHING_DONE
        }

        val projectDescriptor = context.projectDescriptor
        val dataManager = projectDescriptor.dataManager
        val targets = chunk.targets
        val hasKotlin = HasKotlinMarker(dataManager)
        val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)
        val isChunkRebuilding = JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)
                || targets.any { rebuildAfterCacheVersionChanged[it] == true }

        if (kotlinDirtyFilesHolder.hasDirtyOrRemovedFiles) {
            if (!isChunkRebuilding && !IncrementalCompilation.isEnabled()) {
                targets.forEach { rebuildAfterCacheVersionChanged[it] = true }
                return CHUNK_REBUILD_REQUIRED
            }
        } else {
            if (isChunkRebuilding) {
                targets.forEach { hasKotlin[it] = false }
            }

            targets.forEach { rebuildAfterCacheVersionChanged.clean(it) }
            return NOTHING_DONE
        }

        val targetsWithoutOutputDir = targets.filter { it.outputDir == null }
        if (targetsWithoutOutputDir.isNotEmpty()) {
            messageCollector.report(ERROR, "Output directory not specified for " + targetsWithoutOutputDir.joinToString())
            return ABORT
        }

        val project = projectDescriptor.project
        val lookupTracker = getLookupTracker(project)
        val exceptActualTracer = ExpectActualTrackerImpl()
        val incrementalCaches = getIncrementalCaches(chunk, context)
        val environment = createCompileEnvironment(
            representativeTarget,
            incrementalCaches,
            lookupTracker,
            exceptActualTracer,
            chunk,
            messageCollector
        ) ?: return ABORT

        val commonArguments = representativeTarget.compilerArgumentsForChunk(chunk).apply {
            reportOutputFiles = true
            version = true // Always report the version to help diagnosing user issues if they submit the compiler output
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("Compiling files: ${kotlinDirtyFilesHolder.allDirtyFiles}")
        }

        val start = System.nanoTime()
        val outputItemCollector = doCompileModuleChunk(
            chunk, representativeTarget, commonArguments, context, kotlinDirtyFilesHolder, fsOperations,
            environment, incrementalCaches
        )

        statisticsLogger.registerStatistic(chunk, System.nanoTime() - start)

        if (outputItemCollector == null) {
            return NOTHING_DONE
        }

        val compilationErrors = Utils.ERRORS_DETECTED_KEY[context, false]
        if (compilationErrors) {
            LOG.info("Compiled with errors")
            return ABORT
        } else {
            LOG.info("Compiled successfully")
        }

        val generatedFiles = getGeneratedFiles(context, chunk, environment.outputItemsCollector)

        registerOutputItems(outputConsumer, generatedFiles)
        saveVersions(context, chunk, commonArguments)

        if (targets.any { hasKotlin[it] == null }) {
            fsOperations.markChunk(recursively = false, kotlinOnly = true, excludeFiles = kotlinDirtyFilesHolder.allDirtyFiles)
        }

        for (target in targets) {
            hasKotlin[target] = true
            rebuildAfterCacheVersionChanged.clean(target)
        }

        chunk.targets.forEach {
            context.kotlinBuildTargets[it]?.doAfterBuild()
        }

        representativeTarget.updateChunkMappings(
            chunk,
            kotlinDirtyFilesHolder,
            generatedFiles,
            incrementalCaches
        )

        if (!IncrementalCompilation.isEnabled()) {
            return OK
        }

        context.checkCanceled()

        environment.withProgressReporter { progress ->
            progress.progress("updating IC caches")

            val changesCollector = ChangesCollector()

            for ((target, files) in generatedFiles) {
                val kotlinModuleBuilderTarget = context.kotlinBuildTargets[target]!!
                kotlinModuleBuilderTarget.updateCaches(incrementalCaches[target]!!, files, changesCollector, environment)
            }

            updateLookupStorage(lookupTracker, dataManager, kotlinDirtyFilesHolder)

            if (!isChunkRebuilding) {
                changesCollector.processChangesUsingLookups(
                    kotlinDirtyFilesHolder.allDirtyFiles,
                    dataManager,
                    fsOperations,
                    incrementalCaches.values
                )
            }
        }

        return OK
    }

    private fun applyActionsOnCacheVersionChange(
        actions: Set<CacheVersion.Action>,
        cacheVersionsProvider: CacheVersionProvider,
        context: CompileContext,
        dataManager: BuildDataManager,
        targets: MutableSet<ModuleBuildTarget>,
        fsOperations: FSOperationsHelper
    ) {
        val hasKotlin = HasKotlinMarker(dataManager)
        val sortedActions = actions.sorted()

        context.testingContext?.buildLogger?.actionsOnCacheVersionChanged(sortedActions)

        for (status in sortedActions) {
            when (status) {
                CacheVersion.Action.REBUILD_ALL_KOTLIN -> {
                    markAllKotlinForRebuild(context, fsOperations, "Kotlin global lookup map format changed")
                    return
                }
                CacheVersion.Action.REBUILD_CHUNK -> {
                    LOG.info("Clearing caches for " + targets.joinToString { it.presentableName })
                    val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)

                    val kotlinBuildTargets = context.kotlinBuildTargets
                    for (target in targets) {
                        dataManager.getKotlinCache(kotlinBuildTargets[target])?.clean()
                        hasKotlin.clean(target)
                        rebuildAfterCacheVersionChanged[target] = true
                    }

                    fsOperations.markChunk(recursively = false, kotlinOnly = true)

                    return
                }
                CacheVersion.Action.CLEAN_NORMAL_CACHES -> {
                    LOG.info("Clearing caches for all targets")

                    val kotlinBuildTargets = context.kotlinBuildTargets
                    for (target in context.allTargets()) {
                        dataManager.getKotlinCache(kotlinBuildTargets[target])?.clean()
                    }
                }
                CacheVersion.Action.CLEAN_DATA_CONTAINER -> {
                    LOG.info("Clearing lookup cache")
                    dataManager.cleanLookupStorage(LOG)
                    cacheVersionsProvider.dataContainerVersion().clean()
                }
                else -> {
                    assert(status == CacheVersion.Action.DO_NOTHING) { "Unknown version status $status" }
                }
            }
        }
    }

    private fun CompileContext.allTargets() =
        projectDescriptor.buildTargetIndex.allTargets.filterIsInstanceTo<ModuleBuildTarget, MutableSet<ModuleBuildTarget>>(HashSet())

    private fun markAllKotlinForRebuild(context: CompileContext, fsOperations: FSOperationsHelper, reason: String) {
        LOG.info("Rebuilding all Kotlin: $reason")
        val project = context.projectDescriptor.project
        val sourceRoots = project.modules.flatMap { it.sourceRoots }
        val dataManager = context.projectDescriptor.dataManager
        val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)

        for (sourceRoot in sourceRoots) {
            val ktFiles = sourceRoot.file.walk().filter { it.isKotlinSourceFile }
            fsOperations.markFiles(ktFiles.toList())
        }

        val kotlinBuildTargets = context.kotlinBuildTargets
        for (target in context.allTargets()) {
            dataManager.getKotlinCache(kotlinBuildTargets[target])?.clean()
            rebuildAfterCacheVersionChanged[target] = true
        }

        dataManager.cleanLookupStorage(LOG)
    }

    private fun saveVersions(context: CompileContext, chunk: ModuleChunk, commonArguments: CommonCompilerArguments) {
        val dataManager = context.projectDescriptor.dataManager
        val targets = chunk.targets
        val cacheVersionsProvider = CacheVersionProvider(dataManager.dataPaths)
        cacheVersionsProvider.allVersions(targets).forEach { it.saveIfNeeded() }

        val isJsModule = context.kotlinBuildTargets[chunk.representativeTarget()] is KotlinJsModuleBuildTarget
        if (!isJsModule) {
            val jvmBuildMetaInfo = JvmBuildMetaInfo(commonArguments)
            val serializedMetaInfo = JvmBuildMetaInfo.serializeToString(jvmBuildMetaInfo)

            for (target in chunk.targets) {
                jvmBuildMetaInfoFile(target, dataManager).writeText(serializedMetaInfo)
            }
        }
    }

    private fun doCompileModuleChunk(
        chunk: ModuleChunk,
        kotlinTarget: KotlinModuleBuildTarget,
        commonArguments: CommonCompilerArguments,
        context: CompileContext,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        fsOperations: FSOperationsHelper,
        environment: JpsCompilerEnvironment,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>
    ): OutputItemsCollector? {

        val representativeTarget = chunk.representativeTarget()

        fun concatenate(strings: Array<String>?, cp: List<String>) = arrayOf(*strings.orEmpty(), *cp.toTypedArray())

        for (argumentProvider in ServiceLoader.load(KotlinJpsCompilerArgumentsProvider::class.java)) {
            // appending to pluginOptions
            commonArguments.pluginOptions = concatenate(
                commonArguments.pluginOptions,
                argumentProvider.getExtraArguments(representativeTarget, context)
            )
            // appending to classpath
            commonArguments.pluginClasspaths = concatenate(
                commonArguments.pluginClasspaths,
                argumentProvider.getClasspath(representativeTarget, context)
            )

            LOG.debug("Plugin loaded: ${argumentProvider::class.java.simpleName}")
        }

        if (IncrementalCompilation.isEnabled()) {
            for (target in chunk.targets) {
                val cache = incrementalCaches[target]
                val targetDirtyFiles = dirtyFilesHolder.byTarget[target]

                if (cache != null && targetDirtyFiles != null) {
                    val complementaryFiles = cache.clearComplementaryFilesMapping(targetDirtyFiles.dirty + targetDirtyFiles.removed)
                    fsOperations.markFilesForCurrentRound(target, complementaryFiles)

                    cache.markDirty(targetDirtyFiles.dirty + targetDirtyFiles.removed)
                }
            }
        }

        val isDoneSomething = kotlinTarget.compileModuleChunk(
            chunk, commonArguments, dirtyFilesHolder, environment
        )

        return if (isDoneSomething) environment.outputItemsCollector else null
    }

    private fun createCompileEnvironment(
        kotlinModuleBuilderTarget: KotlinModuleBuildTarget,
        incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker,
        chunk: ModuleChunk,
        messageCollector: MessageCollectorAdapter
    ): JpsCompilerEnvironment? {
        val compilerServices = with(Services.Builder()) {
            kotlinModuleBuilderTarget.makeServices(this, incrementalCaches, lookupTracker, exceptActualTracer)
            build()
        }

        val paths = computeKotlinPathsForJpsPlugin()
        if (paths == null || !paths.homePath.exists()) {
            messageCollector.report(
                ERROR, "Cannot find kotlinc home. Make sure the plugin is properly installed, " +
                        "or specify $JPS_KOTLIN_HOME_PROPERTY system property"
            )
            return null
        }

        return JpsCompilerEnvironment(
            paths,
            compilerServices,
            classesToLoadByParent,
            messageCollector,
            OutputItemsCollectorImpl(),
            ProgressReporterImpl(kotlinModuleBuilderTarget.context, chunk)
        )
    }

    // When JPS is run on TeamCity, it can not rely on Kotlin plugin layout,
    // so the path to Kotlin is specified in a system property
    private fun computeKotlinPathsForJpsPlugin(): KotlinPaths? {
        if (System.getProperty("kotlin.jps.tests").equals("true", ignoreCase = true)) {
            return PathUtil.kotlinPathsForDistDirectory
        }

        val jpsKotlinHome = System.getProperty(JPS_KOTLIN_HOME_PROPERTY)
        if (jpsKotlinHome != null) {
            return KotlinPathsFromHomeDir(File(jpsKotlinHome))
        }

        val jar = PathUtil.pathUtilJar.takeIf(File::exists)
        if (jar?.name == "kotlin-jps-plugin.jar") {
            val pluginHome = jar.parentFile.parentFile.parentFile
            return KotlinPathsFromHomeDir(File(pluginHome, PathUtil.HOME_FOLDER_NAME))
        }

        return null
    }

    private fun getGeneratedFiles(
        context: CompileContext,
        chunk: ModuleChunk,
        outputItemCollector: OutputItemsCollectorImpl
    ): Map<ModuleBuildTarget, List<GeneratedFile>> {
        // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
        val sourceToTarget = HashMap<File, ModuleBuildTarget>()
        if (chunk.targets.size > 1) {
            for (target in chunk.targets) {
                context.kotlinBuildTargets[target]?.sourceFiles?.forEach {
                    sourceToTarget[it] = target
                }
            }
        }

        val representativeTarget = chunk.representativeTarget()
        fun SimpleOutputItem.target() =
            sourceFiles.firstOrNull()?.let { sourceToTarget[it] } ?: chunk.targets.singleOrNull {
                        it.outputDir?.let {
                            outputFile.startsWith(it)
                        } ?: false
                    } ?: representativeTarget

        return outputItemCollector.outputs.groupBy(SimpleOutputItem::target, SimpleOutputItem::toGeneratedFile)
    }

    private fun registerOutputItems(outputConsumer: OutputConsumer, outputItems: Map<ModuleBuildTarget, List<GeneratedFile>>) {
        for ((target, outputs) in outputItems) {
            for (output in outputs) {
                outputConsumer.registerOutputFile(target, output.outputFile, output.sourceFiles.map { it.path })
            }
        }
    }

    private fun updateLookupStorage(
        lookupTracker: LookupTracker,
        dataManager: BuildDataManager,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder
    ) {
        if (lookupTracker !is LookupTrackerImpl)
            throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker::class.java}")

        dataManager.withLookupStorage { lookupStorage ->
            lookupStorage.removeLookupsFrom(dirtyFilesHolder.allDirtyFiles.asSequence() + dirtyFilesHolder.allRemovedFilesFiles.asSequence())
            lookupStorage.addAll(lookupTracker.lookups.entrySet(), lookupTracker.pathInterner.values)
        }
    }
}

private class JpsICReporter : ICReporter {
    override fun report(message: () -> String) {
        if (KotlinBuilder.LOG.isDebugEnabled) {
            KotlinBuilder.LOG.debug(message())
        }
    }
}

private fun ChangesCollector.processChangesUsingLookups(
    compiledFiles: Set<File>,
    dataManager: BuildDataManager,
    fsOperations: FSOperationsHelper,
    caches: Iterable<JpsIncrementalCache>
) {
    val allCaches = caches.flatMap { it.thisWithDependentCaches }
    val reporter = JpsICReporter()

    reporter.report { "Start processing changes" }

    val dirtyFiles = getDirtyFiles(allCaches, dataManager)
    fsOperations.markInChunkOrDependents(dirtyFiles.asIterable(), excludeFiles = compiledFiles)

    reporter.report { "End of processing changes" }
}

private fun ChangesCollector.getDirtyFiles(
    caches: Iterable<IncrementalCacheCommon>,
    dataManager: BuildDataManager
): Set<File> {
    val reporter = JpsICReporter()
    val (dirtyLookupSymbols, dirtyClassFqNames) = getDirtyData(caches, reporter)
    val dirtyFilesFromLookups = dataManager.withLookupStorage {
        mapLookupSymbolsToFiles(it, dirtyLookupSymbols, reporter)
    }
    return dirtyFilesFromLookups + mapClassesFqNamesToFiles(caches, dirtyClassFqNames, reporter)
}

private fun getLookupTracker(project: JpsProject): LookupTracker {
    val testLookupTracker = project.testingContext?.lookupTracker ?: LookupTracker.DO_NOTHING

    if (IncrementalCompilation.isEnabled()) return LookupTrackerImpl(testLookupTracker)

    return testLookupTracker
}

private fun getIncrementalCaches(
    chunk: ModuleChunk,
    context: CompileContext
): Map<ModuleBuildTarget, JpsIncrementalCache> {
    val dataManager = context.projectDescriptor.dataManager
    val kotlinBuildTargets = context.kotlinBuildTargets

    val chunkCaches = chunk.targets.keysToMapExceptNulls {
        dataManager.getKotlinCache(kotlinBuildTargets[it])
    }

    val dependentTargets = getDependentTargets(chunkCaches.keys, context)

    val dependentCaches = dependentTargets.mapNotNull {
        dataManager.getKotlinCache(kotlinBuildTargets[it])
    }

    for (chunkCache in chunkCaches.values) {
        for (dependentCache in dependentCaches) {
            chunkCache.addJpsDependentCache(dependentCache)
        }
    }

    return chunkCaches
}

fun getDependentTargets(
    compilingChunkTargets: Set<ModuleBuildTarget>,
    context: CompileContext
): Set<ModuleBuildTarget> {
    if (compilingChunkTargets.isEmpty()) return setOf()

    val compilingChunkModules: Set<JpsModule> = compilingChunkTargets.mapTo(mutableSetOf()) { it.module }
    val compilingChunkIsTests = compilingChunkTargets.any { it.isTests }
    val classpathKind = JpsJavaClasspathKind.compile(compilingChunkIsTests)

    fun dependsOnCompilingChunk(target: BuildTarget<*>): Boolean {
        if (target !is ModuleBuildTarget || compilingChunkIsTests && !target.isTests) return false

        val dependencies = getDependenciesRecursively(target.module, classpathKind)
        return ContainerUtil.intersects(dependencies, compilingChunkModules)
    }

    val dependentTargets = HashSet<ModuleBuildTarget>()
    val sortedChunks = context.projectDescriptor.buildTargetIndex.getSortedTargetChunks(context).iterator()

    // skip chunks that are compiled before compilingChunk
    while (sortedChunks.hasNext()) {
        if (sortedChunks.next().targets == compilingChunkTargets) break
    }

    // process chunks that compiled after compilingChunk
    for (followingChunk in sortedChunks) {
        if (followingChunk.targets.none(::dependsOnCompilingChunk)) continue

        dependentTargets.addAll(followingChunk.targets.filterIsInstance<ModuleBuildTarget>())
    }

    return dependentTargets
}

private fun getDependenciesRecursively(module: JpsModule, kind: JpsJavaClasspathKind): Set<JpsModule> =
    JpsJavaExtensionService.dependencies(module).includedIn(kind).recursivelyExportedOnly().modules

fun jvmBuildMetaInfoFile(target: ModuleBuildTarget, dataManager: BuildDataManager): File =
    File(dataManager.dataPaths.getTargetDataRoot(target), KotlinBuilder.JVM_BUILD_META_INFO_FILE_NAME)