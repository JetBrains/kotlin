/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
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
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.kotlinWarn
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.ir.isProduceUnzippedKlib
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import org.jetbrains.kotlin.gradle.utils.toSortedPathsArray
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.utils.JsLibraryUtils
import java.io.File
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

    // a hack to remove compiler jar from the cp, will be dropped when compilerJarFile will be removed
    private val classpathWithCompilerJar
        get() = project.objects.fileCollection()
            .from(compilerJarFile, defaultCompilerClasspath.filter { !it.nameWithoutExtension.startsWith("kotlin-compiler") })

    protected abstract fun findKotlinCompilerClasspath(project: Project): List<File>

    init {
        doFirst {
            require(!defaultCompilerClasspath.isEmpty) {
                "Default Kotlin compiler classpath is empty! Task: ${path} (${this::class.qualifiedName})"
            }
        }
    }
}

public class GradleCompileTaskProvider {

    constructor(task: Task) {
        buildDir = task.project.buildDir
        projectDir = task.project.rootProject.projectDir
        rootDir = task.project.rootProject.rootDir
        sessionsDir = GradleCompilerRunner.sessionsDir(task.project)
        projectName = task.project.rootProject.name.normalizeForFlagFile()
        val modulesInfo = GradleCompilerRunner.buildModulesInfo(task.project.gradle)
        buildModulesInfo = task.project.gradle.sharedServices.registerIfAbsent(
            IncrementalModuleInfoBuildService.getServiceName(), IncrementalModuleInfoBuildService::class.java
        ) {
            it.parameters.info.set(modulesInfo)
        }
        path = task.path
        logger = task.logger
    }

    val path: String
    val logger: Logger
    val buildDir: File /*= project.buildDir*/
    val projectDir: File /*= project.rootProject.projectDir*/
    val rootDir: File /*= project.rootProject.rootDir*/
    val sessionsDir: File/* = GradleCompilerRunner.sessionsDir(project)*/
    val projectName: String /*= project.rootProject.name.normalizeForFlagFile()*/
    val buildModulesInfo: Provider<out IncrementalModuleInfoProvider> /*= GradleCompilerRunner.buildModulesInfo(project.gradle)*/
}

abstract class AbstractKotlinCompile<T : CommonCompilerArguments> : AbstractKotlinCompileTool<T>(), UsesKotlinJavaToolchain {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    private val layout = project.layout

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:LocalState
    internal val taskBuildDirectory: File by layout.buildDirectory.dir(KOTLIN_BUILD_DIR_NAME).map { it.file(name).asFile }


    @get:Internal
    internal val projectObjects = project.objects

    private val localStateDirectoriesProvider: FileCollection = projectObjects.fileCollection().from({ taskBuildDirectory })

    override fun localStateDirectories(): FileCollection = localStateDirectoriesProvider

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    // todo: deprecate and remove (we may need to design api for configuring IC)
    // don't rely on it to check if IC is enabled, use isIncrementalCompilationEnabled instead
    @get:Internal
    var incremental: Boolean = false
        get() = field
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
        }

    @Input
    internal open fun isIncrementalCompilationEnabled(): Boolean =
        incremental

    @get:Internal
    internal var reportingSettings = ReportingSettings()

    @get:Internal
    internal val taskData: KotlinCompileTaskData = KotlinCompileTaskData.get(project, name)

    @get:Input
    internal open var useModuleDetection: Boolean
        get() = taskData.useModuleDetection.get()
        set(value) {
            taskData.useModuleDetection.set(value)
        }

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(taskData.buildHistoryFile, useModuleDetection)

    @get:InputFiles
    @get:Classpath
    open val pluginClasspath: FileCollection by project.provider {
        // FIXME support compiler plugins with PM20
        (taskData.compilation as? KotlinCompilation<*>?)?.pluginConfigurationName?.let(project.configurations::getByName)
            ?: project.files()
    }

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Classpath
    @get:InputFiles
    protected val additionalClasspath = arrayListOf<File>()

    @get:Internal
    internal val objects = project.objects

    // Store this file collection before it is filtered by File::exists to ensure that Gradle Instant execution doesn't serialize the
    // filtered files, losing those that don't exist yet and will only be created during build
    private val compileClasspathImpl by project.provider {
        classpath + objects.fileCollection().from(additionalClasspath)
    }

    @get:Internal // classpath already participates in the checks
    internal val compileClasspath: Iterable<File>
        get() = compileClasspathImpl

    @field:Transient
    private val sourceFilesExtensionsSources: MutableList<Iterable<String>> = mutableListOf()

    @get:Input
    val sourceFilesExtensions: List<String> by project.provider {
        DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + sourceFilesExtensionsSources.flatten()
    }

    internal fun sourceFilesExtensions(extensions: Iterable<String>) {
        sourceFilesExtensionsSources.add(extensions)
    }

    @get:Internal
    @field:Transient
    internal val kotlinExtProvider: KotlinTopLevelExtension = project.extensions.findByType(KotlinTopLevelExtension::class.java)!!

    override fun getDestinationDir(): File =
        taskData.destinationDir.get()

    override fun setDestinationDir(provider: Provider<File>) {
        taskData.destinationDir.set(provider)
    }

    fun setDestinationDir(provider: () -> File) {
        taskData.destinationDir.set(project.provider(provider))
    }

    override fun setDestinationDir(destinationDir: File) {
        taskData.destinationDir.set(destinationDir)
    }

    @get:Internal
    internal var coroutinesFromGradleProperties: Coroutines? = null
    // Input is needed to force rebuild even if source files are not changed

    @get:Input
    internal val coroutinesStr: Provider<String> = project.provider { coroutines.get().name }

    @get:Input
    internal val coroutines: Provider<Coroutines> = project.provider {
        kotlinExtProvider.experimental.coroutines
            ?: coroutinesFromGradleProperties
            ?: Coroutines.DEFAULT
    }

    @get:Internal
    internal var javaOutputDir: File?
        get() = taskData.javaOutputDir
        set(value) {
            taskData.javaOutputDir = value
        }

    @get:Internal
    internal val sourceSetName: String
        get() = taskData.compilation.compilationPurpose

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal var commonSourceSet: FileCollection = project.files() //TODO

    @get:Input
    internal val moduleName: String by project.provider {
        taskData.compilation.moduleName
    }

    @get:Internal // takes part in the compiler arguments
    val friendPaths: FileCollection = project.files(
        project.provider { taskData.compilation.friendPaths }
    )

    private val kotlinLogger by lazy { GradleKotlinLogger(logger) }

    final override val kotlinJavaToolchainProvider: Provider<KotlinJavaToolchainProvider> =
        objects.propertyWithNewInstance()

    @get:Internal
    internal val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            kotlinJavaToolchainProvider.map {
                compilerRunner(
                    it.javaExecutable.get().asFile,
                    it.jdkToolsJar.orNull
                )
            }
        )

    // Moved creation here to not violate Gradle configuration cache as [compilerRunner] method is called
    // at execution time
    // by lazy is added so properties of task extending this one are captured - required for incremental
    // compilation
    @get:Internal
    protected val gradleCompileTaskProvider by lazy {
        GradleCompileTaskProvider(this)
    }

    internal open fun compilerRunner(
        javaExecutable: File,
        jdkToolsJar: File?
    ): GradleCompilerRunner = GradleCompilerRunner(
        gradleCompileTaskProvider,
        javaExecutable,
        jdkToolsJar
    )

    private val systemPropertiesService = CompilerSystemPropertiesService.registerIfAbsent(project.gradle)

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {
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
        taskBuildDirectory.mkdirs()
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
    internal val isMultiplatform: Boolean by lazy {
        project.plugins.any { it is KotlinPlatformPluginBase || it is KotlinMultiplatformPluginWrapper || it is KotlinPm20PluginWrapper }
    }

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

    internal fun setupPlugins(compilerArgs: T) {
        compilerArgs.pluginClasspaths = pluginClasspath.toSortedPathsArray()
        compilerArgs.pluginOptions = pluginOptions.arguments.toTypedArray()
    }

    protected fun hasFilesInTaskBuildDirectory(): Boolean {
        val taskBuildDir = taskBuildDirectory
        return taskBuildDir.walk().any { it != taskBuildDir && it.isFile }
    }
}

open class KotlinCompileArgumentsProvider<T : AbstractKotlinCompile<out CommonCompilerArguments>>(taskProvider: T) {

    val coroutines: Provider<Coroutines>
    val logger: Logger
    val isMultiplatform: Boolean
    val pluginClasspath: FileCollection
    val pluginOptions: CompilerPluginOptions

    init {
        coroutines = taskProvider.coroutines
        logger = taskProvider.logger
        isMultiplatform = taskProvider.isMultiplatform
        pluginClasspath = taskProvider.pluginClasspath
        pluginOptions = taskProvider.pluginOptions
    }
}

class KotlinJvmCompilerArgumentsProvider
    (taskProvider: KotlinCompile) : KotlinCompileArgumentsProvider<KotlinCompile>(taskProvider) {

    val moduleName: String
    val friendPaths: FileCollection
    val compileClasspath: Iterable<File>
    val destinationDir: File
    internal val kotlinOptions: List<KotlinJvmOptionsImpl?>

    init {
//        super(taskProvider)
        moduleName = taskProvider.moduleName
        friendPaths = taskProvider.friendPaths
        compileClasspath = taskProvider.compileClasspath
        destinationDir = taskProvider.destinationDir
        kotlinOptions = listOfNotNull(
            taskProvider.parentKotlinOptionsImpl,
            taskProvider.kotlinOptions as KotlinJvmOptionsImpl
        )
    }
}

internal inline val <reified T : Task> T.thisTaskProvider: TaskProvider<out T>
    get() = checkNotNull(project.locateTask<T>(name))

@CacheableTask
open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
    @get:Internal
    internal var parentKotlinOptionsImpl: KotlinJvmOptionsImpl? = null

    override val kotlinOptions: KotlinJvmOptions
        get() = taskData.compilation.kotlinOptions as KotlinJvmOptions

    @get:Internal
    @field:Transient
    internal open val sourceRootsContainer = FilteringSourceRootsContainer(project.objects)

    private val jvmSourceRoots by project.provider {
        // serialize in the task state for configuration caching; avoid building anew in task execution, as it may access the project model
        SourceRoots.ForJvm.create(source, sourceRootsContainer, sourceFilesExtensions)
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

        val messageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                if (hasFilesInTaskBuildDirectory()) changedFiles else ChangedFiles.Unknown(),
                taskBuildDirectory,
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
            kotlinScriptExtensions = sourceFilesExtensions.toTypedArray()
        )
        compilerRunner.runJvmCompilerAsync(
            sourceRoots.kotlinSourceFiles,
            commonSourceSet.toList(),
            sourceRoots.javaSourceRoots,
            javaPackagePrefix,
            args,
            environment
        )
    }

    @get:Input
    val disableMultiModuleIC: Boolean by lazy {
        if (!isIncrementalCompilationEnabled() || javaOutputDir == null) {
            false
        } else {

            var illegalTaskOrNull: AbstractCompile? = null
            project.tasks.configureEach {
                if (it is AbstractCompile &&
                    it !is JavaCompile &&
                    it !is AbstractKotlinCompile<*> &&
                    javaOutputDir!!.isParentOf(it.destinationDir)
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
internal open class KotlinCompileWithWorkers @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : KotlinCompile() {

    override fun compilerRunner(
        javaExecutable: File,
        jdkToolsJar: File?
    ) = GradleCompilerRunnerWithWorkers(
        gradleCompileTaskProvider,
        javaExecutable,
        jdkToolsJar,
        workerExecutor
    )
}

@CacheableTask
internal open class Kotlin2JsCompileWithWorkers @Inject constructor(
    objectFactory: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(objectFactory) {

    override fun compilerRunner(
        javaExecutable: File,
        jdkToolsJar: File?
    ) = GradleCompilerRunnerWithWorkers(
        gradleCompileTaskProvider,
        javaExecutable,
        jdkToolsJar,
        workerExecutor
    )
}

@CacheableTask
internal open class KotlinCompileCommonWithWorkers @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : KotlinCompileCommon() {
    override fun compilerRunner(
        javaExecutable: File,
        jdkToolsJar: File?
    ) = GradleCompilerRunnerWithWorkers(
        gradleCompileTaskProvider,
        javaExecutable,
        jdkToolsJar,
        workerExecutor
    )
}

@CacheableTask
open class Kotlin2JsCompile @Inject constructor(
    objectFactory: ObjectFactory
) : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {

    init {
        incremental = true
    }

    override val kotlinOptions = taskData.compilation.kotlinOptions as KotlinJsOptions

    @get:Internal
    protected val defaultOutputFile: File
        get() = File(destinationDir, "${taskData.compilation.ownModuleName}.js")

    @get:Input
    internal var incrementalJsKlib: Boolean = true

    override fun isIncrementalCompilationEnabled(): Boolean =
        when {
            "-Xir-produce-js" in kotlinOptions.freeCompilerArgs -> false
            "-Xir-produce-klib-dir" in kotlinOptions.freeCompilerArgs -> false // TODO: it's not supported yet
            "-Xir-produce-klib-file" in kotlinOptions.freeCompilerArgs -> incrementalJsKlib
            else -> incremental
        }

    @get:Internal
    val outputFile: File
        get() = kotlinOptions.outputFile?.let(::File) ?: defaultOutputFile

    @get:OutputFile
    @get:Optional
    val outputFileOrNull: File?
        get() = outputFile.let { file ->
            if (!kotlinOptions.isProduceUnzippedKlib()) {
                file
            } else {
                null
            }
        }

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
        findKotlinJsCompilerClasspath(project)

    override fun createCompilerArgs(): K2JSCompilerArguments =
        K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        try {
            outputFile.canonicalPath
        } catch (ex: Throwable) {
            logger.warn("IO EXCEPTION: outputFile: ${outputFile.path}")
            throw ex
        }

        args.outputFile = outputFile.absoluteFile.normalize().absolutePath

        if (defaultsOnly) return

        (kotlinOptions as KotlinJsOptionsImpl).updateArguments(args)
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions)

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

    // Kotlin/JS can operate in 3 modes:
    //  1) purely pre-IR backend
    //  2) purely IR backend
    //  3) hybrid pre-IR and IR backend. Can only accept libraries with both JS and IR parts.
    private val libraryFilter: (File) -> Boolean
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
    internal val absolutePathProvider = project.projectDir.absolutePath

    override fun callCompilerAsync(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()

        if (kotlinOptions.isIrBackendEnabled()) {
            logger.info(USING_JS_IR_BACKEND_MESSAGE)
        }

        val dependencies = compileClasspath
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
                if (hasFilesInTaskBuildDirectory()) changedFiles else ChangedFiles.Unknown(),
                taskBuildDirectory,
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath, messageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings,
            incrementalCompilationEnvironment = icEnv
        )
        compilerRunner.runJsCompilerAsync(sourceRoots.kotlinSourceFiles, commonSourceSet.toList(), args, environment)
    }
}
