/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.*
import org.jetbrains.kotlin.gradle.plugin.COMPILER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformPluginBase
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithTransitiveClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.ownModuleName
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.utils.LibraryUtils
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

    private fun useCompilerClasspathConfigurationMessage(propertyName: String) {
        logger.kotlinWarn(
            "'$path.$propertyName' is deprecated and will be removed soon. " +
                    "Use '$COMPILER_CLASSPATH_CONFIGURATION_NAME' " +
                    "configuration for customizing compiler classpath."
        )
    }

    // TODO: remove
    @get:Internal
    var compilerJarFile: File? = null
        @Deprecated("Use $COMPILER_CLASSPATH_CONFIGURATION_NAME configuration")
        set(value) {
            useCompilerClasspathConfigurationMessage("compilerJarFile")
            field = value
        }

    // TODO: remove
    @get:Internal
    var compilerClasspath: List<File>? = null
        @Deprecated("Use $COMPILER_CLASSPATH_CONFIGURATION_NAME configuration")
        set(value) {
            useCompilerClasspathConfigurationMessage("compilerClasspath")
            field = value
        }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()

    @get:Input
    internal var useFallbackCompilerSearch: Boolean = false

    @get:Classpath
    @get:InputFiles
    internal val computedCompilerClasspath: List<File> by project.provider {
        compilerClasspath?.takeIf { it.isNotEmpty() }
            ?: compilerJarFile?.let {
                // a hack to remove compiler jar from the cp, will be dropped when compilerJarFile will be removed
                listOf(it) + findKotlinCompilerClasspath(project).filter { !it.name.startsWith("kotlin-compiler") }
            }
            ?: if (!useFallbackCompilerSearch) {
                try {
                    project.configurations.getByName(COMPILER_CLASSPATH_CONFIGURATION_NAME).resolve().toList()
                } catch (e: Exception) {
                    logger.error(
                        "Could not resolve compiler classpath. " +
                                "Check if Kotlin Gradle plugin repository is configured in $project."
                    )
                    throw e
                }
            } else {
                findKotlinCompilerClasspath(project)
            }
            ?: throw IllegalStateException("Could not find Kotlin Compiler classpath")
    }


    protected abstract fun findKotlinCompilerClasspath(project: Project): List<File>
}

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractKotlinCompileTool<T>() {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:LocalState
    internal val taskBuildDirectory: File by project.provider {
        File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name)
    }

    override fun localStateDirectories(): FileCollection = project.files(taskBuildDirectory)

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    @get:Input
    var incremental: Boolean = false
        get() = field
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
        }

    @get:Internal
    internal var buildReportMode: BuildReportMode? = null

    @get:Internal
    internal val taskData: KotlinCompileTaskData by project.provider {
        KotlinCompileTaskData.get(project, name)
    }

    @get:Input
    internal open var useModuleDetection: Boolean
        get() = taskData.useModuleDetection.get()
        set(value) {
            taskData.useModuleDetection.set(value)
        }

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(taskData.buildHistoryFile, useModuleDetection)

    @get:Classpath
    @get:InputFiles
    val pluginClasspath: FileCollection by project.provider {
        project.configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
    }

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Classpath
    @get:InputFiles
    protected val additionalClasspath = arrayListOf<File>()

    // Store this file collection before it is filtered by File::exists to ensure that Gradle Instant execution doesn't serialize the
    // filtered files, losing those that don't exist yet and will only be created during build
    private val compileClasspathImpl by project.provider {
        classpath + project.files(additionalClasspath)
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

    private val kotlinExt: KotlinProjectExtension
        get() = project.extensions.findByType(KotlinProjectExtension::class.java)!!

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
    internal val coroutinesStr: String
        get() = coroutines.name

    @get:Internal
    internal val coroutines: Coroutines by project.provider {
        kotlinExt.experimental.coroutines
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
        get() = taskData.compilation.name

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal var commonSourceSet: FileCollection = project.files()

    @get:Input
    internal val moduleName: String by project.provider {
        taskData.compilation.moduleName
    }

    @get:Internal // takes part in the compiler arguments
    val friendPaths: Array<String> by project.provider {
        taskData.compilation.run {
            associateWithTransitiveClosure
                .flatMap { it.output.classesDirs }
                .plus(friendArtifacts)
                .map { it.canonicalPath }.toTypedArray()
        }
    }

    private val kotlinLogger by lazy { GradleKotlinLogger(logger) }

    internal open fun compilerRunner(): GradleCompilerRunner =
        GradleCompilerRunner(this)

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {
        // If task throws exception, but its outputs are changed during execution,
        // then Gradle forces next build to be non-incremental (see Gradle's DefaultTaskArtifactStateRepository#persistNewOutputs)
        // To prevent this, we backup outputs before incremental build and restore when exception is thrown
        val outputsBackup: TaskOutputsBackup? =
            if (incremental && inputs.isIncremental)
                kotlinLogger.logTime("Backing up outputs for incremental build") {
                    TaskOutputsBackup(allOutputFiles())
                }
            else null

        if (!incremental) {
            clearLocalState("IC is disabled")
        }

        try {
            executeImpl(inputs)
        } catch (t: Throwable) {
            if (outputsBackup != null) {
                kotlinLogger.logTime("Restoring previous outputs on error") {
                    outputsBackup.restoreOutputs()
                }
            }
            throw t
        }
    }

    protected open fun skipCondition(inputs: IncrementalTaskInputs): Boolean {
        return !inputs.isIncremental && getSourceRoots().kotlinSourceFiles.isEmpty()
    }

    private fun executeImpl(inputs: IncrementalTaskInputs) {
        // Check that the JDK tools are available in Gradle (fail-fast, instead of a fail during the compiler run):
        findToolsJar()

        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "All kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (skipCondition(inputs)) {
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

    @get:Internal
    internal val isMultiplatform: Boolean by project.provider {
        project.plugins.any { it is KotlinPlatformPluginBase || it is KotlinMultiplatformPluginWrapper }
    }

    override fun setupCompilerArgs(args: T, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        AbstractKotlinCompileArgumentsContributor(thisTaskProvider).contributeArguments(
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

internal inline val <reified T : Task> T.thisTaskProvider: TaskProvider<out T>
    get() = checkNotNull(project.locateTask<T>(name))

@CacheableTask
open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
    @get:Internal
    internal var parentKotlinOptionsImpl: KotlinJvmOptionsImpl? = null

    private val kotlinOptionsImpl = KotlinJvmOptionsImpl()

    override val kotlinOptions: KotlinJvmOptions
        get() = kotlinOptionsImpl

    @get:Internal
    internal open val sourceRootsContainer = FilteringSourceRootsContainer()

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
        compilerArgumentsContributor.contributeArguments(args, compilerArgumentsConfigurationFlags(
            defaultsOnly,
            ignoreClasspathResolutionErrors
        ))
    }

    @get:Internal
    internal val compilerArgumentsContributor: CompilerArgumentsContributor<K2JVMCompilerArguments> by project.provider {
        KotlinJvmCompilerArgumentsContributor(thisTaskProvider)
    }

    @Internal
    override fun getSourceRoots() = SourceRoots.ForJvm.create(getSource(), sourceRootsContainer, sourceFilesExtensions)

    override fun callCompilerAsync(args: K2JVMCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.ForJvm

        val messageCollector = GradlePrintingMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner()

        val icEnv = if (incremental) {
            logger.info(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                if (hasFilesInTaskBuildDirectory()) changedFiles else ChangedFiles.Unknown(),
                taskBuildDirectory,
                usePreciseJavaTracking = usePreciseJavaTracking,
                disableMultiModuleIC = disableMultiModuleIC(),
                multiModuleICSettings = multiModuleICSettings
            )
        } else null

        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath, messageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            buildReportMode = buildReportMode,
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

    private fun disableMultiModuleIC(): Boolean {
        if (!incremental || javaOutputDir == null) return false

        fun forEachTask(fn: (Task) -> Unit) {
            if (isGradleVersionAtLeast(4, 10)) {
                project.tasks.configureEach(fn)
            } else {
                project.tasks.forEach(fn)
            }
        }

        var illegalTaskOrNull: AbstractCompile? = null

        forEachTask {
            if (it is AbstractCompile &&
                it !is JavaCompile &&
                it !is AbstractKotlinCompile<*> &&
                javaOutputDir!!.isParentOf(it.destinationDir)
            ) {
                illegalTaskOrNull = illegalTaskOrNull ?: it
            }
        }

        illegalTaskOrNull?.let { illegalTask ->
            logger.info(
                "Kotlin inter-project IC is disabled: " +
                        "unknown task '$illegalTask' destination dir ${illegalTask.destinationDir} " +
                        "intersects with java destination dir $javaOutputDir"
            )
            return true
        }

        return false
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
    override fun compilerRunner() = GradleCompilerRunnerWithWorkers(this, workerExecutor)
}

@CacheableTask
internal open class Kotlin2JsCompileWithWorkers @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : Kotlin2JsCompile() {
    override fun compilerRunner() = GradleCompilerRunnerWithWorkers(this, workerExecutor)
}

@CacheableTask
internal open class KotlinCompileCommonWithWorkers @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : KotlinCompileCommon() {
    override fun compilerRunner() = GradleCompilerRunnerWithWorkers(this, workerExecutor)
}

@CacheableTask
open class Kotlin2JsCompile : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {

    init {
        incremental = true
    }

    private val kotlinOptionsImpl = KotlinJsOptionsImpl()

    override val kotlinOptions: KotlinJsOptions
        get() = kotlinOptionsImpl

    private val defaultOutputFile: File
        get() = File(destinationDir, "${taskData.compilation.ownModuleName}.js")

    @Suppress("unused")
    @get:OutputFile
    val outputFile: File
        get() = kotlinOptions.outputFile?.let(::File) ?: defaultOutputFile

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
        findKotlinJsCompilerClasspath(project)

    override fun createCompilerArgs(): K2JSCompilerArguments =
        K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        args.outputFile = outputFile.canonicalPath

        if (defaultsOnly) return

        kotlinOptionsImpl.updateArguments(args)
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource(), sourceFilesExtensions)

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val friendDependencies: List<String>
        get() {
            val filter = libraryFilter
            return friendPaths.filter { filter(File(it)) }
        }

    @Suppress("unused")
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val sourceMapBaseDirs: FileCollection?
        get() = kotlinOptionsImpl.sourceMapBaseDirs

    private fun isHybridKotlinJsLibrary(file: File): Boolean =
        LibraryUtils.isKotlinJavascriptLibrary(file) && isKotlinLibrary(file)

    private fun KotlinJsOptions.isPreIrBackendDisabled(): Boolean =
        listOf(
            "-Xir-only",
            "-Xir-produce-js",
            "-Xir-produce-klib-file"
        ).any(freeCompilerArgs::contains)

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
            LibraryUtils::isKotlinJavascriptLibrary
        }

    override fun callCompilerAsync(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()

        if (kotlinOptions.isIrBackendEnabled()) {
            logger.info(USING_JS_IR_BACKEND_MESSAGE)
            incremental = false
        }

        val dependencies = compileClasspath
            .filter { it.exists() && libraryFilter(it) }
            .map { it.canonicalPath }

        args.libraries = (dependencies + friendDependencies).distinct().let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependencies.joinToString(File.pathSeparator)

        if (args.sourceMapBaseDirs == null && !args.sourceMapPrefix.isNullOrEmpty()) {
            args.sourceMapBaseDirs = project.projectDir.absolutePath
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val messageCollector = GradlePrintingMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner()

        val icEnv = if (incremental) {
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
            buildReportMode = buildReportMode,
            incrementalCompilationEnvironment = icEnv
        )
        compilerRunner.runJsCompilerAsync(sourceRoots.kotlinSourceFiles, commonSourceSet.toList(), args, environment)
    }
}
