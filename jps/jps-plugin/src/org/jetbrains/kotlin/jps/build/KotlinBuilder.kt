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
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import gnu.trove.THashSet
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaBuilderUtil
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.*
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode.*
import org.jetbrains.jps.incremental.java.JavaBuilder
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmBuildMetaInfo
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.CompilerRunnerConstants
import org.jetbrains.kotlin.config.CompilerRunnerConstants.INTERNAL_ERROR_PREFIX
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings
import org.jetbrains.kotlin.jps.build.JpsJsModuleUtils.getOutputMetaFile
import org.jetbrains.kotlin.jps.incremental.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.preloading.ClassCondition
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.*
import org.jetbrains.org.objectweb.asm.ClassReader
import java.io.File
import java.util.*

class KotlinBuilder : ModuleLevelBuilder(BuilderCategory.SOURCE_PROCESSOR) {
    companion object {
        @JvmField val KOTLIN_BUILDER_NAME: String = "Kotlin Builder"

        val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")
        const val JVM_BUILD_META_INFO_FILE_NAME = "jvm-build-meta-info.txt"
        const val SKIP_CACHE_VERSION_CHECK_PROPERTY = "kotlin.jps.skip.cache.version.check"
        const val JPS_KOTLIN_HOME_PROPERTY = "jps.kotlin.home"
    }

    private val statisticsLogger = TeamcityStatisticsLogger()

    override fun getPresentableName() = KOTLIN_BUILDER_NAME

    override fun getCompilableFileExtensions() = arrayListOf("kt")

    override fun buildStarted(context: CompileContext) {
        LOG.debug("==========================================")
        LOG.info("is Kotlin incremental compilation enabled: ${IncrementalCompilation.isEnabled()}")
        LOG.info("is Kotlin experimental incremental compilation enabled: ${IncrementalCompilation.isExperimental()}")
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

        context.testingContext?.buildLogger?.buildStarted(context, chunk)

        if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) return

        val targets = chunk.targets
        val dataManager = context.projectDescriptor.dataManager
        val hasKotlin = HasKotlinMarker(dataManager)

        if (targets.none { hasKotlin[it] == true }) return

        if (System.getProperty(SKIP_CACHE_VERSION_CHECK_PROPERTY) == null) {
            checkCachesVersions(context, chunk)
        }
    }

    private fun checkCachesVersions(context: CompileContext, chunk: ModuleChunk) {
        val targets = chunk.targets
        val dataManager = context.projectDescriptor.dataManager

        val cacheVersionsProvider = CacheVersionProvider(dataManager.dataPaths)
        val allVersions = cacheVersionsProvider.allVersions(targets)
        val actions = allVersions.map { it.checkVersion() }.toMutableSet()

        if (!JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            val args = compilerArgumentsForChunk(chunk)
            val currentBuildMetaInfo = JvmBuildMetaInfo(args)

            for (target in chunk.targets) {
                val file = jvmBuildMetaInfoFile(target, dataManager)
                if (!file.exists()) continue

                val lastBuildMetaInfo =
                        try {
                            JvmBuildMetaInfo.deserializeFromString(file.readText()) ?: continue
                        }
                        catch (e: Exception) {
                            LOG.error("Could not deserialize jvm build meta info", e)
                            continue
                        }

                val lastBuildLangVersion = LanguageVersion.fromVersionString(lastBuildMetaInfo.languageVersionString)
                // reuse logic from compiler?
                if (lastBuildLangVersion != LanguageVersion.KOTLIN_1_0
                    && lastBuildMetaInfo.isEAP
                    && !currentBuildMetaInfo.isEAP
                ) {
                    // If EAP->Non-EAP build with IC, then rebuild all kotlin
                    LOG.info("Last build was compiled with EAP-plugin. Performing non-incremental rebuild (kotlin only)")
                    actions.add(CacheVersion.Action.REBUILD_ALL_KOTLIN)
                }
            }
        }

        val fsOperations = FSOperationsHelper(context, chunk, LOG)
        applyActionsOnCacheVersionChange(actions, cacheVersionsProvider, context, dataManager, targets, fsOperations)
    }

    override fun chunkBuildFinished(context: CompileContext?, chunk: ModuleChunk?) {
        super.chunkBuildFinished(context, chunk)

        LOG.debug("------------------------------------------")
    }

    override fun build(
            context: CompileContext,
            chunk: ModuleChunk,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            outputConsumer: ModuleLevelBuilder.OutputConsumer
    ): ModuleLevelBuilder.ExitCode {
        val messageCollector = MessageCollectorAdapter(context)
        val fsOperations = FSOperationsHelper(context, chunk, LOG)

        try {
            val proposedExitCode = doBuild(chunk, context, dirtyFilesHolder, messageCollector, outputConsumer, fsOperations)

            val actualExitCode = if (proposedExitCode == OK && fsOperations.hasMarkedDirty) ADDITIONAL_PASS_REQUIRED else proposedExitCode

            LOG.debug("Build result: " + actualExitCode)

            context.testingContext?.buildLogger?.buildFinished(actualExitCode)

            return actualExitCode
        }
        catch (e: StopBuildException) {
            LOG.info("Caught exception: " + e)
            throw e
        }
        catch (e: Throwable) {
            LOG.info("Caught exception: " + e)
            MessageCollectorUtil.reportException(messageCollector, e)
            return ABORT
        }
    }

    private fun doBuild(
            chunk: ModuleChunk,
            context: CompileContext,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            messageCollector: MessageCollectorAdapter,
            outputConsumer: OutputConsumer,
            fsOperations: FSOperationsHelper
    ): ModuleLevelBuilder.ExitCode {
        // Workaround for Android Studio
        if (!JavaBuilder.IS_ENABLED[context, true] && !JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
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

        if (!hasKotlinDirtyOrRemovedFiles(dirtyFilesHolder, chunk)) {
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
        val incrementalCaches = getIncrementalCaches(chunk, context)
        val environment = createCompileEnvironment(incrementalCaches, lookupTracker, context, messageCollector) ?: return ABORT

        val commonArguments = compilerArgumentsForChunk(chunk).apply {
            reportOutputFiles = true
            version = true // Always report the version to help diagnosing user issues if they submit the compiler output
        }

        val allCompiledFiles = getAllCompiledFilesContainer(context)
        val filesToCompile = KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder)

        LOG.debug("Compiling files: ${filesToCompile.values()}")

        val start = System.nanoTime()
        val outputItemCollector = doCompileModuleChunk(allCompiledFiles, chunk, commonArguments, context, dirtyFilesHolder,
                                                       environment, filesToCompile, incrementalCaches, project)

        statisticsLogger.registerStatistic(chunk, System.nanoTime() - start)

        if (outputItemCollector == null) {
            return NOTHING_DONE
        }

        val compilationErrors = Utils.ERRORS_DETECTED_KEY[context, false]
        if (compilationErrors) {
            LOG.info("Compiled with errors")
            return ABORT
        }
        else {
            LOG.info("Compiled successfully")
        }

        val generatedFiles = getGeneratedFiles(chunk, environment.outputItemsCollector)

        registerOutputItems(outputConsumer, generatedFiles)
        saveVersions(context, chunk, commonArguments)

        if (targets.any { hasKotlin[it] == null }) {
            fsOperations.markChunk(recursively = false, kotlinOnly = true, excludeFiles = filesToCompile.values().toSet())
        }

        for (target in targets) {
            hasKotlin[target] = true
            rebuildAfterCacheVersionChanged.clean(target)
        }

        if (JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            copyJsLibraryFilesIfNeeded(chunk)
            return OK
        }

        @Suppress("REIFIED_TYPE_UNSAFE_SUBSTITUTION")
        val generatedClasses = generatedFiles.filterIsInstance<GeneratedJvmClass<ModuleBuildTarget>>()
        updateJavaMappings(chunk, compilationErrors, context, dirtyFilesHolder, filesToCompile, generatedClasses, incrementalCaches)

        if (!IncrementalCompilation.isEnabled()) {
            return OK
        }

        context.checkCanceled()

        val changesInfo = updateKotlinIncrementalCache(compilationErrors, incrementalCaches, generatedFiles)
        updateLookupStorage(chunk, lookupTracker, dataManager, dirtyFilesHolder, filesToCompile)

        if (isChunkRebuilding) {
            return OK
        }

        processChanges(filesToCompile.values().toSet(), allCompiledFiles, dataManager, incrementalCaches.values, changesInfo, fsOperations)
        incrementalCaches.values.forEach { it.cleanDirtyInlineFunctions() }

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
        val buildTargetIndex = context.projectDescriptor.buildTargetIndex
        val allTargets = buildTargetIndex.allTargets.filterIsInstance<ModuleBuildTarget>().toSet()
        val hasKotlin = HasKotlinMarker(dataManager)
        val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)

        val sortedActions = actions.sorted()

        context.testingContext?.buildLogger?.actionsOnCacheVersionChanged(sortedActions)

        for (status in sortedActions) {
            when (status) {
                CacheVersion.Action.REBUILD_ALL_KOTLIN -> {
                    LOG.info("Kotlin global lookup map format changed, so rebuild all kotlin")
                    val project = context.projectDescriptor.project
                    val sourceRoots = project.modules.flatMap { it.sourceRoots }

                    for (sourceRoot in sourceRoots) {
                        val ktFiles = sourceRoot.file.walk().filter { KotlinSourceFileCollector.isKotlinSourceFile(it) }
                        fsOperations.markFiles(ktFiles.asIterable())
                    }

                    for (target in allTargets) {
                        dataManager.getKotlinCache(target).clean()
                        rebuildAfterCacheVersionChanged[target] = true
                    }

                    dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider).clean()
                    return
                }
                CacheVersion.Action.REBUILD_CHUNK -> {
                    LOG.info("Clearing caches for " + targets.joinToString { it.presentableName })

                    for (target in targets) {
                        dataManager.getKotlinCache(target).clean()
                        hasKotlin.clean(target)
                        rebuildAfterCacheVersionChanged[target] = true
                    }

                    fsOperations.markChunk(recursively = false, kotlinOnly = true)

                    return
                }
                CacheVersion.Action.CLEAN_NORMAL_CACHES -> {
                    LOG.info("Clearing caches for all targets")

                    for (target in allTargets) {
                        dataManager.getKotlinCache(target).clean()
                    }
                }
                CacheVersion.Action.CLEAN_EXPERIMENTAL_CACHES -> {
                    LOG.info("Clearing experimental caches for all targets")

                    for (target in allTargets) {
                        dataManager.getKotlinCache(target).cleanExperimental()
                    }
                }
                CacheVersion.Action.CLEAN_DATA_CONTAINER -> {
                    LOG.info("Clearing lookup cache")
                    dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider).clean()
                    cacheVersionsProvider.dataContainerVersion().clean()
                }
                else -> {
                    assert(status == CacheVersion.Action.DO_NOTHING) { "Unknown version status $status" }
                }
            }
        }
    }

    private fun saveVersions(context: CompileContext, chunk: ModuleChunk, commonArguments: CommonCompilerArguments) {
        val dataManager = context.projectDescriptor.dataManager
        val targets = chunk.targets
        val cacheVersionsProvider = CacheVersionProvider(dataManager.dataPaths)
        cacheVersionsProvider.allVersions(targets).forEach { it.saveIfNeeded() }

        if (!JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            val jvmBuildMetaInfo = JvmBuildMetaInfo(commonArguments)
            val serializedMetaInfo = JvmBuildMetaInfo.serializeToString(jvmBuildMetaInfo)

            for (target in chunk.targets) {
                jvmBuildMetaInfoFile(target, dataManager).writeText(serializedMetaInfo)
            }
        }
    }

    private fun compilerArgumentsForChunk(chunk: ModuleChunk): CommonCompilerArguments =
            JpsKotlinCompilerSettings.getCommonCompilerArguments(chunk.representativeTarget().module)

    private fun doCompileModuleChunk(
            allCompiledFiles: MutableSet<File>, chunk: ModuleChunk, commonArguments: CommonCompilerArguments, context: CompileContext,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>, environment: JpsCompilerEnvironment,
            filesToCompile: MultiMap<ModuleBuildTarget, File>, incrementalCaches: Map<ModuleBuildTarget, IncrementalCacheImpl<*>>,
            project: JpsProject
    ): OutputItemsCollector? {

        if (JpsUtils.isJsKotlinModule(chunk.representativeTarget())) {
            LOG.debug("Compiling to JS ${filesToCompile.values().size} files in ${filesToCompile.keySet().joinToString { it.presentableName }}")
            return compileToJs(chunk, commonArguments, environment, project)
        }

        if (IncrementalCompilation.isEnabled()) {
            for (target in chunk.targets) {
                val cache = incrementalCaches[target]!!
                val removedAndDirtyFiles = filesToCompile[target] + dirtyFilesHolder.getRemovedFiles(target).map(::File)
                cache.markOutputClassesDirty(removedAndDirtyFiles)
            }
        }

        val representativeTarget = chunk.representativeTarget()

        fun concatenate(strings: Array<String>?, cp: List<String>) = arrayOf(*strings.orEmpty(), *cp.toTypedArray())

        for (argumentProvider in ServiceLoader.load(KotlinJpsCompilerArgumentsProvider::class.java)) {
            // appending to pluginOptions
            commonArguments.pluginOptions = concatenate(commonArguments.pluginOptions,
                                                        argumentProvider.getExtraArguments(representativeTarget, context))
            // appending to classpath
            commonArguments.pluginClasspaths = concatenate(commonArguments.pluginClasspaths,
                                                           argumentProvider.getClasspath(representativeTarget, context))

            LOG.debug("Plugin loaded: ${argumentProvider::class.java.simpleName}")
        }

        return compileToJvm(allCompiledFiles, chunk, commonArguments, context, dirtyFilesHolder, environment, filesToCompile)
    }

    private fun createCompileEnvironment(
            incrementalCaches: Map<ModuleBuildTarget, IncrementalCache>,
            lookupTracker: LookupTracker,
            context: CompileContext,
            messageCollector: MessageCollectorAdapter
    ): JpsCompilerEnvironment? {
        val compilerServices = with(Services.Builder()) {
            register(IncrementalCompilationComponents::class.java,
                  IncrementalCompilationComponentsImpl(incrementalCaches.mapKeys { TargetId(it.key) },
                                                       lookupTracker))
            register(CompilationCanceledStatus::class.java, object : CompilationCanceledStatus {
                override fun checkCanceled() {
                    if (context.cancelStatus.isCanceled) throw CompilationCanceledException()
                }
            })
            build()
        }

        val paths = computeKotlinPathsForJpsPlugin()
        if (paths == null || !paths.homePath.exists()) {
            messageCollector.report(ERROR, "Cannot find kotlinc home. Make sure the plugin is properly installed, " +
                                           "or specify $JPS_KOTLIN_HOME_PROPERTY system property")
            return null
        }

        return JpsCompilerEnvironment(
                paths,
                compilerServices,
                ClassCondition { className ->
                    className.startsWith("org.jetbrains.kotlin.load.kotlin.incremental.components.")
                    || className.startsWith("org.jetbrains.kotlin.incremental.components.")
                    || className == "org.jetbrains.kotlin.config.Services"
                    || className.startsWith("org.apache.log4j.") // For logging from compiler
                    || className == "org.jetbrains.kotlin.progress.CompilationCanceledStatus"
                    || className == "org.jetbrains.kotlin.progress.CompilationCanceledException"
                    || className == "org.jetbrains.kotlin.modules.TargetId"
                },
                messageCollector,
                OutputItemsCollectorImpl()
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
            chunk: ModuleChunk,
            outputItemCollector: OutputItemsCollectorImpl
    ): List<GeneratedFile<ModuleBuildTarget>> {
        // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
        val sourceToTarget = HashMap<File, ModuleBuildTarget>()
        if (chunk.targets.size > 1) {
            for (target in chunk.targets) {
                for (file in KotlinSourceFileCollector.getAllKotlinSourceFiles(target)) {
                    sourceToTarget.put(file, target)
                }
            }
        }

        val result = ArrayList<GeneratedFile<ModuleBuildTarget>>()

        val representativeTarget = chunk.representativeTarget()
        for (outputItem in outputItemCollector.outputs) {
            val sourceFiles = outputItem.sourceFiles
            val outputFile = outputItem.outputFile
            val target =
                    sourceFiles.firstOrNull()?.let { sourceToTarget[it] } ?:
                    chunk.targets.singleOrNull { it.outputDir?.let { outputFile.startsWith(it) } ?: false } ?:
                    representativeTarget

            if (outputFile.name.endsWith(".class")) {
                result.add(GeneratedJvmClass(target, sourceFiles, outputFile))
            }
            else {
                result.add(GeneratedFile<ModuleBuildTarget>(target, sourceFiles, outputFile))
            }
        }
        return result
    }

    private fun updateJavaMappings(
            chunk: ModuleChunk,
            compilationErrors: Boolean,
            context: CompileContext,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            filesToCompile: MultiMap<ModuleBuildTarget, File>,
            generatedClasses: List<GeneratedJvmClass<ModuleBuildTarget>>,
            incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCacheImpl>
    ) {
        val previousMappings = context.projectDescriptor.dataManager.mappings
        val callback = JavaBuilderUtil.getDependenciesRegistrar(context)

        val targetDirtyFiles: Map<ModuleBuildTarget, Set<File>> = chunk.targets.keysToMap {
            val files = HashSet<File>()
            dirtyFilesHolder.getRemovedFiles(it).mapTo(files, ::File)
            files.addAll(filesToCompile.get(it))
            files
        }

        fun getOldSourceFiles(generatedClass: GeneratedJvmClass<ModuleBuildTarget>): Set<File> {
            val cache = incrementalCaches[generatedClass.target] ?: return emptySet()
            val className = generatedClass.outputClass.className

            if (!cache.isMultifileFacade(className)) return emptySet()

            val name = previousMappings.getName(className.internalName)
            return previousMappings.getClassSources(name)?.toSet() ?: emptySet()
        }

        for (generatedClass in generatedClasses) {
            val sourceFiles = THashSet(FileUtil.FILE_HASHING_STRATEGY)
            sourceFiles.addAll(getOldSourceFiles(generatedClass))
            sourceFiles.removeAll(targetDirtyFiles[generatedClass.target] ?: emptySet())
            sourceFiles.addAll(generatedClass.sourceFiles)

            callback.associate(
                    FileUtil.toSystemIndependentName(generatedClass.outputFile.canonicalPath),
                    sourceFiles.map { FileUtil.toSystemIndependentName(it.canonicalPath) },
                    ClassReader(generatedClass.outputClass.fileContents)
            )
        }

        val allCompiled = filesToCompile.values()
        val successfullyCompiled = if (compilationErrors) listOf<File>() else allCompiled

        JavaBuilderUtil.registerFilesToCompile(context, allCompiled)
        JavaBuilderUtil.registerSuccessfullyCompiled(context, successfullyCompiled)
    }

    private fun registerOutputItems(outputConsumer: ModuleLevelBuilder.OutputConsumer, generatedFiles: List<GeneratedFile<ModuleBuildTarget>>) {
        for (generatedFile in generatedFiles) {
            outputConsumer.registerOutputFile(generatedFile.target, generatedFile.outputFile, generatedFile.sourceFiles.map { it.path })
        }
    }

    private fun updateKotlinIncrementalCache(
            compilationErrors: Boolean,
            incrementalCaches: Map<ModuleBuildTarget, JpsIncrementalCacheImpl>,
            generatedFiles: List<GeneratedFile<ModuleBuildTarget>>
    ): CompilationResult {

        assert(IncrementalCompilation.isEnabled()) { "updateKotlinIncrementalCache should not be called when incremental compilation disabled" }

        var changesInfo = CompilationResult.NO_CHANGES
        for (generatedFile in generatedFiles) {
            val ic = incrementalCaches[generatedFile.target]!!
            val newChangesInfo =
                    if (generatedFile is GeneratedJvmClass<ModuleBuildTarget>) {
                        ic.saveFileToCache(generatedFile)
                    }
                    else if (generatedFile.outputFile.isModuleMappingFile()) {
                        ic.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
                    }
                    else {
                        continue
                    }

            changesInfo += newChangesInfo
        }

        if (!compilationErrors) {
            incrementalCaches.values.forEach {
                val newChangesInfo = it.clearCacheForRemovedClasses()
                changesInfo += newChangesInfo
            }
        }

        return changesInfo
    }

    private fun updateLookupStorage(
            chunk: ModuleChunk,
            lookupTracker: LookupTracker,
            dataManager: BuildDataManager,
            dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
            filesToCompile: MultiMap<ModuleBuildTarget, File>
    ) {
        if (!IncrementalCompilation.isExperimental()) return

        if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker::class.java}")

        val lookupStorage = dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider)

        val removedFiles = chunk.targets.flatMap { KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, it) }
        lookupStorage.removeLookupsFrom(filesToCompile.values().asSequence() + removedFiles.asSequence())

        lookupStorage.addAll(lookupTracker.lookups.entrySet(), lookupTracker.pathInterner.values)
    }

    // if null is returned, nothing was done
    private fun compileToJs(chunk: ModuleChunk,
                            commonArguments: CommonCompilerArguments,
                            environment: JpsCompilerEnvironment,
                            project: JpsProject
    ): OutputItemsCollector? {
        val representativeTarget = chunk.representativeTarget()
        if (chunk.modules.size > 1) {
            // We do not support circular dependencies, but if they are present, we do our best should not break the build,
            // so we simply yield a warning and report NOTHING_DONE
            environment.messageCollector.report(
                    STRONG_WARNING,
                    "Circular dependencies are not supported. The following JS modules depend on each other: "
                    + chunk.modules.joinToString(", ") { it.name } + ". "
                    + "Kotlin is not compiled for these modules"
            )
            return null
        }

        val sourceFiles = KotlinSourceFileCollector.getAllKotlinSourceFiles(representativeTarget)
        if (sourceFiles.isEmpty()) {
            return null
        }

        val outputDir = KotlinBuilderModuleScriptGenerator.getOutputDirSafe(representativeTarget)

        val representativeModule = representativeTarget.module
        val moduleName = representativeModule.name
        val outputFile = JpsJsModuleUtils.getOutputFile(outputDir, moduleName, representativeTarget.isTests)
        val libraries = JpsJsModuleUtils.getLibraryFilesAndDependencies(representativeTarget)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(representativeModule)
        val k2JsArguments = JpsKotlinCompilerSettings.getK2JsCompilerArguments(representativeModule)

        val sourceRoots = KotlinSourceFileCollector.getRelevantSourceRoots(representativeTarget).map { it.file }

        val friendPaths = KotlinBuilderModuleScriptGenerator.getProductionModulesWhichInternalsAreVisible(representativeTarget).mapNotNull {
            val file = getOutputMetaFile(it, false)
            if (file.exists()) file.absolutePath.toString() else null
        }

        val compilerRunner = JpsKotlinCompilerRunner()
        compilerRunner.runK2JsCompiler(commonArguments, k2JsArguments, compilerSettings, environment, sourceFiles, sourceRoots,
                                       libraries, friendPaths, outputFile)
        return environment.outputItemsCollector
    }

    private fun copyJsLibraryFilesIfNeeded(chunk: ModuleChunk) {
        val representativeTarget = chunk.representativeTarget()
        val outputDir = KotlinBuilderModuleScriptGenerator.getOutputDirSafe(representativeTarget)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(representativeTarget.module)
        val k2jsCompilerSettings = JpsKotlinCompilerSettings.getK2JsCompilerArguments(representativeTarget.module)
        if (compilerSettings.copyJsLibraryFiles) {
            val outputLibraryRuntimeDirectory = File(outputDir, compilerSettings.outputDirectoryForJsLibraryFiles).absolutePath
            val libraryFilesToCopy = arrayListOf<String>()
            JpsJsModuleUtils.getLibraryFiles(representativeTarget, libraryFilesToCopy)
            JsLibraryUtils.copyJsFilesFromLibraries(libraryFilesToCopy, outputLibraryRuntimeDirectory,
                                                    copySourceMap = k2jsCompilerSettings.sourceMap)
        }
    }

    // if null is returned, nothing was done
    private fun compileToJvm(allCompiledFiles: MutableSet<File>,
                             chunk: ModuleChunk,
                             commonArguments: CommonCompilerArguments,
                             context: CompileContext,
                             dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
                             environment: JpsCompilerEnvironment,
                             filesToCompile: MultiMap<ModuleBuildTarget, File>
    ): OutputItemsCollector? {
        if (chunk.modules.size > 1) {
            environment.messageCollector.report(
                    STRONG_WARNING,
                    "Circular dependencies are only partially supported. The following modules depend on each other: "
                    + chunk.modules.joinToString(", ") { it.name } + ". "
                    + "Kotlin will compile them, but some strange effect may happen"
            )
        }

        allCompiledFiles.addAll(filesToCompile.values())

        val processedTargetsWithRemoved = getProcessedTargetsWithRemovedFilesContainer(context)

        var totalRemovedFiles = 0
        for (target in chunk.targets) {
            val removedFilesInTarget = KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, target)
            if (!removedFilesInTarget.isEmpty()) {
                if (processedTargetsWithRemoved.add(target)) {
                    totalRemovedFiles += removedFilesInTarget.size
                }
            }
        }

        val moduleFile = KotlinBuilderModuleScriptGenerator.generateModuleDescription(context, chunk, filesToCompile, totalRemovedFiles != 0)
        if (moduleFile == null) {
            KotlinBuilder.LOG.debug("Not compiling, because no files affected: " + filesToCompile.keySet().joinToString { it.presentableName })
            // No Kotlin sources found
            return null
        }

        val module = chunk.representativeTarget().module
        val k2JvmArguments = JpsKotlinCompilerSettings.getK2JvmCompilerArguments(module)
        val compilerSettings = JpsKotlinCompilerSettings.getCompilerSettings(module)

        KotlinBuilder.LOG.debug("Compiling to JVM ${filesToCompile.values().size} files"
                                + (if (totalRemovedFiles == 0) "" else " ($totalRemovedFiles removed files)")
                                + " in " + filesToCompile.keySet().joinToString { it.presentableName })

        val compilerRunner = JpsKotlinCompilerRunner()
        compilerRunner.runK2JvmCompiler(commonArguments, k2JvmArguments, compilerSettings, environment, moduleFile)
        moduleFile.delete()

        return environment.outputItemsCollector
    }

    class MessageCollectorAdapter(private val context: CompileContext) : MessageCollector {
        private var hasErrors = false

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            hasErrors = hasErrors or severity.isError
            var prefix = ""
            if (severity == EXCEPTION) {
                prefix = INTERNAL_ERROR_PREFIX
            }
            val kind = kind(severity)
            if (kind != null) {
                context.processMessage(CompilerMessage(
                        CompilerRunnerConstants.KOTLIN_COMPILER_NAME,
                        kind,
                        prefix + message,
                        location?.path,
                        -1, -1, -1,
                        location?.line?.toLong() ?: -1,
                        location?.column?.toLong() ?: -1
                ))
            }
            else {
                val path = if (location != null) "${location.path}:${location.line}:${location.column}: " else ""
                KotlinBuilder.LOG.debug(path + message)
            }
        }

        override fun clear() {
            hasErrors = false
        }

        override fun hasErrors(): Boolean = hasErrors

        private fun kind(severity: CompilerMessageSeverity): BuildMessage.Kind? {
            return when (severity) {
                INFO -> BuildMessage.Kind.INFO
                ERROR, EXCEPTION -> BuildMessage.Kind.ERROR
                WARNING, STRONG_WARNING -> BuildMessage.Kind.WARNING
                LOGGING -> null
                else -> throw IllegalArgumentException("Unsupported severity: " + severity)
            }
        }
    }
}

private fun processChanges(
        compiledFiles: Set<File>,
        allCompiledFiles: MutableSet<File>,
        dataManager: BuildDataManager,
        caches: Collection<JpsIncrementalCacheImpl>,
        compilationResult: CompilationResult,
        fsOperations: FSOperationsHelper
) {
    if (IncrementalCompilation.isExperimental()) {
        compilationResult.doProcessChangesUsingLookups(compiledFiles, dataManager, fsOperations, caches)
    }
    else {
        compilationResult.doProcessChanges(compiledFiles, allCompiledFiles, caches, fsOperations)
    }
}

private fun CompilationResult.doProcessChanges(
        compiledFiles: Set<File>,
        allCompiledFiles: MutableSet<File>,
        caches: Collection<JpsIncrementalCacheImpl>,
        fsOperations: FSOperationsHelper
) {
    KotlinBuilder.LOG.debug("compilationResult = $this")

    when {
        inlineAdded -> {
            allCompiledFiles.clear()
            fsOperations.markChunk(recursively = true, kotlinOnly = true, excludeFiles = compiledFiles)
            return
        }
        constantsChanged -> {
            fsOperations.markChunk(recursively = true, kotlinOnly = false, excludeFiles = allCompiledFiles)
            return
        }
        protoChanged -> {
            fsOperations.markChunk(recursively = false, kotlinOnly = true, excludeFiles = allCompiledFiles)
        }
    }

    if (inlineChanged) {
        val files = caches.flatMap { it.getFilesToReinline() }
        fsOperations.markFiles(files, excludeFiles = compiledFiles)
    }
}

private class JpsICReporter : ICReporter {
    override fun report(message: ()->String) {
        if (KotlinBuilder.LOG.isDebugEnabled) {
            KotlinBuilder.LOG.debug(message())
        }
    }
}

private fun CompilationResult.doProcessChangesUsingLookups(
        compiledFiles: Set<File>,
        dataManager: BuildDataManager,
        fsOperations: FSOperationsHelper,
        caches: Iterable<IncrementalCacheImpl<ModuleBuildTarget>>
) {
    val lookupStorage = dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider)
    val allCaches = caches.flatMap { it.thisWithDependentCaches }
    val reporter = JpsICReporter()

    reporter.report { "Start processing changes" }

    val (dirtyLookupSymbols, dirtyClassFqNames) = getDirtyData(allCaches, reporter)
    val dirtyFiles = mapLookupSymbolsToFiles(lookupStorage, dirtyLookupSymbols, reporter) +
                     mapClassesFqNamesToFiles(allCaches, dirtyClassFqNames, reporter)
    fsOperations.markFiles(dirtyFiles.asIterable(), excludeFiles = compiledFiles)

    reporter.report { "End of processing changes" }
}

private fun getLookupTracker(project: JpsProject): LookupTracker {
    val testLookupTracker = project.testingContext?.lookupTracker ?: LookupTracker.DO_NOTHING

    if (IncrementalCompilation.isExperimental()) return LookupTrackerImpl(testLookupTracker)

    return testLookupTracker
}

private fun getIncrementalCaches(chunk: ModuleChunk, context: CompileContext): Map<ModuleBuildTarget, JpsIncrementalCacheImpl> {
    val dependentTargets = getDependentTargets(chunk, context)

    val dataManager = context.projectDescriptor.dataManager
    val chunkCaches = chunk.targets.keysToMap { dataManager.getKotlinCache(it) }
    val dependentCaches = dependentTargets.map { dataManager.getKotlinCache(it) }

    for (chunkCache in chunkCaches.values) {
        for (dependentCache in dependentCaches) {
            chunkCache.addDependentCache(dependentCache)
        }
    }

    return chunkCaches
}

fun getDependentTargets(
        compilingChunk: ModuleChunk,
        context: CompileContext
): Set<ModuleBuildTarget> {
    val compilingChunkIsTests = compilingChunk.targets.any { it.isTests }
    val classpathKind = JpsJavaClasspathKind.compile(compilingChunkIsTests)

    fun dependsOnCompilingChunk(target: BuildTarget<*>): Boolean {
        if (target !is ModuleBuildTarget || compilingChunkIsTests && !target.isTests) return false

        val dependencies = getDependenciesRecursively(target.module, classpathKind)
        return ContainerUtil.intersects(dependencies, compilingChunk.modules)
    }

    val dependentTargets = HashSet<ModuleBuildTarget>()
    val sortedChunks = context.projectDescriptor.buildTargetIndex.getSortedTargetChunks(context).iterator()

    // skip chunks that are compiled before compilingChunk
    while (sortedChunks.hasNext()) {
        if (sortedChunks.next().targets == compilingChunk.targets) break
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

// TODO: investigate thread safety
private val ALL_COMPILED_FILES_KEY = Key.create<MutableSet<File>>("_all_kotlin_compiled_files_")
private fun getAllCompiledFilesContainer(context: CompileContext): MutableSet<File> {
    var allCompiledFiles = ALL_COMPILED_FILES_KEY.get(context)
    if (allCompiledFiles == null) {
        allCompiledFiles = THashSet(FileUtil.FILE_HASHING_STRATEGY)
        ALL_COMPILED_FILES_KEY.set(context, allCompiledFiles)
    }
    return allCompiledFiles
}

// TODO: investigate thread safety
private val PROCESSED_TARGETS_WITH_REMOVED_FILES = Key.create<MutableSet<ModuleBuildTarget>>("_processed_targets_with_removed_files_")
private fun getProcessedTargetsWithRemovedFilesContainer(context: CompileContext): MutableSet<ModuleBuildTarget> {
    var set = PROCESSED_TARGETS_WITH_REMOVED_FILES.get(context)
    if (set == null) {
        set = HashSet<ModuleBuildTarget>()
        PROCESSED_TARGETS_WITH_REMOVED_FILES.set(context, set)
    }
    return set
}

private fun hasKotlinDirtyOrRemovedFiles(
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        chunk: ModuleChunk
): Boolean {
    if (!dirtyFilesHolder.hasDirtyFiles() && !dirtyFilesHolder.hasRemovedFiles()) return false

    if (!KotlinSourceFileCollector.getDirtySourceFiles(dirtyFilesHolder).isEmpty) return true

    return chunk.targets.any { KotlinSourceFileCollector.getRemovedKotlinFiles(dirtyFilesHolder, it).isNotEmpty() }
}

fun jvmBuildMetaInfoFile(target: ModuleBuildTarget, dataManager: BuildDataManager): File =
        File(dataManager.dataPaths.getTargetDataRoot(target), KotlinBuilder.JVM_BUILD_META_INFO_FILE_NAME)
