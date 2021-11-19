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
import org.jetbrains.jps.builders.storage.BuildDataCorruptedException
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode.*
import org.jetbrains.jps.incremental.java.JavaBuilder
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.jps.KotlinJpsBundle
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.JpsLookupStorageManager
import org.jetbrains.kotlin.jps.model.kotlinKind
import org.jetbrains.kotlin.jps.targets.KotlinJvmModuleBuildTarget
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
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
            kotlinContext.reportUnsupportedTargets()
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

        if (!kotlinContext.rebuildingAllKotlin && kotlinChunk.isEnabled) {
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
        )

        val removedClasses = HashSet<String>()
        for (target in kotlinChunk.targets) {
            val cache = incrementalCaches[target] ?: continue
            val dirtyFiles = dirtyFilesHolder.getDirtyFiles(target.jpsModuleBuildTarget).keys
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
        val affectedByRemovedClasses = changesCollector.getDirtyFiles(incrementalCaches.values, kotlinContext.lookupStorageManager)

        fsOperations.markFilesForCurrentRound(affectedByRemovedClasses.dirtyFiles + affectedByRemovedClasses.forceRecompileTogether)
    }

    override fun chunkBuildFinished(context: CompileContext, chunk: ModuleChunk) {
        super.chunkBuildFinished(context, chunk)

        if (chunk.isDummy(context)) return

        // Temporary workaround for KT-33808
        val kotlinContext = ensureKotlinContextInitialized(context)
        for (target in chunk.targets) {
            if (kotlinContext.hasKotlinMarker[target] != true) continue

            val outputRoots = target.getOutputRoots(context)
            if (outputRoots.size > 1) {
                outputRoots.forEach { it.mkdirs() }
            }
        }

        LOG.debug("------------------------------------------")
    }

    override fun build(
        context: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        outputConsumer: OutputConsumer
    ): ExitCode {
        if (chunk.isDummy(context))
            return NOTHING_DONE

        val kotlinTarget = context.kotlin.targetsBinding[chunk.representativeTarget()] ?: return OK
        val messageCollector = MessageCollectorAdapter(context, kotlinTarget)

        // New mpp project model: modules which is imported from sources sets of the compilations shouldn't be compiled for now.
        // It should be compiled only as one of source root of target compilation, which is added in [KotlinSourceRootProvider].
        if (chunk.modules.any { it.kotlinKind == KotlinModuleKind.SOURCE_SET_HOLDER }) {
            if (chunk.modules.size > 1) {
                messageCollector.report(
                    ERROR,
                    KotlinJpsBundle.message("error.text.cyclically.dependent.modules.are.not.supported.in.multiplatform.projects")
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
        } catch (e: BuildDataCorruptedException) {
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
    ): ExitCode {
        // Workaround for Android Studio
        if (representativeTarget is KotlinJvmModuleBuildTarget && !JavaBuilder.IS_ENABLED[context, true]) {
            messageCollector.report(INFO, KotlinJpsBundle.message("info.text.kotlin.jps.plugin.is.disabled"))
            return NOTHING_DONE
        }

        val kotlinContext = context.kotlin
        val kotlinChunk = chunk.toKotlinChunk(context)!!

        if (!kotlinChunk.haveSameCompiler) {
            messageCollector.report(
                ERROR,
                KotlinJpsBundle.message("error.text.cyclically.dependent.modules.0.should.have.same.compiler", kotlinChunk.presentableModulesToCompilersList)
            )
            return ABORT
        }

        if (!kotlinChunk.isEnabled) {
            return NOTHING_DONE
        }

        val projectDescriptor = context.projectDescriptor
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
            messageCollector.report(ERROR,
                                    KotlinJpsBundle.message("error.text.output.directory.not.specified.for.0", targetsWithoutOutputDir.joinToString())
            )
            return ABORT
        }

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
        )

        context.testingContext?.buildLogger?.compilingFiles(
            kotlinDirtyFilesHolder.allDirtyFiles,
            kotlinDirtyFilesHolder.allRemovedFilesFiles
        )

        if (LOG.isDebugEnabled) {
            LOG.debug("Compiling files: ${kotlinDirtyFilesHolder.allDirtyFiles}")
        }

        val start = System.nanoTime()
        val outputItemCollector = doCompileModuleChunk(
            kotlinChunk,
            representativeTarget,
            kotlinChunk.compilerArguments,
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

        markDirtyComplementaryMultifileClasses(generatedFiles, kotlinContext, incrementalCaches, fsOperations)

        val kotlinTargets = kotlinContext.targetsBinding
        for ((target, outputItems) in generatedFiles) {
            val kotlinTarget = kotlinTargets[target] ?: error("Could not find Kotlin target for JPS target $target")
            kotlinTarget.registerOutputItems(outputConsumer, outputItems)
        }
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
                kotlinModuleBuilderTarget.updateCaches(
                    kotlinDirtyFilesHolder,
                    incrementalCaches[kotlinModuleBuilderTarget]!!,
                    files,
                    changesCollector,
                    environment
                )
            }

            updateLookupStorage(lookupTracker, kotlinContext.lookupStorageManager, kotlinDirtyFilesHolder)

            if (!isChunkRebuilding) {
                changesCollector.processChangesUsingLookups(
                    kotlinDirtyFilesHolder.allDirtyFiles,
                    kotlinContext.lookupStorageManager,
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
        representativeTarget: KotlinModuleBuildTarget<*>,
        commonArguments: CommonCompilerArguments,
        context: CompileContext,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        fsOperations: FSOperationsHelper,
        environment: JpsCompilerEnvironment,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>
    ): OutputItemsCollector? {
        kotlinChunk.targets.forEach {
            it.nextRound(context)
        }

        if (representativeTarget.isIncrementalCompilationEnabled) {
            for (target in kotlinChunk.targets) {
                val cache = incrementalCaches[target]
                val jpsTarget = target.jpsModuleBuildTarget

                val targetDirtyFiles = dirtyFilesHolder.byTarget[jpsTarget]
                if (cache != null && targetDirtyFiles != null) {
                    val dirtyFiles = targetDirtyFiles.dirty.keys + targetDirtyFiles.removed
                    val complementaryFiles = cache.getComplementaryFilesRecursive(dirtyFiles)

                    // Get all parts of @JvmMultifileClass file for simultaneous rebuild
                    var dirtyMultifileClassFiles: Collection<File> = emptyList()
                    if (cache is IncrementalJvmCache) {
                        dirtyMultifileClassFiles = cache.classesBySources(dirtyFiles)
                            .filter { cache.isMultifileFacade(it) }
                            .flatMap { cache.getAllPartsOfMultifileFacade(it).orEmpty() }
                            .flatMap { cache.sourcesByInternalName(it) }
                            .distinct()
                            .filter { !dirtyFiles.contains(it) }
                    }
                    fsOperations.markFilesForCurrentRound(jpsTarget, complementaryFiles + dirtyMultifileClassFiles)

                    cache.markDirty(targetDirtyFiles.dirty.keys + targetDirtyFiles.removed)
                }
            }
        }

        val isDoneSomething = representativeTarget.compileModuleChunk(commonArguments, dirtyFilesHolder, environment)

        return if (isDoneSomething) environment.outputItemsCollector else null
    }

    private fun createCompileEnvironment(
        context: CompileContext,
        kotlinModuleBuilderTarget: KotlinModuleBuildTarget<*>,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker,
        chunk: ModuleChunk,
        messageCollector: MessageCollectorAdapter
    ): JpsCompilerEnvironment {
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
                context.kotlin.targetsBinding[target]?.sourceFiles?.forEach {
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

        return outputItemCollector.outputs
            .sortedBy { it.outputFile }
            .groupBy(SimpleOutputItem::target, SimpleOutputItem::toGeneratedFile)
    }

    private fun updateLookupStorage(
        lookupTracker: LookupTracker,
        lookupStorageManager: JpsLookupStorageManager,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder
    ) {
        if (lookupTracker !is LookupTrackerImpl)
            throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker::class.java}")

        lookupStorageManager.withLookupStorage { lookupStorage ->
            lookupStorage.removeLookupsFrom(dirtyFilesHolder.allDirtyFiles.asSequence() + dirtyFilesHolder.allRemovedFilesFiles.asSequence())
            lookupStorage.addAll(lookupTracker.lookups, lookupTracker.pathInterner.values)
        }
    }

    private fun markDirtyComplementaryMultifileClasses(
        generatedFiles: Map<ModuleBuildTarget, List<GeneratedFile>>,
        kotlinContext: KotlinCompileContext,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        fsOperations: FSOperationsHelper
    ) {
        for ((target, files) in generatedFiles) {
            val kotlinModuleBuilderTarget = kotlinContext.targetsBinding[target] ?: continue
            val cache = incrementalCaches[kotlinModuleBuilderTarget] as? IncrementalJvmCache ?: continue
            val generated = files.filterIsInstance<GeneratedJvmClass>()
            val multifileClasses = generated.filter { it.outputClass.classHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS }
            val expectedAllParts = multifileClasses.flatMap { cache.getAllPartsOfMultifileFacade(it.outputClass.className).orEmpty() }
            if (multifileClasses.isEmpty()) continue
            val actualParts = generated.filter { it.outputClass.classHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART }
                .map { it.outputClass.className.toString() }
            if (!actualParts.containsAll(expectedAllParts)) {
                fsOperations.markFiles(expectedAllParts.flatMap { cache.sourcesByInternalName(it) }
                                               + multifileClasses.flatMap { it.sourceFiles })
            }
        }
    }
}

private class JpsICReporter : ICReporterBase() {
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
    }

    override fun report(message: () -> String) {
        if (KotlinBuilder.LOG.isDebugEnabled) {
            KotlinBuilder.LOG.debug(message())
        }
    }

    override fun reportVerbose(message: () -> String) {
        report(message)
    }
}

private fun ChangesCollector.processChangesUsingLookups(
    compiledFiles: Set<File>,
    lookupStorageManager: JpsLookupStorageManager,
    fsOperations: FSOperationsHelper,
    caches: Iterable<JpsIncrementalCache>
) {
    val allCaches = caches.flatMap { it.thisWithDependentCaches }
    val reporter = JpsICReporter()

    reporter.reportVerbose { "Start processing changes" }

    val dirtyFiles = getDirtyFiles(allCaches, lookupStorageManager)
    // if list of inheritors of sealed class has changed it should be recompiled with all the inheritors
    // Here we have a small optimization. Do not recompile the bunch if ALL these files were recompiled during the previous round.
    val excludeFiles = if (compiledFiles.containsAll(dirtyFiles.forceRecompileTogether))
        compiledFiles
    else
        compiledFiles.minus(dirtyFiles.forceRecompileTogether)
    fsOperations.markInChunkOrDependents(
        (dirtyFiles.dirtyFiles + dirtyFiles.forceRecompileTogether).asIterable(),
        excludeFiles = excludeFiles
    )

    reporter.reportVerbose { "End of processing changes" }
}

data class FilesToRecompile(val dirtyFiles: Set<File>, val forceRecompileTogether: Set<File>)

private fun ChangesCollector.getDirtyFiles(
    caches: Iterable<IncrementalCacheCommon>,
    lookupStorageManager: JpsLookupStorageManager
): FilesToRecompile {
    val reporter = JpsICReporter()
    val (dirtyLookupSymbols, dirtyClassFqNames, forceRecompile) = getDirtyData(caches, reporter)
    val dirtyFilesFromLookups = lookupStorageManager.withLookupStorage {
        mapLookupSymbolsToFiles(it, dirtyLookupSymbols, reporter)
    }
    return FilesToRecompile(
        dirtyFilesFromLookups + mapClassesFqNamesToFiles(caches, dirtyClassFqNames, reporter),
        mapClassesFqNamesToFiles(caches, forceRecompile, reporter)
    )

}

private fun getLookupTracker(project: JpsProject, representativeTarget: KotlinModuleBuildTarget<*>): LookupTracker {
    val testLookupTracker = project.testingContext?.lookupTracker ?: LookupTracker.DO_NOTHING

    if (representativeTarget.isIncrementalCompilationEnabled) return LookupTrackerImpl(testLookupTracker)

    return testLookupTracker
}