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
import org.jetbrains.jps.ModuleChunk
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
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.withLookupStorage
import org.jetbrains.kotlin.jps.model.kotlinKind
import org.jetbrains.kotlin.jps.targets.KotlinJvmModuleBuildTarget
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.preloading.ClassCondition
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*
import kotlin.collections.HashSet
import kotlin.system.measureTimeMillis

class KotlinBuilder : ModuleLevelBuilder(BuilderCategory.SOURCE_PROCESSOR) {
    companion object {
        const val KOTLIN_BUILDER_NAME: String = "Kotlin Builder"

        val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")
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

    override fun getCompilableFileExtensions() = arrayListOf("kt", "kts")

    override fun buildStarted(context: CompileContext) {
        logSettings(context)
    }

    private fun logSettings(context: CompileContext) {
        LOG.debug("==========================================")
        LOG.info("is Kotlin incremental compilation enabled for JVM: ${IncrementalCompilation.isEnabledForJvm()}")
        LOG.info("is Kotlin incremental compilation enabled for JS: ${IncrementalCompilation.isEnabledForJs()}")
        if (IncrementalCompilation.isEnabledForJs()) {
            val messageCollector = MessageCollectorAdapter(context, null)
            messageCollector.report(INFO, "Using experimental incremental compilation for Kotlin/JS")
        }

        LOG.info("is Kotlin compiler daemon enabled: ${isDaemonEnabled()}")

        val historyLabel = context.getBuilderParameter("history label")
        if (historyLabel != null) {
            LOG.info("Label in local history: $historyLabel")
        }
    }

    /**
     * Ensure Kotlin Context initialized.
     * Kotlin Context should be initialized only when required (before first kotlin chunk build).
     */
    private fun ensureKotlinContextInitialized(context: CompileContext): KotlinCompileContext {
        val kotlinCompileContext = context.getUserData(kotlinCompileContextKey)
        if (kotlinCompileContext != null) return kotlinCompileContext

        // don't synchronize on context, since it is chunk local only
        synchronized(kotlinCompileContextKey) {
            val actualKotlinCompileContext = context.getUserData(kotlinCompileContextKey)
            if (actualKotlinCompileContext != null) return actualKotlinCompileContext

            try {
                return initializeKotlinContext(context)
            } catch (t: Throwable) {
                jpsReportInternalBuilderError(context, Error("Cannot initialize Kotlin context: ${t.message}", t))
                throw t
            }
        }
    }

    private fun initializeKotlinContext(context: CompileContext): KotlinCompileContext {
        lateinit var kotlinContext: KotlinCompileContext

        val time = measureTimeMillis {
            kotlinContext = KotlinCompileContext(context)

            context.putUserData(kotlinCompileContextKey, kotlinContext)
            context.testingContext?.kotlinCompileContext = kotlinContext

            if (kotlinContext.shouldCheckCacheVersions && kotlinContext.hasKotlin()) {
                kotlinContext.checkCacheVersions()
            }

            kotlinContext.cleanupCaches()
        }

        LOG.info("Total Kotlin global compile context initialization time: $time ms")

        return kotlinContext
    }

    override fun buildFinished(context: CompileContext) {
        ensureKotlinContextDisposed(context)
    }

    private fun ensureKotlinContextDisposed(context: CompileContext) {
        if (context.getUserData(kotlinCompileContextKey) != null) {
            // don't synchronize on context, since it chunk local only
            synchronized(kotlinCompileContextKey) {
                val kotlinCompileContext = context.getUserData(kotlinCompileContextKey)
                if (kotlinCompileContext != null) {
                    kotlinCompileContext.dispose()
                    context.putUserData(kotlinCompileContextKey, null)

                    statisticsLogger.reportTotal()
                }
            }
        }
    }

    override fun chunkBuildStarted(context: CompileContext, chunk: ModuleChunk) {
        super.chunkBuildStarted(context, chunk)

        if (chunk.isDummy(context)) return

        val kotlinContext = ensureKotlinContextInitialized(context)

        val buildLogger = context.testingContext?.buildLogger
        buildLogger?.chunkBuildStarted(context, chunk)

        if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) return

        val targets = chunk.targets
        if (targets.none { kotlinContext.hasKotlinMarker[it] == true }) return

        val kotlinChunk = kotlinContext.getChunk(chunk) ?: return
        kotlinContext.checkChunkCacheVersion(kotlinChunk)

        if (!kotlinContext.rebuildingAllKotlin) {
            markAdditionalFilesForInitialRound(kotlinChunk, chunk, kotlinContext)
        }

        buildLogger?.afterChunkBuildStarted(context, chunk)
    }

    /**
     * Invalidate usages of removed classes.
     * See KT-13677 for more details.
     *
     * todo(1.2.80): move to KotlinChunk
     * todo(1.2.80): got rid of jpsGlobalContext usages (replace with KotlinCompileContext)
     */
    private fun markAdditionalFilesForInitialRound(
        kotlinChunk: KotlinChunk,
        chunk: ModuleChunk,
        kotlinContext: KotlinCompileContext
    ) {
        val context = kotlinContext.jpsContext
        val dirtyFilesHolder = KotlinDirtySourceFilesHolder(
            chunk,
            context,
            object : DirtyFilesHolderBase<JavaSourceRootDescriptor, ModuleBuildTarget>(context) {
                override fun processDirtyFiles(processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>) {
                    FSOperations.processFilesToRecompile(context, chunk, processor)
                }
            }
        )
        val fsOperations = FSOperationsHelper(context, chunk, dirtyFilesHolder, LOG)

        val representativeTarget = kotlinContext.targetsBinding[chunk.representativeTarget()] ?: return

        // dependent caches are not required, since we are not going to update caches
        val incrementalCaches = kotlinChunk.loadCaches(loadDependent = false)

        val messageCollector = MessageCollectorAdapter(context, representativeTarget)
        val environment = createCompileEnvironment(
            kotlinContext.jpsContext,
            representativeTarget,
            incrementalCaches,
            LookupTracker.DO_NOTHING,
            ExpectActualTracker.DoNothing,
            chunk,
            messageCollector
        ) ?: return

        val removedClasses = HashSet<String>()
        for (target in kotlinChunk.targets) {
            val cache = incrementalCaches[target] ?: continue
            val dirtyFiles = dirtyFilesHolder.getDirtyFiles(target.jpsModuleBuildTarget)
            val removedFiles = dirtyFilesHolder.getRemovedFiles(target.jpsModuleBuildTarget)

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

        // New mpp project model: modules which is imported from sources sets of the compilations shouldn't be compiled for now.
        // It should be compiled only as one of source root of target compilation, which is added in [KotlinSourceRootProvider].
        if (chunk.modules.any { it.kotlinKind == KotlinModuleKind.SOURCE_SET_HOLDER }) {
            if (chunk.modules.size > 1) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Cyclically dependent modules are not supported in multiplatform projects"
                )
                return ABORT
            }

            return NOTHING_DONE
        }

        val kotlinDirtyFilesHolder = KotlinDirtySourceFilesHolder(chunk, context, dirtyFilesHolder)
        val fsOperations = FSOperationsHelper(context, chunk, kotlinDirtyFilesHolder, LOG)

        try {
            val proposedExitCode =
                doBuild(chunk, kotlinTarget, context, kotlinDirtyFilesHolder, messageCollector, outputConsumer, fsOperations)

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
        representativeTarget: KotlinModuleBuildTarget<*>,
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

        val kotlinContext = context.kotlin
        val projectDescriptor = context.projectDescriptor
        val dataManager = projectDescriptor.dataManager
        val targets = chunk.targets
        val isChunkRebuilding = JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)
                || targets.any { kotlinContext.rebuildAfterCacheVersionChanged[it] == true }

        if (!kotlinDirtyFilesHolder.hasDirtyOrRemovedFiles) {
            if (isChunkRebuilding) {
                targets.forEach {
                    kotlinContext.hasKotlinMarker[it] = false
                }
            }

            targets.forEach { kotlinContext.rebuildAfterCacheVersionChanged.clean(it) }
            return NOTHING_DONE
        }

        // Request CHUNK_REBUILD when IC is off and there are dirty Kotlin files
        // Otherwise unexpected compile error might happen, when there are Groovy files,
        // but they are not dirty, so Groovy builder does not generate source stubs,
        // and Kotlin builder is filtering out output directory from classpath
        // (because it may contain outdated Java classes).
        if (!isChunkRebuilding && !representativeTarget.isIncrementalCompilationEnabled) {
            targets.forEach { kotlinContext.rebuildAfterCacheVersionChanged[it] = true }
            return CHUNK_REBUILD_REQUIRED
        }

        val targetsWithoutOutputDir = targets.filter { it.outputDir == null }
        if (targetsWithoutOutputDir.isNotEmpty()) {
            messageCollector.report(ERROR, "Output directory not specified for " + targetsWithoutOutputDir.joinToString())
            return ABORT
        }

        val kotlinChunk = chunk.toKotlinChunk(context)!!
        val project = projectDescriptor.project
        val lookupTracker = getLookupTracker(project, representativeTarget)
        val exceptActualTracer = ExpectActualTrackerImpl()
        val incrementalCaches = kotlinChunk.loadCaches()
        val environment = createCompileEnvironment(
            context,
            representativeTarget,
            incrementalCaches,
            lookupTracker,
            exceptActualTracer,
            chunk,
            messageCollector
        ) ?: return ABORT

        val commonArguments = kotlinChunk.compilerArguments.apply {
            reportOutputFiles = true
            version = true // Always report the version to help diagnosing user issues if they submit the compiler output
            if (languageVersion == null) languageVersion = VersionView.RELEASED_VERSION.versionString
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("Compiling files: ${kotlinDirtyFilesHolder.allDirtyFiles}")
        }

        val start = System.nanoTime()
        val outputItemCollector = doCompileModuleChunk(
            kotlinChunk,
            chunk,
            representativeTarget,
            commonArguments,
            context,
            kotlinDirtyFilesHolder,
            fsOperations,
            environment,
            incrementalCaches
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
        kotlinChunk.saveVersions()

        if (targets.any { kotlinContext.hasKotlinMarker[it] == null }) {
            fsOperations.markChunk(recursively = false, kotlinOnly = true, excludeFiles = kotlinDirtyFilesHolder.allDirtyFiles)
        }

        for (target in targets) {
            kotlinContext.hasKotlinMarker[target] = true
            kotlinContext.rebuildAfterCacheVersionChanged.clean(target)
        }

        kotlinChunk.targets.forEach {
            it.doAfterBuild()
        }

        representativeTarget.updateChunkMappings(
            context,
            chunk,
            kotlinDirtyFilesHolder,
            generatedFiles,
            incrementalCaches
        )

        if (!representativeTarget.isIncrementalCompilationEnabled) {
            return OK
        }

        context.checkCanceled()

        environment.withProgressReporter { progress ->
            progress.progress("performing incremental compilation analysis")

            val changesCollector = ChangesCollector()

            for ((target, files) in generatedFiles) {
                val kotlinModuleBuilderTarget = kotlinContext.targetsBinding[target]!!
                kotlinModuleBuilderTarget.updateCaches(incrementalCaches[kotlinModuleBuilderTarget]!!, files, changesCollector, environment)
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

    // todo(1.2.80): got rid of ModuleChunk (replace with KotlinChunk)
    // todo(1.2.80): introduce KotlinRoundCompileContext, move dirtyFilesHolder, fsOperations, environment to it
    private fun doCompileModuleChunk(
        kotlinChunk: KotlinChunk,
        chunk: ModuleChunk,
        representativeTarget: KotlinModuleBuildTarget<*>,
        commonArguments: CommonCompilerArguments,
        context: CompileContext,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        fsOperations: FSOperationsHelper,
        environment: JpsCompilerEnvironment,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>
    ): OutputItemsCollector? {
        loadPlugins(representativeTarget, commonArguments, context)

        kotlinChunk.targets.forEach {
            it.nextRound(context)
        }

        if (representativeTarget.isIncrementalCompilationEnabled) {
            for (target in kotlinChunk.targets) {
                val cache = incrementalCaches[target]
                val jpsTarget = target.jpsModuleBuildTarget
                val targetDirtyFiles = dirtyFilesHolder.byTarget[jpsTarget]

                if (cache != null && targetDirtyFiles != null) {
                    val complementaryFiles = cache.clearComplementaryFilesMapping(
                        targetDirtyFiles.dirty + targetDirtyFiles.removed
                    )

                    fsOperations.markFilesForCurrentRound(jpsTarget, complementaryFiles)

                    cache.markDirty(targetDirtyFiles.dirty + targetDirtyFiles.removed)
                }
            }
        }

        val isDoneSomething = representativeTarget.compileModuleChunk(chunk, commonArguments, dirtyFilesHolder, environment)

        return if (isDoneSomething) environment.outputItemsCollector else null
    }

    private fun loadPlugins(
        representativeTarget: KotlinModuleBuildTarget<*>,
        commonArguments: CommonCompilerArguments,
        context: CompileContext
    ) {
        fun concatenate(strings: Array<String>?, cp: List<String>) = arrayOf(*strings.orEmpty(), *cp.toTypedArray())

        for (argumentProvider in ServiceLoader.load(KotlinJpsCompilerArgumentsProvider::class.java)) {
            val jpsModuleBuildTarget = representativeTarget.jpsModuleBuildTarget
            // appending to pluginOptions
            commonArguments.pluginOptions = concatenate(
                commonArguments.pluginOptions,
                argumentProvider.getExtraArguments(jpsModuleBuildTarget, context)
            )
            // appending to classpath
            commonArguments.pluginClasspaths = concatenate(
                commonArguments.pluginClasspaths,
                argumentProvider.getClasspath(jpsModuleBuildTarget, context)
            )

            LOG.debug("Plugin loaded: ${argumentProvider::class.java.simpleName}")
        }
    }

    private fun createCompileEnvironment(
        context: CompileContext,
        kotlinModuleBuilderTarget: KotlinModuleBuildTarget<*>,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
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
            ProgressReporterImpl(context, chunk)
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
            sourceFiles.firstOrNull()?.let { sourceToTarget[it] }
                ?: chunk.targets.singleOrNull { target ->
                    target.outputDir?.let { outputDir ->
                        outputFile.startsWith(outputDir)
                    } ?: false
                }
                ?: representativeTarget

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

private fun getLookupTracker(project: JpsProject, representativeTarget: KotlinModuleBuildTarget<*>): LookupTracker {
    val testLookupTracker = project.testingContext?.lookupTracker ?: LookupTracker.DO_NOTHING

    if (representativeTarget.isIncrementalCompilationEnabled) return LookupTrackerImpl(testLookupTracker)

    return testLookupTracker
}