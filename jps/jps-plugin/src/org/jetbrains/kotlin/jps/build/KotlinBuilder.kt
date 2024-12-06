/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.XmlSerializerUtil
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
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.report.ICReporter.ReportSeverity
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.CompilationService.Companion.loadImplementation
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.mergeBeans
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.CompilerRunnerUtil.withCompilerClassloader
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner.Companion.filterDuplicatedCompilerPluginOptions
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner.Companion.setupK2JvmArguments
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.additionalArgumentsAsList
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.jps.KotlinJpsBundle
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalCache
import org.jetbrains.kotlin.jps.incremental.JpsLookupStorageManager
import org.jetbrains.kotlin.jps.model.k2JvmCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerSettings
import org.jetbrains.kotlin.jps.model.kotlinKind
import org.jetbrains.kotlin.jps.statistic.JpsBuilderMetricReporter
import org.jetbrains.kotlin.jps.statistic.JpsStatisticsReportService
import org.jetbrains.kotlin.jps.statistic.statisticsReportServiceKey
import org.jetbrains.kotlin.jps.targets.KotlinJvmModuleBuildTarget
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.preloading.ClassCondition
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import kotlin.system.measureTimeMillis

class KotlinBuilder : ModuleLevelBuilder(BuilderCategory.SOURCE_PROCESSOR) {
    companion object {
        @NlsSafe
        const val KOTLIN_BUILDER_NAME: String = "Kotlin Builder"

        val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")
        const val SKIP_CACHE_VERSION_CHECK_PROPERTY = "kotlin.jps.skip.cache.version.check"
        const val JPS_KOTLIN_HOME_PROPERTY = "jps.kotlin.home"

        val useDependencyGraph = System.getProperty("jps.use.dependency.graph", "false")!!.toBoolean()
        val isKotlinBuilderInDumbMode = System.getProperty("kotlin.jps.dumb.mode", "false")!!.toBoolean()
        val enableLookupStorageFillingInDumbMode = System.getProperty("kotlin.jps.enable.lookups.in.dumb.mode", "false")!!.toBoolean()

        private val classesToLoadByParentFromRegistry =
            System.getProperty("kotlin.jps.classesToLoadByParent")?.split(',')?.map { it.trim() } ?: emptyList()
        private val classPrefixesToLoadByParentFromRegistry =
            System.getProperty("kotlin.jps.classPrefixesToLoadByParent")?.split(',')?.map { it.trim() } ?: emptyList()

        val classesToLoadByParent: ClassCondition
            get() = ClassCondition { className ->
                val prefixes = listOf(
                    "org.apache.log4j.", // For logging from compiler
                    "org.jetbrains.kotlin.incremental.components.",
                    "org.jetbrains.kotlin.incremental.js",
                    "org.jetbrains.kotlin.load.kotlin.incremental.components."
                ) + classPrefixesToLoadByParentFromRegistry

                val classes = listOf(
                    "org.jetbrains.kotlin.config.Services",
                    "org.jetbrains.kotlin.progress.CompilationCanceledStatus",
                    "org.jetbrains.kotlin.progress.CompilationCanceledException",
                    "org.jetbrains.kotlin.modules.TargetId",
                    "org.jetbrains.kotlin.cli.common.ExitCode"
                ) + classesToLoadByParentFromRegistry

                prefixes.forEach { if (className.startsWith(it)) return@ClassCondition true }
                classes.forEach { if (className == it) return@ClassCondition true }

                return@ClassCondition false
            }
    }

    private val statisticsLogger = TeamcityStatisticsLogger()

    override fun getPresentableName() = KOTLIN_BUILDER_NAME

    override fun getCompilableFileExtensions() = arrayListOf("kt", "kts")

    override fun buildStarted(context: CompileContext) {
        logSettings(context)
        val reportService = JpsStatisticsReportService.create()
        context.putUserData(statisticsReportServiceKey, reportService)
        reportService.buildStarted(context)
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
        val reportService = JpsStatisticsReportService.getFromContext(context)
        reportService.buildFinish(context)
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

        if (!isKotlinBuilderInDumbMode && !kotlinContext.rebuildingAllKotlin && kotlinChunk.isEnabled) {
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
        kotlinContext: KotlinCompileContext,
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
            InlineConstTracker.DoNothing,
            EnumWhenTracker.DoNothing,
            ImportTracker.DoNothing,
            chunk,
            messageCollector
        ) ?: return

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
        outputConsumer: OutputConsumer,
    ): ExitCode {
        val reportService = JpsStatisticsReportService.getFromContext(context)
        reportService.moduleBuildStarted(chunk)
        return doBuild(context, chunk, dirtyFilesHolder, outputConsumer).also { result ->
            reportService.moduleBuildFinished(chunk, context, result)
        }
    }

    private fun doBuild(
        context: CompileContext,
        chunk: ModuleChunk,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        outputConsumer: OutputConsumer,
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
            val reportService = JpsStatisticsReportService.getFromContext(context)
            reportService.reportDirtyFiles(kotlinDirtyFilesHolder)
            return reportService.reportMetrics(chunk, JpsBuildTime.JPS_ITERATION) {
                val proposedExitCode =
                    doBuild(chunk, kotlinTarget, context, kotlinDirtyFilesHolder, messageCollector, outputConsumer, fsOperations)

                val actualExitCode =
                    if (proposedExitCode == OK && fsOperations.hasMarkedDirty) ADDITIONAL_PASS_REQUIRED else proposedExitCode

                LOG.debug("Build result: $actualExitCode")

                context.testingContext?.buildLogger?.buildFinished(actualExitCode)
                actualExitCode
            }
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
        fsOperations: FSOperationsHelper,
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
                KotlinJpsBundle.message(
                    "error.text.cyclically.dependent.modules.0.should.have.same.compiler",
                    kotlinChunk.presentableModulesToCompilersList
                )
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
            messageCollector.report(
                ERROR,
                KotlinJpsBundle.message("error.text.output.directory.not.specified.for.0", targetsWithoutOutputDir.joinToString())
            )
            return ABORT
        }

        val project = projectDescriptor.project
        val lookupTracker = getLookupTracker(project, representativeTarget)
        val exceptActualTracker = ExpectActualTrackerImpl()
        val incrementalCaches = kotlinChunk.loadCaches()
        val inlineConstTracker = InlineConstTrackerImpl()
        val enumWhenTracker = EnumWhenTrackerImpl()
        val importTracker = ImportTrackerImpl()

        val environment = createCompileEnvironment(
            context,
            representativeTarget,
            incrementalCaches,
            lookupTracker,
            exceptActualTracker,
            inlineConstTracker,
            enumWhenTracker,
            importTracker,
            chunk,
            messageCollector
        ) ?: return ABORT

        context.testingContext?.buildLogger?.compilingFiles(
            kotlinDirtyFilesHolder.allDirtyFiles,
            kotlinDirtyFilesHolder.allRemovedFilesFiles
        )

        cleanJsOutputs(context, kotlinChunk, incrementalCaches, kotlinDirtyFilesHolder)


        val reportService = JpsStatisticsReportService.getFromContext(context)
        reportService.reportCompilerArguments(chunk, kotlinChunk)
        val start = System.nanoTime()


        // TODO: 1) get rid of aether and maybe publish fat-jar? or use resolving logic on IJ side?
        // TODO: 2) is js compilation supported in bta?
        // TODO: 3) Should we get output reporter from BTA?
        if (System.getProperty("kotlin.jps.build.bta") == "true") {
            val groupId = "org.jetbrains.kotlin"
            val version = "2.1.255-SNAPSHOT"
            val artifactId = "kotlin-build-tools-impl"
            val m2Repo = System.getProperty("user.home") + "/.m2/repository"
            val groupPath = groupId.replace(".", "/")
            val jarFile = File("$m2Repo/$groupPath/$artifactId/$version/$artifactId-$version.jar")
            if (!jarFile.exists()) throw IllegalStateException("kotlin-build-tools-impl.jar is not found in $m2Repo. File: ${jarFile.absolutePath}")

            val urls = AetherResolver().resolveArtifact("$groupId:$artifactId:$version")
//kotlinbuildtoolsapiclasspath - task to resolve transitive dependencies
//            withCompilerClassloader(environment) { classloader ->
////            val classpath = resolve("kotlin-build-tools-impl"), kotlinVersion) // => list of jar files
////                val classpath = arrayOf(jarFile.toURI().toURL())
////                val parentClassloader = SharedApiClassesClassLoader() // load BTA interfaces to JPS process
////                val classloader = URLClassLoader(classpath, parentClassloader)
//
//
//                val className = "org.jetbrains.kotlin.buildtools.internal.CompilationServiceImpl" // Известное имя класса в JAR
//                val isJarLoaded = try {
//                    classloader.loadClass(className)
//                    true
//                } catch (e: ClassNotFoundException) {
//                    false
//                }
//                fun addJarToClassLoader(jarFile: File, classloader: ClassLoader): ClassLoader {
//                    require(jarFile.exists() && jarFile.extension == "jar") { "Invalid JAR file: ${jarFile.absolutePath}" }
//                    val jarUrl = jarFile.toURI().toURL()
//
//                    return if (classloader is URLClassLoader) {
//                        val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
//                        method.isAccessible = true
//                        method.invoke(classloader, jarUrl)
//                        classloader
//                    } else {
//                        // Create a new URLClassLoader
//                        URLClassLoader(arrayOf(jarUrl), classloader)
//                    }
//                }
//
//                val compilationService: CompilationService = if (!isJarLoaded) {
//                    val updatedClassLoader = addJarToClassLoader(jarFile, classloader)
//                    // Проверка после добавления
//                    try {
//                        updatedClassLoader.loadClass(className)
//                        println("JAR successfully added to ClassLoader.")
//                    } catch (e: ClassNotFoundException) {
//                        println("Failed to load class from added JAR.")
//                    }
//                    loadImplementation(updatedClassLoader)
//                } else {
//                    loadImplementation(classloader)
//                }


//            val tracker = object : BtaLookupTracker {
//                override fun report(lookups: List<String>) {
//                    println(lookups) // check classloader clash
//                }
//            }

//        compilationService.compileDumbJvm(ProjectId.ProjectUUID(""), null, null, null, null, tracker)
            val parentClassloader = SharedApiClassesClassLoader() // load BTA interfaces to JPS process
            val classloader = URLClassLoader(urls, parentClassloader)
            val compilationService: CompilationService = loadImplementation(classloader)

            // Prepare args
            val arguments = collectArguments(kotlinChunk, representativeTarget, kotlinDirtyFilesHolder)
            // do non-inc compile
            compilationService.compileJvm(
                projectId = ProjectId.ProjectUUID(uuid = UUID.randomUUID()),
                strategyConfig = compilationService.makeCompilerExecutionStrategyConfiguration().apply {
//                    useInProcessStrategy()
                    useDaemonStrategy(listOf("-Xmx2G"))
                },
                compilationConfig = compilationService.makeJvmCompilationConfiguration().apply {
//                     useIncrementalCompilation() // now - disabled IC
                },
                sources = kotlinDirtyFilesHolder.allDirtyFiles.toList(),
                arguments = arguments
            )

            return OK
        }

        val outputItemCollector = doCompileModuleChunk(
            kotlinChunk,
            representativeTarget,
            kotlinChunk.compilerArguments,
            context,
            kotlinDirtyFilesHolder,
            fsOperations,
            environment,
            incrementalCaches,
            reportService.getMetricReporter(chunk)
        )

        statisticsLogger.registerStatistic(chunk, System.nanoTime() - start)

        if (outputItemCollector == null) {
            return NOTHING_DONE
        }

        val compilationErrors = Utils.ERRORS_DETECTED_KEY[context, false]
        if (compilationErrors) {
            LOG.info("Compiled with errors")
            JavaBuilderUtil.registerFilesWithErrors(context, messageCollector.filesWithErrors.map(::File))
            return ABORT
        } else {
            JavaBuilderUtil.registerSuccessfullyCompiled(context, kotlinDirtyFilesHolder.allDirtyFiles)
            LOG.info("Compiled successfully")
        }

        val generatedFiles = getGeneratedFiles(context, chunk, environment.outputItemsCollector)

        if (!isKotlinBuilderInDumbMode) markDirtyComplementaryMultifileClasses(
            generatedFiles,
            kotlinContext,
            incrementalCaches,
            fsOperations
        )

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
            incrementalCaches,
            environment
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

            if (!isKotlinBuilderInDumbMode || enableLookupStorageFillingInDumbMode) {
                updateLookupStorage(lookupTracker, kotlinContext.lookupStorageManager, kotlinDirtyFilesHolder)
            }

            if (!isKotlinBuilderInDumbMode && !isChunkRebuilding) {
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

    private fun collectArguments(kotlinChunk: KotlinChunk, representativeTarget: KotlinModuleBuildTarget<*>, dirtyFilesHolder: KotlinDirtySourceFilesHolder): List<String> {
        val module = representativeTarget.module
        val commonArguments = kotlinChunk.compilerArguments
        val k2jvmArguments = module.k2JvmCompilerArguments
        val compilerSettings = module.kotlinCompilerSettings
        val moduleFile = (representativeTarget as? KotlinJvmModuleBuildTarget)!!.generateChunkModuleDescription(dirtyFilesHolder) ?: return emptyList() // TODO: do nothing
        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2jvmArguments))
        setupK2JvmArguments(moduleFile, arguments)

        val allArgs = ArgumentUtils.convertArgumentsToStringList(arguments) +
                (compilerSettings.additionalArgumentsAsList)
        return filterDuplicatedCompilerPluginOptions(allArgs)

//        withCompilerSettings(compilerSettings) {
//            runCompiler(KotlinCompilerClass.JVM, arguments, environment, buildMetricReporter)
//        }
//        return emptyList()
    }
    private fun cleanJsOutputs(
        context: CompileContext,
        kotlinChunk: KotlinChunk,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        kotlinDirtyFilesHolder: KotlinDirtySourceFilesHolder,
    ) {
        for (target in kotlinChunk.targets) {
            val cache = incrementalCaches[target] ?: continue

            if (cache is IncrementalJsCache) {
                val filesToDelete = mutableListOf<File>()
                val dirtyFiles = kotlinDirtyFilesHolder.getDirtyFiles(target.jpsModuleBuildTarget).keys
                val removedFiles = kotlinDirtyFilesHolder.getRemovedFiles(target.jpsModuleBuildTarget)

                for (file: File in dirtyFiles + removedFiles) {
                    filesToDelete.addAll(cache.getOutputsBySource(file).filter { it !in filesToDelete })
                }

                if (filesToDelete.isNotEmpty()) {
                    val deletedForThisSource = mutableSetOf<String>()
                    val parentDirs = mutableSetOf<File>()

                    for (kjsmFile in filesToDelete) {
                        BuildOperations.deleteRecursively(kjsmFile.path, deletedForThisSource, parentDirs)
                    }

                    FSOperations.pruneEmptyDirs(context, parentDirs)

                    val logger = context.loggingManager.projectBuilderLogger
                    if (logger.isEnabled && deletedForThisSource.isNotEmpty()) {
                        logger.logDeletedFiles(deletedForThisSource)
                    }
                }
            }
        }
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
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        buildMetricReporter: JpsBuilderMetricReporter?,
    ): OutputItemsCollector? {
        kotlinChunk.targets.forEach {
            it.nextRound(context)
        }

        if (representativeTarget.isIncrementalCompilationEnabled) {
            buildMetricReporter?.addTag(StatTag.INCREMENTAL)
            for (target in kotlinChunk.targets) {
                val cache = incrementalCaches[target]
                val jpsTarget = target.jpsModuleBuildTarget

                val targetDirtyFiles = dirtyFilesHolder.byTarget[jpsTarget]
                if (cache != null && targetDirtyFiles != null) {
                    val complementaryFiles = cache.getComplementaryFilesRecursive(targetDirtyFiles.dirty.keys + targetDirtyFiles.removed)
                    context.testingContext?.buildLogger?.markedAsComplementaryFiles(ArrayList(complementaryFiles))
                    fsOperations.markFilesForCurrentRound(jpsTarget, complementaryFiles)

                    cache.markDirty(targetDirtyFiles.dirty.keys + targetDirtyFiles.removed)
                }
            }
        }

        registerFilesToCompile(dirtyFilesHolder, context)
        val isDoneSomething = representativeTarget.compileModuleChunk(commonArguments, dirtyFilesHolder, environment, buildMetricReporter)

        return if (isDoneSomething) environment.outputItemsCollector else null
    }

    private fun registerFilesToCompile(
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
        context: CompileContext,
    ) {
        val allDirtyFiles = dirtyFilesHolder.allDirtyFiles
        if (LOG.isDebugEnabled) {
            LOG.debug("Compiling files: $allDirtyFiles")
        }
        JavaBuilderUtil.registerFilesToCompile(context, allDirtyFiles)
    }

    private fun createCompileEnvironment(
        context: CompileContext,
        kotlinModuleBuilderTarget: KotlinModuleBuildTarget<*>,
        incrementalCaches: Map<KotlinModuleBuildTarget<*>, JpsIncrementalCache>,
        lookupTracker: LookupTracker,
        exceptActualTracer: ExpectActualTracker,
        inlineConstTracker: InlineConstTracker,
        enumWhenTracker: EnumWhenTracker,
        importTracker: ImportTracker,
        chunk: ModuleChunk,
        messageCollector: MessageCollectorAdapter,
    ): JpsCompilerEnvironment? {
        val compilerServices = with(Services.Builder()) {
            kotlinModuleBuilderTarget.makeServices(
                this,
                incrementalCaches,
                lookupTracker,
                exceptActualTracer,
                inlineConstTracker,
                enumWhenTracker,
                importTracker
            )
            build()
        }

        return JpsCompilerEnvironment(
            computeKotlinPathsForJpsPlugin(messageCollector) ?: return null,
            compilerServices,
            classesToLoadByParent,
            messageCollector,
            OutputItemsCollectorImpl(),
            ProgressReporterImpl(context, chunk)
        )
    }

    // When JPS is run on TeamCity, it can not rely on Kotlin plugin layout,
    // so the path to Kotlin is specified in a system property
    private fun computeKotlinPathsForJpsPlugin(messageCollector: MessageCollectorAdapter): KotlinPaths? {
        val jpsKotlinHome = System.getProperty(JPS_KOTLIN_HOME_PROPERTY)?.let { File(it) }
        if (System.getProperty("kotlin.jps.tests").equals("true", ignoreCase = true) && jpsKotlinHome == null) {
            return PathUtil.kotlinPathsForDistDirectory
        }

        return when {
            jpsKotlinHome == null -> {
                messageCollector.report(ERROR, "Make sure that '$JPS_KOTLIN_HOME_PROPERTY' system property is set in JPS process")
                null
            }
            jpsKotlinHome.exists() -> KotlinPathsFromHomeDir(jpsKotlinHome)
            else -> {
                messageCollector.report(ERROR, "Cannot find kotlinc home at $jpsKotlinHome")
                null
            }
        }
    }

    private fun getGeneratedFiles(
        context: CompileContext,
        chunk: ModuleChunk,
        outputItemCollector: OutputItemsCollectorImpl,
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
            .groupBy(SimpleOutputItem::target) { it.toGeneratedFile(MetadataVersion.INSTANCE) }
    }

    private fun updateLookupStorage(
        lookupTracker: LookupTracker,
        lookupStorageManager: JpsLookupStorageManager,
        dirtyFilesHolder: KotlinDirtySourceFilesHolder,
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
        fsOperations: FSOperationsHelper,
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

    override fun report(message: () -> String, severity: ReportSeverity) {
        // Currently, all severity levels are mapped to debug
        if (KotlinBuilder.LOG.isDebugEnabled) {
            KotlinBuilder.LOG.debug(message())
        }
    }
}

private fun ChangesCollector.processChangesUsingLookups(
    compiledFiles: Set<File>,
    lookupStorageManager: JpsLookupStorageManager,
    fsOperations: FSOperationsHelper,
    caches: Iterable<JpsIncrementalCache>,
) {
    val allCaches = caches.flatMap { it.thisWithDependentCaches }
    val reporter = JpsICReporter()

    reporter.debug { "Start processing changes" }

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

    reporter.debug { "End of processing changes" }
}

data class FilesToRecompile(val dirtyFiles: Set<File>, val forceRecompileTogether: Set<File>)

private fun ChangesCollector.getDirtyFiles(
    caches: Iterable<IncrementalCacheCommon>,
    lookupStorageManager: JpsLookupStorageManager,
): FilesToRecompile {
    val reporter = JpsICReporter()
    val (dirtyLookupSymbols, dirtyClassFqNames, forceRecompile) = getChangedAndImpactedSymbols(caches, reporter)
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