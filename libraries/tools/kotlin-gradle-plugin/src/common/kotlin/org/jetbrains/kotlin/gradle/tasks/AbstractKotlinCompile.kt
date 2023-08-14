/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.CompilerExecutionSettings
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.UsesCompilerSystemPropertiesService
import org.jetbrains.kotlin.compilerRunner.createGradleCompilerRunner
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.incremental.UsesIncrementalModuleInfoBuildService
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_SUPPRESS_EXPERIMENTAL_IC_OPTIMIZATIONS_WARNING
import org.jetbrains.kotlin.gradle.plugin.UsesBuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.UsesVariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.internal.UsesBuildIdProviderService
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.tasks.cleanOutputsAndLocalState as cleanOutputsAndLocalStateUtil

private const val ABI_SNAPSHOT_FILE_NAME = "abi-snapshot.bin"

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractKotlinCompile<T : CommonCompilerArguments> @Inject constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor,
) : AbstractKotlinCompileTool<T>(objectFactory),
    CompileUsingKotlinDaemonWithNormalization,
    UsesBuildMetricsService,
    UsesIncrementalModuleInfoBuildService,
    UsesCompilerSystemPropertiesService,
    UsesVariantImplementationFactories,
    UsesBuildFinishedListenerService,
    UsesClassLoadersCachingBuildService,
    UsesKotlinToolingDiagnostics,
    UsesBuildIdProviderService,
    BaseKotlinCompile {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    @get:Inject
    internal abstract val projectLayout: ProjectLayout

    @get:Inject
    internal abstract val fileSystemOperations: FileSystemOperations

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:OutputDirectory
    internal open val taskBuildCacheableOutputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:LocalState
    internal abstract val taskBuildLocalStateDirectory: DirectoryProperty

    @get:Nested
    abstract val compilerOptions: KotlinCommonCompilerOptions

    @get:Internal
    internal val buildHistoryFile
        get() = taskBuildLocalStateDirectory.file("build-history.bin")

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    // todo: deprecate and remove (we may need to design api for configuring IC)
    // don't rely on it to check if IC is enabled, use isIncrementalCompilationEnabled instead
    @get:Internal
    var incremental: Boolean = false
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
        }

    @Input
    internal open fun isIncrementalCompilationEnabled(): Boolean =
        incremental

    @Deprecated("Scheduled for removal with Kotlin 2.0", ReplaceWith("moduleName"))
    @get:Input
    abstract val ownModuleName: Property<String>

    @get:Internal
    val startParameters = BuildReportsService.getStartParameters(project)

    @get:Input
    @get:Optional
    abstract val explicitApiMode: Property<ExplicitApiMode>

    @get:Internal
    internal abstract val suppressKotlinOptionsFreeArgsModificationWarning: Property<Boolean>

    internal fun reportingSettings() = buildMetricsService.orNull?.parameters?.reportingSettings?.orNull ?: ReportingSettings()

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(buildHistoryFile.get().asFile, useModuleDetection.get())

    /**
     * Plugin Data provided by [KpmCompilerPlugin]
     */
    @get:Optional
    @get:Nested
    // TODO: replace with objects.property and introduce task configurator
    internal var kotlinPluginData: Provider<KotlinCompilerPluginData>? = null

    @get:Internal
    internal val javaOutputDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val commonSourceSet: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    val abiSnapshotFile
        get() = taskBuildCacheableOutputDirectory.file(ABI_SNAPSHOT_FILE_NAME)

    @get:Input
    val abiSnapshotRelativePath: Property<String> = objectFactory.property(String::class.java).value(
        //TODO update to support any jar changes
        "$name/${ABI_SNAPSHOT_FILE_NAME}"
    )

    @get:Internal
    internal val friendSourceSets = objectFactory.listProperty(String::class.java)

    private val kotlinLogger by lazy { GradleKotlinLogger(logger) }

    @get:Internal
    protected val gradleCompileTaskProvider: Provider<GradleCompileTaskProvider> = objectFactory
        .property(
            objectFactory.newInstance<GradleCompileTaskProvider>(project.gradle, this, project, incrementalModuleInfoProvider)
        )

    @get:Internal
    internal open val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> = objectFactory
        .propertyWithNewInstance({ null })

    @get:Internal
    internal val compilerRunner: Provider<GradleCompilerRunner> =
        objectFactory.propertyWithConvention(
            gradleCompileTaskProvider.flatMap { taskProvider ->
                compilerExecutionStrategy
                    .zip(metrics) { executionStrategy, metrics ->
                        metrics to executionStrategy
                    }
                    .flatMap { params ->
                        defaultKotlinJavaToolchain
                            .map {
                                val toolsJar = it.currentJvmJdkToolsJar.orNull
                                createGradleCompilerRunner(
                                    taskProvider,
                                    toolsJar,
                                    CompilerExecutionSettings(
                                        normalizedKotlinDaemonJvmArguments.orNull,
                                        params.second,
                                        useDaemonFallbackStrategy.get()
                                    ),
                                    params.first,
                                    workerExecutor,
                                    runViaBuildToolsApi.get(),
                                    classLoadersCachingService,
                                    buildFinishedListenerService,
                                    buildIdService,
                                )
                            }
                    }
            }
        )

    @get:Internal
    internal abstract val preciseCompilationResultsBackup: Property<Boolean>

    @get:Internal
    internal abstract val keepIncrementalCompilationCachesInMemory: Property<Boolean>

    @get:Internal
    internal abstract val suppressExperimentalIcOptimizationsWarning: Property<Boolean>

    /** Task outputs that we don't want to include in [TaskOutputsBackup] (see [TaskOutputsBackup.outputsToRestore] for more info). */
    @get:Internal
    internal abstract val taskOutputsBackupExcludes: SetProperty<File>

    private fun notifyUserAboutExperimentalICOptimizations() {
        if (suppressExperimentalIcOptimizationsWarning.get()) {
            return
        }
        if (!preciseCompilationResultsBackup.get() && !keepIncrementalCompilationCachesInMemory.get()) {
            return
        }
        val key = "experimental-ic-optimizations"
        buildFinishedListenerService.get().onCloseOnceByKey(key) {
            Logging.getLogger(key).warn(
                """
                
                The build has experimental Kotlin incremental compilation optimizations enabled.
                If you notice incorrect compilation results after enabling it, please file a bug report at https://kotl.in/issue/experimental-ic-optimizations
                
                You can suppress this warning by adding `${KOTLIN_SUPPRESS_EXPERIMENTAL_IC_OPTIMIZATIONS_WARNING}=true` to the gradle.properties
                """.trimIndent()
            )
        }
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        notifyUserAboutExperimentalICOptimizations()
        val buildMetrics = metrics.get()
        buildMetrics.addTimeMetric(GradleBuildPerformanceMetric.START_TASK_ACTION_EXECUTION)
        buildMetrics.measure(GradleBuildTime.OUT_OF_WORKER_TASK_ACTION) {
            KotlinBuildStatsService.applyIfInitialised {
                if (name.contains("Test"))
                    it.report(BooleanMetrics.TESTS_EXECUTED, true)
                else
                    it.report(BooleanMetrics.COMPILATION_STARTED, true)
            }
            validateCompilerClasspath()
            collectCommonCompilerStats()
            systemPropertiesService.get().startIntercept()
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"

            // If task throws exception, but its outputs are changed during execution,
            // then Gradle forces next build to be non-incremental (see Gradle's DefaultTaskArtifactStateRepository#persistNewOutputs)
            // To prevent this, we backup outputs before incremental build and restore when exception is thrown
            val outputsBackup: TaskOutputsBackup? =
                if (isIncrementalCompilationEnabled() && inputChanges.isIncremental)
                    buildMetrics.measure(GradleBuildTime.BACKUP_OUTPUT) {
                        TaskOutputsBackup(
                            fileSystemOperations,
                            projectLayout.buildDirectory,
                            projectLayout.buildDirectory.dir("snapshot/kotlin/$name"),
                            outputsToRestore = allOutputFiles() - taskOutputsBackupExcludes.get(),
                            logger
                        ).also {
                            it.createSnapshot()
                        }
                    }
                else null

            if (!isIncrementalCompilationEnabled()) {
                cleanOutputsAndLocalState("IC is disabled")
            } else if (!inputChanges.isIncremental) {
                cleanOutputsAndLocalState("Task cannot run incrementally")
            }

            executeImpl(inputChanges, outputsBackup)
        }

        buildMetricsService.orNull?.also { it.addTask(path, this.javaClass, buildMetrics) }
    }

    private fun collectCommonCompilerStats() {
        KotlinBuildStatsService.getInstance()?.apply {
            report(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, compilerOptions.progressiveMode.get())
            compilerOptions.apiVersion.orNull?.also { v ->
                report(StringMetrics.KOTLIN_API_VERSION, v.version)
            }
            compilerOptions.languageVersion.orNull?.also { v ->
                report(StringMetrics.KOTLIN_LANGUAGE_VERSION, v.version)
            }
        }
    }

    protected open fun cleanOutputsAndLocalState(reason: String?) {
        cleanOutputsAndLocalStateUtil(reason)
    }

    protected open fun skipCondition(): Boolean = sources.isEmpty

    @get:Internal
    protected open val incrementalProps: List<FileCollection>
        get() = listOfNotNull(
            sources,
            libraries,
            commonSourceSet
        )

    private fun executeImpl(
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val allKotlinSources = sources.asFileTree.files

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(gradleCompileTaskProvider.get().projectDir.get())}" }

        if (!inputChanges.isIncremental && skipCondition()) {
            // Skip running only if non-incremental run. Otherwise, we may need to do some cleanup.
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        val args = createCompilerArguments()

        taskBuildCacheableOutputDirectory.get().asFile.mkdirs()
        taskBuildLocalStateDirectory.get().asFile.mkdirs()
        callCompilerAsync(
            args,
            inputChanges,
            taskOutputsBackup
        )
    }

    protected fun getChangedFiles(
        inputChanges: InputChanges,
        incrementalProps: List<FileCollection>
    ) = if (!inputChanges.isIncremental) {
        SourcesChanges.Unknown
    } else {
        incrementalProps
            .fold(mutableListOf<File>() to mutableListOf<File>()) { (modified, removed), prop ->
                inputChanges.getFileChanges(prop).forEach {
                    when (it.changeType) {
                        ChangeType.ADDED, ChangeType.MODIFIED -> modified.add(it.file)
                        ChangeType.REMOVED -> removed.add(it.file)
                        else -> Unit
                    }
                }
                modified to removed
            }
            .run {
                SourcesChanges.Known(first, second)
            }
    }

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    internal abstract fun callCompilerAsync(
        args: T,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    )
}
