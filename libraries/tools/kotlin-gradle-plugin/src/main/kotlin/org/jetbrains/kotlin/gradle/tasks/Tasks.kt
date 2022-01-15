/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.*
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner.Companion.normalizeForFlagFile
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.incremental.*
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.tasks.TaskConfigurator
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.internal.transforms.ClasspathEntrySnapshotTransform
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.BuildMetricsReporterService
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.ir.isProduceUnzippedKlib
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.*
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_JVM_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin/JVM incremental compilation"
const val USING_JS_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin/JS incremental compilation"
const val USING_JS_IR_BACKEND_MESSAGE = "Using Kotlin/JS IR backend"

abstract class AbstractKotlinCompileTool<T : CommonToolArguments>
    : AbstractCompile(),
    CompilerArgumentAwareWithInput<T>,
    TaskWithLocalState {

    @ReplacedBy("stableSources")
    override fun getSource() = super.getSource()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val stableSources: FileCollection = project.files(
        { source }
    )

    @Incremental
    override fun getClasspath(): FileCollection {
        return super.getClasspath()
    }

    @get:Internal
    override val metrics: Property<BuildMetricsReporter> = project.objects
        .property(BuildMetricsReporterImpl())

    /**
     * By default, should be set by plugin from [COMPILER_CLASSPATH_CONFIGURATION_NAME] configuration.
     *
     * Empty classpath will fail the build.
     */
    @get:Classpath
    internal val defaultCompilerClasspath: ConfigurableFileCollection =
        project.objects.fileCollection()

    protected fun validateCompilerClasspath() {
        // Note that the check triggers configuration resolution
        require(!defaultCompilerClasspath.isEmpty) {
            "Default Kotlin compiler classpath is empty! Task: $path (${this::class.qualifiedName})"
        }
    }
}

abstract class GradleCompileTaskProvider @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    gradle: Gradle,
    task: Task,
    project: Project
) {

    @get:Internal
    val path: Provider<String> = objectFactory.property(task.path)

    @get:Internal
    val logger: Provider<Logger> = objectFactory.property(task.logger)

    @get:Internal
    val buildDir: DirectoryProperty = projectLayout.buildDirectory

    @get:Internal
    val projectDir: Provider<File> = objectFactory
        .property(project.rootProject.projectDir)

    @get:Internal
    val rootDir: Provider<File> = objectFactory
        .property(project.rootProject.rootDir)

    @get:Internal
    val sessionsDir: Provider<File> = objectFactory
        .property(GradleCompilerRunner.sessionsDir(project.rootProject.buildDir))

    @get:Internal
    val projectName: Provider<String> = objectFactory
        .property(project.rootProject.name.normalizeForFlagFile())

    @get:Internal
    val buildModulesInfo: Provider<out IncrementalModuleInfoProvider> = objectFactory.property(
        /**
         * See https://youtrack.jetbrains.com/issue/KT-46820. Build service that holds the incremental info may
         * be instantiated during execution phase and there could be multiple threads trying to do that. Because the
         * underlying mechanism does not support multi-threaded access, we need to add external synchronization.
         */
        synchronized(gradle.sharedServices) {
            gradle.sharedServices.registerIfAbsent(
                IncrementalModuleInfoBuildService.getServiceName(), IncrementalModuleInfoBuildService::class.java
            ) {
                it.parameters.info.set(
                    objectFactory.providerWithLazyConvention {
                        GradleCompilerRunner.buildModulesInfo(gradle)
                    }
                )
            }
        }
    )
}

abstract class AbstractKotlinCompile<T : CommonCompilerArguments> : AbstractKotlinCompileTool<T>(),
    CompileUsingKotlinDaemonWithNormalization {

    open class Configurator<T : AbstractKotlinCompile<*>>(protected val compilation: KotlinCompilationData<*>) : TaskConfigurator<T> {
        override fun configure(task: T) {
            val project = task.project
            task.friendPaths.from(project.provider { compilation.friendPaths })

            if (compilation is KotlinCompilation<*>) {
                task.friendSourceSets.set(project.provider { compilation.associateWithTransitiveClosure.map { it.name } })
                // FIXME support compiler plugins with PM20
                task.pluginClasspath.from(project.configurations.getByName(compilation.pluginConfigurationName))
            }
            task.moduleName.set(project.provider { compilation.moduleName })
            task.sourceSetName.set(project.provider { compilation.compilationPurpose })
            task.coroutines.value(
                project.provider {
                    project.extensions.findByType(KotlinTopLevelExtension::class.java)!!.experimental.coroutines
                        ?: PropertiesProvider(project).coroutines
                        ?: Coroutines.DEFAULT
                }
            ).disallowChanges()
            task.multiPlatformEnabled.value(
                project.provider {
                    project.plugins.any { it is KotlinPlatformPluginBase || it is KotlinMultiplatformPluginWrapper || it is KotlinPm20PluginWrapper }
                }
            ).disallowChanges()
            task.taskBuildDirectory.value(getKotlinBuildDir(task)).disallowChanges()
            task.localStateDirectories.from(task.taskBuildDirectory).disallowChanges()

            PropertiesProvider(task.project).mapKotlinDaemonProperties(task)
        }

        private fun getKotlinBuildDir(task: T): Provider<Directory> =
            task.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/${task.name}")

        protected open fun getClasspathSnapshotDir(task: T): Provider<Directory> =
            task.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/classpath-snapshot/${task.name}")
    }

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    private val layout = project.layout

    @get:Inject
    internal abstract val fileSystemOperations: FileSystemOperations

    @get:Internal
    protected val objects: ObjectFactory = project.objects

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:LocalState
    internal val taskBuildDirectory: DirectoryProperty = objects.directoryProperty()

    @get:Internal
    internal val buildHistoryFile
        get() = taskBuildDirectory.file("build-history.bin")

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

    @get:Internal
    val startParameters = BuildMetricsReporterService.getStartParameters(project)

    @get:Internal
    internal abstract val buildMetricsReporterService: Property<BuildMetricsReporterService?>

    internal fun reportingSettings() = buildMetricsReporterService.orNull?.parameters?.reportingSettings ?: ReportingSettings()

    @get:Input
    internal val useModuleDetection: Property<Boolean> = objects.property(Boolean::class.java).value(false)

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(buildHistoryFile.get().asFile, useModuleDetection.get())

    @get:Classpath
    open val pluginClasspath: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Input
    val sourceFilesExtensions: ListProperty<String> = objects.listProperty(String::class.java).value(DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS)

    /**
     * Plugin Data provided by [KpmCompilerPlugin]
     */
    @get:Optional
    @get:Nested
    // TODO: replace with objects.property and introduce task configurator
    internal var kotlinPluginData: Provider<KotlinCompilerPluginData>? = null

    // Input is needed to force rebuild even if source files are not changed
    @get:Input
    internal val coroutines: Property<Coroutines> = objects.property(Coroutines::class.java)

    @get:Internal
    internal val javaOutputDir: DirectoryProperty = objects.directoryProperty()

    @get:Internal
    internal val sourceSetName: Property<String> = objects.property(String::class.java)

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val commonSourceSet: ConfigurableFileCollection = objects.fileCollection()

    @get:Input
    internal val moduleName: Property<String> = objects.property(String::class.java)

    @get:Internal
    val abiSnapshotFile
        get() = taskBuildDirectory.file(IncrementalCompilerRunner.ABI_SNAPSHOT_FILE_NAME)

    @get:Input
    val abiSnapshotRelativePath: Property<String> = objects.property(String::class.java).value(
        //TODO update to support any jar changes
        "$name/${IncrementalCompilerRunner.ABI_SNAPSHOT_FILE_NAME}"
    )

    @get:Internal
    internal val friendSourceSets = objects.listProperty(String::class.java)

    @get:Internal // takes part in the compiler arguments
    val friendPaths: ConfigurableFileCollection = objects.fileCollection()

    private val kotlinLogger by lazy { GradleKotlinLogger(logger) }

    abstract override val kotlinDaemonJvmArguments: ListProperty<String>

    @get:Internal
    protected val gradleCompileTaskProvider: Provider<GradleCompileTaskProvider> = objects
        .property(
            objects.newInstance<GradleCompileTaskProvider>(project.gradle, this, project)
        )

    @get:Internal
    internal open val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunner(
                    it,
                    null,
                    normalizedKotlinDaemonJvmArguments.orNull,
                    metrics.get(),
                    compilerExecutionStrategy.get(),
                )
            }
        )

    private val systemPropertiesService = CompilerSystemPropertiesService.registerIfAbsent(project.gradle)

    /** Task outputs that we don't want to include in [TaskOutputsBackup] (see [TaskOutputsBackup]'s kdoc for more info). */
    @get:Internal
    protected open val taskOutputsBackupExcludes: List<File> = emptyList()

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val buildMetrics = metrics.get()
        buildMetrics.measure(BuildTime.GRADLE_TASK_ACTION) {
            KotlinBuildStatsService.applyIfInitialised {
                if (name.contains("Test"))
                    it.report(BooleanMetrics.TESTS_EXECUTED, true)
                else
                    it.report(BooleanMetrics.COMPILATION_STARTED, true)
            }
            validateCompilerClasspath()
            systemPropertiesService.get().startIntercept()
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"

            // If task throws exception, but its outputs are changed during execution,
            // then Gradle forces next build to be non-incremental (see Gradle's DefaultTaskArtifactStateRepository#persistNewOutputs)
            // To prevent this, we backup outputs before incremental build and restore when exception is thrown
            val outputsBackup: TaskOutputsBackup? =
                if (isIncrementalCompilationEnabled() && inputChanges.isIncremental)
                    buildMetrics.measure(BuildTime.BACKUP_OUTPUT) {
                        TaskOutputsBackup(
                            fileSystemOperations,
                            layout.buildDirectory,
                            layout.buildDirectory.dir("snapshot/kotlin/$name"),
                            allOutputFiles(),
                            taskOutputsBackupExcludes,
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

        buildMetricsReporterService.orNull?.also { it.add(path, this::class.java.name, buildMetrics) }
    }

    protected open fun skipCondition(): Boolean =
        getSourceRoots().kotlinSourceFiles.isEmpty()

    private val projectDir = project.rootProject.projectDir

    @get:Internal
    protected open val incrementalProps: List<FileCollection>
        get() = listOfNotNull(
            stableSources,
            classpath,
            commonSourceSet
        )

    private fun executeImpl(
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(projectDir)}" }

        if (!inputChanges.isIncremental && skipCondition()) {
            // Skip running only if non-incremental run. Otherwise, we may need to do some cleanup.
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = prepareCompilerArguments()
        taskBuildDirectory.get().asFile.mkdirs()
        callCompilerAsync(
            args,
            sourceRoots,
            inputChanges,
            taskOutputsBackup
        )
    }

    protected fun getChangedFiles(
        inputChanges: InputChanges,
        incrementalProps: List<FileCollection>
    ) = if (!inputChanges.isIncremental) {
        ChangedFiles.Unknown()
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
                ChangedFiles.Known(first, second)
            }
    }

    @Internal
    internal abstract fun getSourceRoots(): SourceRoots

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    internal abstract fun callCompilerAsync(
        args: T,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    )

    @get:Input
    internal val multiPlatformEnabled: Property<Boolean> = objects.property(Boolean::class.java)

    @get:Internal
    internal val abstractKotlinCompileArgumentsContributor by lazy {
        AbstractKotlinCompileArgumentsContributor(
            KotlinCompileArgumentsProvider(this)
        )
    }

    override fun setupCompilerArgs(args: T, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        abstractKotlinCompileArgumentsContributor.contributeArguments(
            args,
            compilerArgumentsConfigurationFlags(defaultsOnly, ignoreClasspathResolutionErrors)
        )
        if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
            args.reportPerf = true
        }
    }
}

open class KotlinCompileArgumentsProvider<T : AbstractKotlinCompile<out CommonCompilerArguments>>(taskProvider: T) {

    val coroutines: Provider<Coroutines> = taskProvider.coroutines
    val logger: Logger = taskProvider.logger
    val isMultiplatform: Boolean = taskProvider.multiPlatformEnabled.get()
    private val pluginData = taskProvider.kotlinPluginData?.orNull
    val pluginClasspath: FileCollection = listOfNotNull(taskProvider.pluginClasspath, pluginData?.classpath).reduce(FileCollection::plus)
    val pluginOptions: CompilerPluginOptions =
        listOfNotNull(taskProvider.pluginOptions, pluginData?.options).reduce(CompilerPluginOptions::plus)
}

class KotlinJvmCompilerArgumentsProvider
    (taskProvider: KotlinCompile) : KotlinCompileArgumentsProvider<KotlinCompile>(taskProvider) {
    val moduleName: String = taskProvider.moduleName.get()
    val friendPaths: FileCollection = taskProvider.friendPaths
    val compileClasspath: Iterable<File> = taskProvider.classpath
    val destinationDir: File = taskProvider.destinationDir
    internal val kotlinOptions: List<KotlinJvmOptionsImpl> = listOfNotNull(
        taskProvider.parentKotlinOptionsImpl.orNull as? KotlinJvmOptionsImpl,
        taskProvider.kotlinOptions as KotlinJvmOptionsImpl
    )
}

internal inline val <reified T : Task> T.thisTaskProvider: TaskProvider<out T>
    get() = checkNotNull(project.locateTask<T>(name))

@CacheableTask
abstract class KotlinCompile @Inject constructor(
    override val kotlinOptions: KotlinJvmOptions,
    workerExecutor: WorkerExecutor
) : AbstractKotlinCompile<K2JVMCompilerArguments>(),
    KotlinJvmCompile,
    UsesKotlinJavaToolchain {

    internal open class Configurator<T : KotlinCompile>(
        kotlinCompilation: KotlinCompilationData<*>,
        private val properties: PropertiesProvider
    ) : AbstractKotlinCompile.Configurator<T>(kotlinCompilation) {

        companion object {
            private const val TRANSFORMS_REGISTERED = "_kgp_internal_kotlin_compile_transforms_registered"

            val ARTIFACT_TYPE_ATTRIBUTE: Attribute<String> = Attribute.of("artifactType", String::class.java)
            private const val DIRECTORY_ARTIFACT_TYPE = "directory"
            private const val JAR_ARTIFACT_TYPE = "jar"
            const val CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE = "classpath-entry-snapshot"
        }

        /**
         * Prepares for configuration of the task. This method must be called during build configuration, not during task configuration
         * (which typically happens after build configuration). The reason is that some actions must be performed early (e.g., creating
         * configurations should be done early to avoid issues with composite builds (https://issuetracker.google.com/183952598)).
         */
        fun runAtConfigurationTime(taskProvider: TaskProvider<T>, project: Project) {
            if (properties.useClasspathSnapshot) {
                registerTransformsOnce(project)
                project.configurations.create(classpathSnapshotConfigurationName(taskProvider.name)).apply {
                    project.dependencies.add(name, project.files(project.provider { taskProvider.get().classpath }))
                }
            }
        }

        private fun registerTransformsOnce(project: Project) {
            if (project.extensions.extraProperties.has(TRANSFORMS_REGISTERED)) {
                return
            }
            project.extensions.extraProperties[TRANSFORMS_REGISTERED] = true

            project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
                it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_ARTIFACT_TYPE)
                it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            }
            project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
                it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_ARTIFACT_TYPE)
                it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            }
        }

        private fun classpathSnapshotConfigurationName(taskName: String) = "_kgp_internal_${taskName}_classpath_snapshot"

        override fun configure(task: T) {
            super.configure(task)

            val compileJavaTaskProvider = when (compilation) {
                is KotlinJvmCompilation -> compilation.compileJavaTaskProvider
                is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
                is KotlinWithJavaCompilation -> compilation.compileJavaTaskProvider
                else -> null
            }

            if (compileJavaTaskProvider != null) {
                task.associatedJavaCompileTaskTargetCompatibility.set(
                    compileJavaTaskProvider.map { it.targetCompatibility }
                )
                task.associatedJavaCompileTaskSources.from(
                    compileJavaTaskProvider.map { javaTask ->
                        javaTask.source
                    }
                )
                task.associatedJavaCompileTaskName.set(
                    compileJavaTaskProvider.map { it.name }
                )
            }
            task.moduleName.set(task.project.provider {
                task.kotlinOptions.moduleName ?: task.parentKotlinOptionsImpl.orNull?.moduleName ?: compilation.moduleName
            })

            if (properties.useClasspathSnapshot) {
                val classpathSnapshot = task.project.configurations.getByName(classpathSnapshotConfigurationName(task.name))
                task.classpathSnapshotProperties.classpathSnapshot.from(
                    classpathSnapshot.incoming.artifactView {
                        it.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
                    }.files
                )
                val classpathSnapshotDir = getClasspathSnapshotDir(task)
                task.classpathSnapshotProperties.classpathSnapshotDir.value(classpathSnapshotDir).disallowChanges()
            } else {
                task.classpathSnapshotProperties.classpath.from(task.project.provider { task.classpath })
            }
        }
    }

    @get:Internal
    internal val parentKotlinOptionsImpl: Property<KotlinJvmOptions> = objects.property(KotlinJvmOptions::class.java)

    @get:Internal
    @field:Transient
    internal open val sourceRootsContainer = FilteringSourceRootsContainer(objects)

    private val jvmSourceRoots by project.provider {
        // serialize in the task state for configuration caching; avoid building anew in task execution, as it may access the project model
        SourceRoots.ForJvm.create(source, sourceRootsContainer, sourceFilesExtensions.get())
    }

    /** A package prefix that is used for locating Java sources in a directory structure with non-full-depth packages.
     *
     * Example: a Java source file with `package com.example.my.package` is located in directory `src/main/java/my/package`.
     * Then, for the Kotlin compilation to locate the source file, use package prefix `"com.example"` */
    @get:Input
    @get:Optional
    var javaPackagePrefix: String? = null

    @get:Input
    var usePreciseJavaTracking: Boolean = true
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.usePreciseJavaTracking=$value" }
        }

    @Internal // To support compile avoidance (ClasspathSnapshotProperties.classpathSnapshot will be used as input instead)
    override fun getClasspath(): FileCollection {
        return super.getClasspath()
    }

    @get:Nested
    abstract val classpathSnapshotProperties: ClasspathSnapshotProperties

    /** Properties related to the `kotlin.incremental.useClasspathSnapshot` feature. */
    abstract class ClasspathSnapshotProperties {
        @get:Input
        abstract val useClasspathSnapshot: Property<Boolean>

        @get:Classpath
        @get:Incremental
        @get:Optional // Set if useClasspathSnapshot == true
        abstract val classpathSnapshot: ConfigurableFileCollection

        @get:Classpath
        @get:Incremental
        @get:Optional // Set if useClasspathSnapshot == false (to restore the existing classpath annotations when the feature is disabled)
        abstract val classpath: ConfigurableFileCollection

        @get:OutputDirectory
        @get:Optional // Set if useClasspathSnapshot == true
        abstract val classpathSnapshotDir: DirectoryProperty
    }

    override val incrementalProps: List<FileCollection>
        get() = listOf(stableSources, commonSourceSet, classpathSnapshotProperties.classpath, classpathSnapshotProperties.classpathSnapshot)

    // Exclude classpathSnapshotDir from TaskOutputsBackup (see TaskOutputsBackup's kdoc for more info). */
    override val taskOutputsBackupExcludes: List<File>
        get() = classpathSnapshotProperties.classpathSnapshotDir.orNull?.asFile?.let { listOf(it) } ?: emptyList()

    @get:Internal
    internal val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> = objects
        .propertyWithNewInstance(
            project.gradle,
            { this }
        )

    final override val kotlinJavaToolchainProvider: Provider<KotlinJavaToolchain> = defaultKotlinJavaToolchain.cast()

    @get:Internal
    override val compilerRunner: Provider<GradleCompilerRunner> = objects.propertyWithConvention(
        // From Gradle 6.6 better to replace flatMap with provider.zip()
        defaultKotlinJavaToolchain.flatMap { toolchain ->
            objects.property(gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    toolchain.currentJvmJdkToolsJar.orNull,
                    normalizedKotlinDaemonJvmArguments.orNull,
                    metrics.get(),
                    compilerExecutionStrategy.get(),
                    workerExecutor
                )
            })
        }
    )

    @get:Internal
    internal abstract val associatedJavaCompileTaskTargetCompatibility: Property<String>

    @get:Internal
    internal abstract val associatedJavaCompileTaskSources: ConfigurableFileCollection

    @get:Internal
    internal abstract val associatedJavaCompileTaskName: Property<String>

    @get:Internal
    internal abstract val jvmTargetValidationMode: Property<PropertiesProvider.JvmTargetValidationMode>

    init {
        incremental = true
    }

    override fun createCompilerArgs(): K2JVMCompilerArguments =
        K2JVMCompilerArguments()

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compilerArgumentsContributor.contributeArguments(
            args, compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )

        // This method could be called on configuration phase to calculate `filteredArgumentsMap` property
        if (state.executing) {
            defaultKotlinJavaToolchain.get().updateJvmTarget(this, args)
        }

        if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
            args.reportPerf = true
        }
    }

    @get:Internal
    internal val compilerArgumentsContributor: CompilerArgumentsContributor<K2JVMCompilerArguments> by lazy {
        KotlinJvmCompilerArgumentsContributor(KotlinJvmCompilerArgumentsProvider(this))
    }

    override fun getSourceRoots(): SourceRoots.ForJvm = jvmSourceRoots

    override fun callCompilerAsync(
        args: K2JVMCompilerArguments,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        sourceRoots as SourceRoots.ForJvm

        validateKotlinAndJavaHasSameTargetCompatibility(args, sourceRoots)

        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                changedFiles = getChangedFiles(inputChanges, incrementalProps),
                classpathChanges = getClasspathChanges(inputChanges),
                workingDir = taskBuildDirectory.get().asFile,
                usePreciseJavaTracking = usePreciseJavaTracking,
                disableMultiModuleIC = disableMultiModuleIC,
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        @Suppress("ConvertArgumentToSet")
        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, messageCollector, outputItemCollector,
            // In the incremental compiler, outputFiles will be cleaned on rebuild. However, because classpathSnapshotDir is not included in
            // TaskOutputsBackup, we don't want classpathSnapshotDir to be cleaned immediately on rebuild, and therefore we exclude it from
            // outputFiles here. (See TaskOutputsBackup's kdoc for more info.)
            outputFiles = allOutputFiles()
                    - (classpathSnapshotProperties.classpathSnapshotDir.orNull?.asFile?.let { setOf(it) } ?: emptySet()),
            reportingSettings = reportingSettings(),
            incrementalCompilationEnvironment = icEnv,
            kotlinScriptExtensions = sourceFilesExtensions.get().toTypedArray()
        )
        compilerRunner.runJvmCompilerAsync(
            sourceRoots.kotlinSourceFiles.files.toList(),
            commonSourceSet.toList(),
            sourceRoots.javaSourceRoots,
            javaPackagePrefix,
            args,
            environment,
            defaultKotlinJavaToolchain.get().providedJvm.get().javaHome,
            taskOutputsBackup
        )
    }

    private fun validateKotlinAndJavaHasSameTargetCompatibility(args: K2JVMCompilerArguments, sourceRoots: SourceRoots.ForJvm) {
        val mixedSourcesArePresent = !associatedJavaCompileTaskSources.isEmpty && !sourceRoots.kotlinSourceFiles.isEmpty
        if (mixedSourcesArePresent) {
            associatedJavaCompileTaskTargetCompatibility.orNull?.let { targetCompatibility ->
                val normalizedJavaTarget = when (targetCompatibility) {
                    "6" -> "1.6"
                    "7" -> "1.7"
                    "8" -> "1.8"
                    "1.9" -> "9"
                    else -> targetCompatibility
                }

                val jvmTarget = args.jvmTarget ?: JvmTarget.DEFAULT.toString()
                if (normalizedJavaTarget != jvmTarget) {
                    val javaTaskName = associatedJavaCompileTaskName.get()
                    val errorMessage = "'$javaTaskName' task (current target is $targetCompatibility) and " +
                            "'$name' task (current target is $jvmTarget) " +
                            "jvm target compatibility should be set to the same Java version."
                    when (jvmTargetValidationMode.get()) {
                        PropertiesProvider.JvmTargetValidationMode.ERROR -> throw GradleException(errorMessage)
                        PropertiesProvider.JvmTargetValidationMode.WARNING -> logger.warn(errorMessage)
                        else -> Unit
                    }
                }
            }
        }
    }

    @get:Input
    val disableMultiModuleIC: Boolean by lazy {
        if (!isIncrementalCompilationEnabled() || !javaOutputDir.isPresent) {
            false
        } else {

            var illegalTaskOrNull: AbstractCompile? = null
            project.tasks.configureEach {
                if (it is AbstractCompile &&
                    it !is JavaCompile &&
                    it !is AbstractKotlinCompile<*> &&
                    javaOutputDir.get().asFile.isParentOf(it.destinationDir)
                ) {
                    illegalTaskOrNull = illegalTaskOrNull ?: it
                }
            }
            if (illegalTaskOrNull != null) {
                val illegalTask = illegalTaskOrNull!!
                logger.info(
                    "Kotlin inter-project IC is disabled: " +
                            "unknown task '$illegalTask' destination dir ${illegalTask.destinationDir} " +
                            "intersects with java destination dir $javaOutputDir"
                )
            }
            illegalTaskOrNull != null
        }
    }

    // override setSource to track source directory sets and files (for generated android folders)
    override fun setSource(sources: Any) {
        sourceRootsContainer.set(sources)
        super.setSource(sources)
    }

    // override source to track source directory sets and files (for generated android folders)
    override fun source(vararg sources: Any): SourceTask {
        sourceRootsContainer.add(*sources)
        return super.source(*sources)
    }

    private fun getClasspathChanges(inputChanges: InputChanges): ClasspathChanges = when {
        !classpathSnapshotProperties.useClasspathSnapshot.get() -> ClasspathSnapshotDisabled
        else -> {
            val classpathSnapshotFiles = ClasspathSnapshotFiles(
                classpathSnapshotProperties.classpathSnapshot.files.toList(),
                classpathSnapshotProperties.classpathSnapshotDir.get().asFile
            )
            when {
                !inputChanges.isIncremental -> NotAvailableForNonIncrementalRun(classpathSnapshotFiles)
                inputChanges.getFileChanges(classpathSnapshotProperties.classpathSnapshot).none() -> NoChanges(classpathSnapshotFiles)
                !classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.exists() -> {
                    // When this happens, it means that the classpath snapshot in the previous run was not saved for some reason. It's
                    // likely that there were no source files to compile, so the task action was skipped (see
                    // AbstractKotlinCompile.executeImpl), and therefore the classpath snapshot was not saved.
                    // Missing classpath snapshot will make this run non-incremental, but because there were no source files to compile in
                    // the previous run, *all* source files in this run (if there are any) need to be compiled anyway, so being
                    // non-incremental is actually okay.
                    NotAvailableDueToMissingClasspathSnapshot(classpathSnapshotFiles)
                }
                else -> ToBeComputedByIncrementalCompiler(classpathSnapshotFiles)
            }
        }
    }
}

@CacheableTask
abstract class Kotlin2JsCompile @Inject constructor(
    override val kotlinOptions: KotlinJsOptions,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {

    init {
        incremental = true
    }

    open class Configurator<T : Kotlin2JsCompile>(compilation: KotlinCompilationData<*>) :
        AbstractKotlinCompile.Configurator<T>(compilation) {

        override fun configure(task: T) {
            super.configure(task)

            task.outputFileProperty.value(
                task.project.provider {
                    task.kotlinOptions.outputFile?.let(::File)
                        ?: task.destinationDirectory.locationOnly.get().asFile.resolve("${compilation.ownModuleName}.js")
                }
            ).disallowChanges()
            task.optionalOutputFile.fileProvider(
                task.outputFileProperty.flatMap { outputFile ->
                    task.project.provider {
                        outputFile.takeUnless { task.kotlinOptions.isProduceUnzippedKlib() }
                    }
                }
            ).disallowChanges()
            val libraryCacheService = task.project.rootProject.gradle.sharedServices.registerIfAbsent(
                "${LibraryFilterCachingService::class.java.canonicalName}_${LibraryFilterCachingService::class.java.classLoader.hashCode()}",
                LibraryFilterCachingService::class.java
            ) {}
            task.libraryCache.set(libraryCacheService).also { task.libraryCache.disallowChanges() }
        }
    }

    internal abstract class LibraryFilterCachingService : BuildService<BuildServiceParameters.None>, AutoCloseable {
        internal data class LibraryFilterCacheKey(val dependency: File, val irEnabled: Boolean, val preIrDisabled: Boolean)

        private val cache = ConcurrentHashMap<LibraryFilterCacheKey, Boolean>()

        fun getOrCompute(key: LibraryFilterCacheKey, compute: (File) -> Boolean) = cache.computeIfAbsent(key) {
            compute(it.dependency)
        }

        override fun close() {
            cache.clear()
        }
    }

    @get:Input
    internal var incrementalJsKlib: Boolean = true

    override fun isIncrementalCompilationEnabled(): Boolean =
        when {
            "-Xir-produce-js" in kotlinOptions.freeCompilerArgs -> {
                false
            }
            "-Xir-produce-klib-dir" in kotlinOptions.freeCompilerArgs -> {
                KotlinBuildStatsService.applyIfInitialised {
                    it.report(BooleanMetrics.JS_KLIB_INCREMENTAL, incrementalJsKlib)
                }
                incrementalJsKlib
            }
            "-Xir-produce-klib-file" in kotlinOptions.freeCompilerArgs -> {
                KotlinBuildStatsService.applyIfInitialised {
                    it.report(BooleanMetrics.JS_KLIB_INCREMENTAL, incrementalJsKlib)
                }
                incrementalJsKlib
            }
            else -> incremental
        }

    // This can be file or directory
    @get:Internal
    abstract val outputFileProperty: Property<File>

    @Deprecated("Please use outputFileProperty, this is kept for backwards compatibility.", replaceWith = ReplaceWith("outputFileProperty"))
    @get:Internal
    val outputFile: File
        get() = outputFileProperty.get()

    @get:OutputFile
    @get:Optional
    abstract val optionalOutputFile: RegularFileProperty

    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    normalizedKotlinDaemonJvmArguments.orNull,
                    metrics.get(),
                    compilerExecutionStrategy.get(),
                    workerExecutor
                )
            }
        )

    override fun createCompilerArgs(): K2JSCompilerArguments =
        K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        try {
            outputFileProperty.get().canonicalPath
        } catch (ex: Throwable) {
            logger.warn("IO EXCEPTION: outputFile: ${outputFileProperty.get().path}")
            throw ex
        }

        args.outputFile = outputFileProperty.get().absoluteFile.normalize().absolutePath

        if (defaultsOnly) return

        (kotlinOptions as KotlinJsOptionsImpl).updateArguments(args)
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions.get())

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val friendDependencies: FileCollection = objectFactory
        .fileCollection()
        .from(friendPaths)
        .filter {
            // .jar files are not required for js compilation as friend modules
            // and, because of `@InputFiles` and different normalization strategy from `@Classpath`,
            // they produce build cache misses
            it.exists() && !it.name.endsWith(".jar") && libraryFilter(it)
        }

    @Suppress("unused")
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val sourceMapBaseDirs: FileCollection?
        get() = (kotlinOptions as KotlinJsOptionsImpl).sourceMapBaseDirs

    private fun isHybridKotlinJsLibrary(file: File): Boolean =
        JsLibraryUtils.isKotlinJavascriptLibrary(file) && isKotlinLibrary(file)

    private fun KotlinJsOptions.isPreIrBackendDisabled(): Boolean =
        listOf(
            "-Xir-only",
            "-Xir-produce-js",
            "-Xir-produce-klib-file"
        ).any(freeCompilerArgs::contains)

    // see also isIncrementalCompilationEnabled
    private fun KotlinJsOptions.isIrBackendEnabled(): Boolean =
        listOf(
            "-Xir-produce-klib-dir",
            "-Xir-produce-js",
            "-Xir-produce-klib-file"
        ).any(freeCompilerArgs::contains)

    private val File.asLibraryFilterCacheKey: LibraryFilterCachingService.LibraryFilterCacheKey
        get() = LibraryFilterCachingService.LibraryFilterCacheKey(
            this,
            irEnabled = kotlinOptions.isIrBackendEnabled(),
            preIrDisabled = kotlinOptions.isPreIrBackendDisabled()
        )

    // Kotlin/JS can operate in 3 modes:
    //  1) purely pre-IR backend
    //  2) purely IR backend
    //  3) hybrid pre-IR and IR backend. Can only accept libraries with both JS and IR parts.
    private val libraryFilterBody: (File) -> Boolean
        get() = if (kotlinOptions.isIrBackendEnabled()) {
            if (kotlinOptions.isPreIrBackendDisabled()) {
                //::isKotlinLibrary
                // Workaround for KT-47797
                { isKotlinLibrary(it) }
            } else {
                ::isHybridKotlinJsLibrary
            }
        } else {
            JsLibraryUtils::isKotlinJavascriptLibrary
        }

    @get:Internal
    internal abstract val libraryCache: Property<LibraryFilterCachingService>

    @get:Internal
    protected val libraryFilter: (File) -> Boolean
        get() = { file ->
            libraryCache.get().getOrCompute(file.asLibraryFilterCacheKey, libraryFilterBody)
        }

    @get:Internal
    internal val absolutePathProvider = project.projectDir.absolutePath

    override val incrementalProps: List<FileCollection>
        get() = super.incrementalProps + listOf(friendDependencies)

    override fun callCompilerAsync(
        args: K2JSCompilerArguments,
        sourceRoots: SourceRoots,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()

        if (kotlinOptions.isIrBackendEnabled()) {
            logger.info(USING_JS_IR_BACKEND_MESSAGE)
        }

        val dependencies = classpath
            .filter { it.exists() && libraryFilter(it) }
            .map { it.canonicalPath }

        args.libraries = dependencies.distinct().let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependencies.files.joinToString(File.pathSeparator) { it.absolutePath }

        if (args.sourceMapBaseDirs == null && !args.sourceMapPrefix.isNullOrEmpty()) {
            args.sourceMapBaseDirs = absolutePathProvider
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                getChangedFiles(inputChanges, incrementalProps),
                ClasspathChanges.NotAvailableForJSCompiler,
                taskBuildDirectory.get().asFile,
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, messageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings(),
            incrementalCompilationEnvironment = icEnv
        )
        compilerRunner.runJsCompilerAsync(
            sourceRoots.kotlinSourceFiles.files.toList(),
            commonSourceSet.toList(),
            args,
            environment,
            taskOutputsBackup
        )
    }
}

data class KotlinCompilerPluginData(
    @get:Classpath
    val classpath: FileCollection,

    @get:Internal
    val options: CompilerPluginOptions,

    /**
     * Used only for Up-to-date checks
     */
    @get:Nested
    val inputsOutputsState: InputsOutputsState
) {
    data class InputsOutputsState(
        @get:Input
        val inputs: Map<String, String>,

        @get:InputFiles
        @get:IgnoreEmptyDirectories
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val inputFiles: Set<File>,

        @get:OutputFiles
        val outputFiles: Set<File>
    )
}
