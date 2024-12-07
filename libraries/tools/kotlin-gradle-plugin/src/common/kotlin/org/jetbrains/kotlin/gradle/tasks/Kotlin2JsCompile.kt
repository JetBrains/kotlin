/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ContributeCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.create
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.internal.UsesLibraryFilterCachingService
import org.jetbrains.kotlin.gradle.tasks.internal.KotlinJsOptionsCompat
import org.jetbrains.kotlin.gradle.utils.chainedDisallowChanges
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.toPathsArray
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class Kotlin2JsCompile @Inject constructor(
    final override val compilerOptions: KotlinJsCompilerOptions,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor,
) : AbstractKotlinCompile<K2JSCompilerArguments>(objectFactory, workerExecutor),
    UsesLibraryFilterCachingService,
    KotlinJsCompile,
    K2MultiplatformCompilationTask,
    ProducesKlib {

    init {
        incremental = true
        compilerOptions.verbose.convention(logger.isDebugEnabled)
    }

    @Suppress("DEPRECATION")
    @Deprecated(KOTLIN_OPTIONS_DEPRECATION_MESSAGE)
    override val kotlinOptions: KotlinJsOptions = KotlinJsOptionsCompat(
        { this },
        compilerOptions
    )

    @get:Input
    internal var incrementalJsKlib: Boolean = true

    override fun isIncrementalCompilationEnabled(): Boolean {
        return incrementalJsKlib || incremental
    }

    // Workaround to be able to use default value and change it later based on external input
    @get:Internal
    internal abstract val defaultDestinationDirectory: DirectoryProperty

    // To be sure that outputFileProperty will be correct on transition period
    @get:Internal
    internal abstract val _outputFileProperty: Property<File>

    // hidden to keep ABI compatiblity, because we have plugins which still use outputFileProperty
    @Deprecated("Use destinationDirectory and moduleName instead", level = DeprecationLevel.HIDDEN)
    @get:Internal
    val outputFileProperty: Property<File>
        get() = _outputFileProperty

    override val produceUnpackagedKlib: Property<Boolean> = objectFactory.property(Boolean::class.java).value(true).chainedDisallowChanges()

    override val klibOutput: Provider<File>
        get() = destinationDirectory.asFile

    // Workaround to add additional compiler args based on the exising one
    // Currently there is a logic to add additional compiler arguments based on already existing one.
    // And it is not possible to update compilerOptions.freeCompilerArgs using some kind of .map
    // or .flatMap call - this will cause StackOverlowException as upstream source will be updated
    // and .map will be called again.
    @get:Input
    internal abstract val enhancedFreeCompilerArgs: ListProperty<String>

    /**
     * Workaround for those "nasty" plugins that are adding 'freeCompilerArgs' on task execution phase.
     * With properties api it is not possible to update property value after task configuration is finished.
     *
     * Marking it as `@Internal` as anyway on the configuration phase, when Gradle does task inputs snapshot,
     * this input will always be empty.
     */
    @get:Internal
    internal var executionTimeFreeCompilerArgs: List<String>? = null

    @get:Deprecated(
        message = "Task.moduleName is not used in Kotlin/JS"
    )
    @get:Optional
    @get:Input
    abstract override val moduleName: Property<String>

    @get:Internal
    internal abstract val mainCompilationModuleName: Property<String>

    @get:Internal
    internal abstract val projectVersion: Property<String>

    @get:Nested
    override val multiplatformStructure: K2MultiplatformStructure = objectFactory.newInstance()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("KTIJ-25227: Necessary override for IDEs < 2023.2", level = DeprecationLevel.ERROR)
    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        @Suppress("DEPRECATION_ERROR")
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
    }

    /**
     * In some cases, test compilations may have both main compilation outputs as directory and klib.
     * This produces compiler warning about having two similar Klibs as inputs.
     * We want to avoid such warnings by excluding packed .klib artifact from the main compilation.
     */
    private fun FileCollection.filterMainCompilationKlibArtifact(): FileCollection = run {
        val klibPrefix = mainCompilationModuleName.orNull
        val version = projectVersion.get()
        if (klibPrefix != null) {
            // "unspecified" is default version value when user hasn't explicitly configured the project version
            val mainCompilationKlibName = if (version != "unspecified") "$klibPrefix-js-$version.klib" else "$klibPrefix-js.klib"
            filter { it.name != mainCompilationKlibName }
        } else this
    }

    override fun createCompilerArguments(context: CreateCompilerArgumentsContext) = context.create<K2JSCompilerArguments> {
        primitive { args ->
            args.multiPlatform = multiPlatformEnabled.get()

            args.pluginOptions = (pluginOptions.toSingleCompilerPluginOptions() + kotlinPluginData?.orNull?.options)
                .arguments.toTypedArray()

            if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
                args.reportPerf = true
            }

            KotlinJsCompilerOptionsHelper.fillCompilerArguments(compilerOptions, args)

            args.outputDir = destinationDirectory.get().asFile.normalize().absolutePath
            args.moduleName = compilerOptions.moduleName.get()

            if (compilerOptions.usesK2.get() && multiPlatformEnabled.get()) {
                args.fragments = multiplatformStructure.fragmentsCompilerArgs
                args.fragmentRefines = multiplatformStructure.fragmentRefinesCompilerArgs
            }

            explicitApiMode.orNull?.run { args.explicitApi = toCompilerValue() }

            // Overriding freeArgs from compilerOptions with enhanced one + additional one set on execution phase
            // containing additional arguments based on the js compilation configuration
            args.freeArgs = executionTimeFreeCompilerArgs ?: enhancedFreeCompilerArgs.get().toList()
        }

        pluginClasspath { args ->
            args.pluginClasspaths = runSafe {
                listOfNotNull(
                    pluginClasspath, kotlinPluginData?.orNull?.classpath
                ).reduce(FileCollection::plus).toPathsArray()
            }
        }

        dependencyClasspath { args ->
            args.friendModules = friendDependencies.files.joinToString(File.pathSeparator) { it.absolutePath }

            args.libraries = runSafe {
                libraries
                    .filter { it.exists() && libraryFilter(it) }
                    .filterMainCompilationKlibArtifact()
                    .map { it.absolutePath }
                    .toSet()
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(File.pathSeparator)
            }
        }

        sources { args ->
            if (!args.sourceMapPrefix.isNullOrEmpty()) {
                args.sourceMapBaseDirs = sourceMapBaseDir.get().asFile.absolutePath
            }

            if (multiPlatformEnabled.get()) {
                if (compilerOptions.usesK2.get()) {
                    args.fragmentSources = multiplatformStructure.fragmentSourcesCompilerArgs(sources.files, sourceFileFilter)
                } else {
                    args.commonSources = commonSourceSet.asFileTree.toPathsArray()
                }
            }

            args.freeArgs += sources.asFileTree.files.map { it.absolutePath }
        }

        contributeAdditionalCompilerArguments(this)
    }

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:Optional
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val friendDependencies: FileCollection = objectFactory
        .fileCollection()
        .from(friendPaths)
        .filter { libraryFilter(it) }

    @get:Internal
    internal val sourceMapBaseDir: Property<Directory> = objectFactory
        .directoryProperty()
        .value(project.layout.projectDirectory)

    private val File.asLibraryFilterCacheKey: LibraryFilterCachingService.LibraryFilterCacheKey
        get() = LibraryFilterCachingService.LibraryFilterCacheKey(
            this
        )

    @get:Internal
    abstract override val libraries: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    internal val directoryLibraries by lazy {
        libraries.filter { it.isDirectory }
    }

    @get:Classpath
    @get:Incremental
    internal val packedLibraries by lazy {
        libraries.filter { !it.isDirectory }
    }

    @get:Internal
    protected val libraryFilter: (File) -> Boolean
        get() = { file ->
            libraryFilterCacheService.get().getOrCompute(file.asLibraryFilterCacheKey, ::isKotlinLibrary)
        }

    override val incrementalProps: List<FileCollection>
        /*
         * We are not interested in the entire list of changes in `directoryLibraries`.
         * It has a special treatment in the usage place and ModulesApiHistory.
         */
        get() = super.incrementalProps - listOf(libraries) + listOf(packedLibraries, friendDependencies)

    protected open fun processArgsBeforeCompile(args: K2JSCompilerArguments) = Unit

    protected open fun contributeAdditionalCompilerArguments(context: ContributeCompilerArgumentsContext<K2JSCompilerArguments>) {
        context.primitive { args ->
            args.irProduceKlibDir = true
        }
    }

    private operator fun SourcesChanges.plus(other: SourcesChanges): SourcesChanges {
        return when {
            this is SourcesChanges.Unknown || this is SourcesChanges.ToBeCalculated -> this
            other is SourcesChanges.Unknown || other is SourcesChanges.ToBeCalculated -> other
            this is SourcesChanges.Known && other is SourcesChanges.Known -> SourcesChanges.Known(
                modifiedFiles + other.modifiedFiles,
                removedFiles + other.removedFiles
            )
            else -> error("Impossible combination of sources changes during merging them")
        }
    }

    override fun callCompilerAsync(
        args: K2JSCompilerArguments,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?,
    ) {
        logger.debug("Calling compiler")

        val dependencies = libraries
            .filter { it.exists() && libraryFilter(it) }
            .filterMainCompilationKlibArtifact()
            .map { it.normalize().absolutePath }

        args.libraries = dependencies.distinct().let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependencies.files.joinToString(File.pathSeparator) { it.absolutePath }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val gradlePrintingMessageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val gradleMessageCollector =
            GradleErrorMessageCollector(logger, gradlePrintingMessageCollector, kotlinPluginVersion = getKotlinPluginVersion(logger))
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            val changedFiles = getChangedFiles(inputChanges, incrementalProps) + getChangedFiles(inputChanges, listOf(directoryLibraries)) {
                it.endsWith("default/${KLIB_MANIFEST_FILE_NAME}")
            }
            IncrementalCompilationEnvironment(
                changedFiles,
                ClasspathChanges.NotAvailableForJSCompiler,
                taskBuildCacheableOutputDirectory.get().asFile,
                rootProjectDir = rootProjectDir,
                buildDir = projectLayout.buildDirectory.getFile(),
                multiModuleICSettings = multiModuleICSettings,
                icFeatures = makeIncrementalCompilationFeatures(),
            )
        } else null

        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, gradleMessageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings(),
            incrementalCompilationEnvironment = icEnv,
            compilerArgumentsLogLevel = kotlinCompilerArgumentsLogLevel.get()
        )
        processArgsBeforeCompile(args)
        compilerRunner.runJsCompilerAsync(
            args,
            environment,
            taskOutputsBackup
        )
        compilerRunner.errorsFiles?.let { gradleMessageCollector.flush(it) }

    }

    private val rootProjectDir = project.rootDir
}
