/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.*
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner.Companion.normalizeForFlagFile
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.incremental.IncrementalModuleInfoBuildService
import org.jetbrains.kotlin.gradle.incremental.IncrementalModuleInfoProvider
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.tasks.TaskConfigurator
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithTransitiveClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.ir.isProduceUnzippedKlib
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import org.jetbrains.kotlin.incremental.ChangedFiles
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

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()

    @get:Input
    internal var useFallbackCompilerSearch: Boolean = false

    @get:Internal
    override val metrics: BuildMetricsReporter =
        BuildMetricsReporterImpl()

    /**
     * By default should be set by plugin from [COMPILER_CLASSPATH_CONFIGURATION_NAME] configuration.
     *
     * Empty classpath will fail the build.
     */
    @get:Classpath
    internal val defaultCompilerClasspath: ConfigurableFileCollection =
        project.objects.fileCollection()

    @get:Classpath
    @get:InputFiles
    internal val computedCompilerClasspath: FileCollection = project.objects.fileCollection().from({
        when {
            useFallbackCompilerSearch -> findKotlinCompilerClasspath(project)
            else -> defaultCompilerClasspath
        }
    })

    protected abstract fun findKotlinCompilerClasspath(project: Project): List<File>

    init {
        doFirst {
            require(!defaultCompilerClasspath.isEmpty) {
                "Default Kotlin compiler classpath is empty! Task: ${path} (${this::class.qualifiedName})"
            }
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

abstract class AbstractKotlinCompile<T : CommonCompilerArguments> : AbstractKotlinCompileTool<T>() {

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
            task.taskBuildDirectory.value(
                project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/${task.name}")
            ).disallowChanges()
            task.localStateDirectories.from(task.taskBuildDirectory).disallowChanges()
        }
    }

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    private val layout = project.layout

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
    internal var reportingSettings = ReportingSettings()

    @get:Input
    internal val useModuleDetection: Property<Boolean> = objects.property(Boolean::class.java).value(false)

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(buildHistoryFile.get().asFile, useModuleDetection.get())

    @get:InputFiles
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

    @get:Internal
    protected val gradleCompileTaskProvider: Provider<GradleCompileTaskProvider> = objects
        .property(
            objects.newInstance<GradleCompileTaskProvider>(project.gradle, this, project)
        )

    @get:Internal
    internal open val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunner(it, null)
            }
        )

    private val systemPropertiesService = CompilerSystemPropertiesService.registerIfAbsent(project.gradle)

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {
        metrics.measure(BuildTime.GRADLE_TASK_ACTION) {
            systemPropertiesService.get().startIntercept()
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"

            // If task throws exception, but its outputs are changed during execution,
            // then Gradle forces next build to be non-incremental (see Gradle's DefaultTaskArtifactStateRepository#persistNewOutputs)
            // To prevent this, we backup outputs before incremental build and restore when exception is thrown
            val outputsBackup: TaskOutputsBackup? =
                if (isIncrementalCompilationEnabled() && inputs.isIncremental)
                    metrics.measure(BuildTime.BACKUP_OUTPUT) {
                        TaskOutputsBackup(allOutputFiles())
                    }
                else null

            if (!isIncrementalCompilationEnabled()) {
                clearLocalState("IC is disabled")
            } else if (!inputs.isIncremental) {
                clearLocalState("Task cannot run incrementally")
            }

            try {
                executeImpl(inputs)
            } catch (t: Throwable) {
                if (outputsBackup != null) {
                    metrics.measure(BuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
                        outputsBackup.restoreOutputs()
                    }
                }
                throw t
            }
        }
    }

    protected open fun skipCondition(): Boolean =
        getSourceRoots().kotlinSourceFiles.isEmpty()

    private val projectDir = project.rootProject.projectDir

    private fun executeImpl(inputs: IncrementalTaskInputs) {
        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(projectDir)}" }

        if (!inputs.isIncremental && skipCondition()) {
            // Skip running only if non-incremental run. Otherwise, we may need to do some cleanup.
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = prepareCompilerArguments()
        taskBuildDirectory.get().asFile.mkdirs()
        callCompilerAsync(args, sourceRoots, ChangedFiles(inputs))
    }

    @Internal
    internal abstract fun getSourceRoots(): SourceRoots

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    internal abstract fun callCompilerAsync(args: T, sourceRoots: SourceRoots, changedFiles: ChangedFiles)

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
    }
}

open class KotlinCompileArgumentsProvider<T : AbstractKotlinCompile<out CommonCompilerArguments>>(taskProvider: T) {

    val coroutines: Provider<Coroutines> = taskProvider.coroutines
    val logger: Logger = taskProvider.logger
    val isMultiplatform: Boolean = taskProvider.multiPlatformEnabled.get()
    private val pluginData = taskProvider.kotlinPluginData?.orNull
    val pluginClasspath: FileCollection = listOfNotNull(taskProvider.pluginClasspath, pluginData?.classpath).reduce(FileCollection::plus)
    val pluginOptions: CompilerPluginOptions = listOfNotNull(taskProvider.pluginOptions, pluginData?.options).reduce(CompilerPluginOptions::plus)
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
    override val kotlinOptions: KotlinJvmOptions
) : AbstractKotlinCompile<K2JVMCompilerArguments>(),
    KotlinJvmCompile,
    UsesKotlinJavaToolchain {

    class Configurator(kotlinCompilation: KotlinCompilationData<*>) : AbstractKotlinCompile.Configurator<KotlinCompile>(kotlinCompilation) {
        override fun configure(task: KotlinCompile) {
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
                task.associatedJavaCompileTaskName.set(
                    compileJavaTaskProvider.map { it.name }
                )
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

    @get:Input
    abstract val useClasspathSnapshot: Property<Boolean>

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
                GradleCompilerRunner(
                    it,
                    toolchain.currentJvmJdkToolsJar.orNull
                )
            })
        }
    )

    @get:Internal
    internal abstract val associatedJavaCompileTaskTargetCompatibility: Property<String>

    @get:Internal
    internal abstract val associatedJavaCompileTaskName: Property<String>

    @get:Internal
    internal abstract val jvmTargetValidationMode: Property<PropertiesProvider.JvmTargetValidationMode>

    init {
        incremental = true
    }

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
        findKotlinJvmCompilerClasspath(project)

    override fun createCompilerArgs(): K2JVMCompilerArguments =
        K2JVMCompilerArguments()

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compilerArgumentsContributor.contributeArguments(
            args, compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )
    }

    @get:Internal
    internal val compilerArgumentsContributor: CompilerArgumentsContributor<K2JVMCompilerArguments> by lazy {
        KotlinJvmCompilerArgumentsContributor(KotlinJvmCompilerArgumentsProvider(this))
    }

    override fun getSourceRoots(): SourceRoots.ForJvm = jvmSourceRoots

    override fun callCompilerAsync(args: K2JVMCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.ForJvm

        validateKotlinAndJavaHasSameTargetCompatibility(args)

        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                changedFiles,
                taskBuildDirectory.get().asFile,
                usePreciseJavaTracking = usePreciseJavaTracking,
                disableMultiModuleIC = disableMultiModuleIC,
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath, messageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings,
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
            defaultKotlinJavaToolchain.get().providedJvm.get().javaHome
        )
    }

    private fun validateKotlinAndJavaHasSameTargetCompatibility(args: K2JVMCompilerArguments) {
        associatedJavaCompileTaskTargetCompatibility.orNull?.let { targetCompatibility ->
            val normalizedJavaVersion = if (targetCompatibility == "1.9") "9" else targetCompatibility
            if (normalizedJavaVersion != args.jvmTarget) {
                val javaTaskName = associatedJavaCompileTaskName.get()
                val errorMessage = "'$javaTaskName' task (current target is $targetCompatibility) and " +
                        "'$name' task (current target is ${args.jvmTarget}) " +
                        "jvm target compatibility should be set to the same Java version."
                when (jvmTargetValidationMode.get()) {
                    PropertiesProvider.JvmTargetValidationMode.ERROR -> throw GradleException(errorMessage)
                    PropertiesProvider.JvmTargetValidationMode.WARNING -> logger.warn(errorMessage)
                    else -> Unit
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
}

@CacheableTask
internal abstract class KotlinCompileWithWorkers @Inject constructor(
    kotlinOptions: KotlinJvmOptions,
    workerExecutor: WorkerExecutor
) : KotlinCompile(kotlinOptions) {
    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    workerExecutor
                ) as GradleCompilerRunner
            }
        )
}

@CacheableTask
internal abstract class Kotlin2JsCompileWithWorkers @Inject constructor(
    kotlinOptions: KotlinJsOptions,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(kotlinOptions, objectFactory) {
    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    workerExecutor
                ) as GradleCompilerRunner
            }
        )
}

@CacheableTask
internal abstract class KotlinCompileCommonWithWorkers @Inject constructor(
    kotlinOptions: KotlinMultiplatformCommonOptions,
    workerExecutor: WorkerExecutor
) : KotlinCompileCommon(kotlinOptions) {
    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    workerExecutor
                ) as GradleCompilerRunner
            }
        )
}

@CacheableTask
abstract class Kotlin2JsCompile @Inject constructor(
    override val kotlinOptions: KotlinJsOptions,
    objectFactory: ObjectFactory
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
                    it.report(BooleanMetrics.JS_KLIB_INCREMENTAL, false)
                }
                false
            } // TODO: it's not supported yet
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

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
        findKotlinJsCompilerClasspath(project)

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
                ::isKotlinLibrary
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

    override fun callCompilerAsync(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
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
                changedFiles,
                taskBuildDirectory.get().asFile,
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath, messageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings,
            incrementalCompilationEnvironment = icEnv
        )
        compilerRunner.runJsCompilerAsync(
            sourceRoots.kotlinSourceFiles.files.toList(),
            commonSourceSet.toList(),
            args,
            environment
        )
    }
}

data class KotlinCompilerPluginData(
    @get:InputFiles
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
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val inputFiles: Set<File>,

        @get:OutputFiles
        val outputFiles: Set<File>
    )
}

