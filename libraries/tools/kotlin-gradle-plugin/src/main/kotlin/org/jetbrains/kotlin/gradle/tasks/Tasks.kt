/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentAwareWithInput
import org.jetbrains.kotlin.gradle.internal.prepareCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.ParsedGradleVersion
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import org.jetbrains.kotlin.gradle.utils.toSortedPathsArray
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File
import java.util.*
import kotlin.properties.Delegates

const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin incremental compilation"
const val USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE = "Using experimental Kotlin/JS incremental compilation"

abstract class AbstractKotlinCompileTool<T : CommonToolArguments>() : AbstractCompile(), CompilerArgumentAwareWithInput<T> {
    private fun useCompilerClasspathConfigurationMessage(propertyName: String) {
        project.logger.kotlinWarn(
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

    @get:Classpath @get:InputFiles
    internal val computedCompilerClasspath: List<File>
        get() = compilerClasspath?.takeIf { it.isNotEmpty() }
                ?: compilerJarFile?.let {
                    // a hack to remove compiler jar from the cp, will be dropped when compilerJarFile will be removed
                    listOf(it) + findKotlinCompilerClasspath(project).filter { !it.name.startsWith("kotlin-compiler") }
                }
                ?: if (!useFallbackCompilerSearch) {
                    try {
                        project.configurations.getByName(COMPILER_CLASSPATH_CONFIGURATION_NAME).resolve().toList()
                    } catch (e: Exception) {
                        project.logger.error(
                            "Could not resolve compiler classpath. " +
                                    "Check if Kotlin Gradle plugin repository is configured in $project."
                        )
                        throw e
                    }
                } else {
                    findKotlinCompilerClasspath(project)
                }
                ?: throw IllegalStateException("Could not find Kotlin Compiler classpath")


    protected abstract fun findKotlinCompilerClasspath(project: Project): List<File>
}

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractKotlinCompileTool<T>() {

    init {
        cacheOnlyIfEnabledForKotlin()
    }

    // avoid creating directory in getter: this can lead to failure in parallel build
    @get:LocalState
    internal val taskBuildDirectory: File
        get() = File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name)

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
    internal val buildHistoryFile: File get() = File(taskBuildDirectory, "build-history.bin")

    @get:Input
    internal var useModuleDetection: Boolean = false

    @get:Internal
    protected val multiModuleICSettings: MultiModuleICSettings
        get() = MultiModuleICSettings(buildHistoryFile, useModuleDetection)

    @get:Classpath
    @get:InputFiles
    val pluginClasspath: FileCollection
        get() = project.configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Classpath @get:InputFiles
    protected val additionalClasspath = arrayListOf<File>()

    @get:Internal // classpath already participates in the checks
    internal val compileClasspath: Iterable<File>
        get() = (classpath + additionalClasspath)
                .filterTo(LinkedHashSet(), File::exists)

    private val kotlinExt: KotlinProjectExtension
            get() = project.extensions.findByType(KotlinProjectExtension::class.java)!!

    private lateinit var destinationDirProvider: Lazy<File>

    override fun getDestinationDir(): File {
        return destinationDirProvider.value
    }

    fun setDestinationDir(provider: () -> File) {
        destinationDirProvider = lazy(provider)
    }

    override fun setDestinationDir(destinationDir: File) {
        destinationDirProvider = lazyOf(destinationDir)
    }

    @get:Internal
    internal var coroutinesFromGradleProperties: Coroutines? = null
    // Input is needed to force rebuild even if source files are not changed
    @get:Input
    internal val coroutinesStr: String
            get() = coroutines.name

    private val coroutines: Coroutines
        get() = kotlinExt.experimental.coroutines
                ?: coroutinesFromGradleProperties
                ?: Coroutines.DEFAULT

    @get:Internal
    internal var friendTaskName: String? = null

    @get:Internal
    internal var javaOutputDir: File? = null

    @get:Internal
    internal var sourceSetName: String by Delegates.notNull()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal var commonSourceSet: Iterable<File> = emptyList()

    @get:Input
    internal val moduleName: String
        get() {
            val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
                    ?: project.name
            val suffix = if (sourceSetName == "main") "" else "_$sourceSetName"
            return filterModuleName("${baseName}$suffix")
        }

    @Suppress("UNCHECKED_CAST")
    @get:Internal
    internal val friendTask: AbstractKotlinCompile<T>?
            get() = friendTaskName?.let { project.tasks.findByName(it) } as? AbstractKotlinCompile<T>

    /** Classes directories that are not produced by this task but should be consumed by
     * other tasks that have this one as a [friendTask]. */
    private val attachedClassesDirs: MutableList<Lazy<File?>> = mutableListOf()

    /** Registers the directory provided by the [provider] as attached, meaning that the directory should
     * be consumed as a friend classes directory by other tasks that have this task as a [friendTask]. */
    fun attachClassesDir(provider: () -> File?) {
        attachedClassesDirs += lazy(provider)
    }

    @get:Internal // takes part in the compiler arguments
    var friendPaths: Lazy<Array<String>?> = lazy {
        friendTask?.let { friendTask ->
            val possibleFriendDirs = ArrayList<File?>().apply {
                add(friendTask.javaOutputDir)
                add(friendTask.destinationDir)
                addAll(friendTask.attachedClassesDirs.map { it.value })
            }

            possibleFriendDirs.filterNotNullTo(HashSet())
                .map { it.absolutePath }
                .toTypedArray()
        }
    }

    override fun compile() {
        assert(false, { "unexpected call to compile()" })
    }

    @TaskAction
    open fun execute(inputs: IncrementalTaskInputs): Unit {
        // Check that the JDK tools are available in Gradle (fail-fast, instead of a fail during the compiler run):
        findToolsJar()

        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "all kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (allKotlinSources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = prepareCompilerArguments()
        taskBuildDirectory.mkdirs()
        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }

    @Internal
    internal abstract fun getSourceRoots(): SourceRoots

    internal abstract fun callCompiler(args: T, sourceRoots: SourceRoots, changedFiles: ChangedFiles)

    override fun setupCompilerArgs(args: T, defaultsOnly: Boolean) {
        args.coroutinesState = when (coroutines) {
            Coroutines.ENABLE -> CommonCompilerArguments.ENABLE
            Coroutines.WARN -> CommonCompilerArguments.WARN
            Coroutines.ERROR -> CommonCompilerArguments.ERROR
            Coroutines.DEFAULT -> CommonCompilerArguments.DEFAULT
        }

        logger.kotlinDebug { "args.coroutinesState=${args.coroutinesState}" }

        if (project.logger.isDebugEnabled) {
            args.verbose = true
        }

        args.multiPlatform = project.plugins.any { it is KotlinPlatformPluginBase || it is KotlinMultiplatformPluginWrapper }

        setupPlugins(args)
    }

    fun setupPlugins(compilerArgs: T) {
        compilerArgs.pluginClasspaths = pluginClasspath.toSortedPathsArray()
        compilerArgs.pluginOptions = pluginOptions.arguments.toTypedArray()
    }

    protected fun hasFilesInTaskBuildDirectory(): Boolean {
        val taskBuildDir = taskBuildDirectory
        return taskBuildDir.walk().any { it != taskBuildDir && it.isFile }
    }
}

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

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly)

        args.addCompilerBuiltIns = true

        val gradleVersion = getGradleVersion()
        if (gradleVersion == null || gradleVersion >= ParsedGradleVersion(3, 2)) {
            args.loadBuiltInsFromDependencies = true
        }

        args.moduleName = friendTask?.moduleName ?: moduleName
        logger.kotlinDebug { "args.moduleName = ${args.moduleName}" }

        args.friendPaths = friendPaths.value
        logger.kotlinDebug { "args.friendPaths = ${args.friendPaths?.joinToString() ?: "[]"}" }

        if (defaultsOnly) return

        args.classpathAsList = compileClasspath.toList()
        args.destinationAsFile = destinationDir
        parentKotlinOptionsImpl?.updateArguments(args)
        kotlinOptionsImpl.updateArguments(args)

        logger.kotlinDebug { "$name destinationDir = $destinationDir" }
    }

    @Internal
    override fun getSourceRoots() = SourceRoots.ForJvm.create(getSource(), sourceRootsContainer)

    override fun callCompiler(args: K2JVMCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.ForJvm

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = GradleCompilerRunner(project)

        val environment = when {
            !incremental ->
                GradleCompilerEnvironment(computedCompilerClasspath, messageCollector, outputItemCollector, args)
            else -> {
                logger.info(USING_INCREMENTAL_COMPILATION_MESSAGE)
                GradleIncrementalCompilerEnvironment(
                        computedCompilerClasspath,
                        if (hasFilesInTaskBuildDirectory()) changedFiles else ChangedFiles.Unknown(),
                        taskBuildDirectory,
                        messageCollector, outputItemCollector, args,
                        usePreciseJavaTracking = usePreciseJavaTracking,
                        localStateDirs = outputDirectories,
                        multiModuleICSettings = multiModuleICSettings
                )
            }
        }

        if (!incremental) {
            clearOutputDirectories(reason = "IC is disabled for the task")
        }

        try {
            val exitCode = compilerRunner.runJvmCompiler(
                sourceRoots.kotlinSourceFiles,
                commonSourceSet.toList(),
                sourceRoots.javaSourceRoots,
                javaPackagePrefix,
                args,
                environment
            )

            disableMultiModuleICIfNeeded()
            processCompilerExitCode(exitCode)
        }
        catch (e: Throwable) {
            cleanupOnError()
            throw e
        }
    }

    private fun disableMultiModuleICIfNeeded() {
        if (!incremental || javaOutputDir == null) return

        val illegalTask = project.tasks.firstOrNull {
            it is AbstractCompile &&
                    it !is JavaCompile &&
                    it !is AbstractKotlinCompile<*> &&
                    javaOutputDir!!.isParentOf(it.destinationDir)
        } as? AbstractCompile

        if (illegalTask != null) {
            project.logger.info(
                "Kotlin inter-project IC is disabled: " +
                        "unknown task '$illegalTask' destination dir ${illegalTask.destinationDir} " +
                        "intersects with java destination dir $javaOutputDir"
            )
            buildHistoryFile.delete()
        }
    }

    private fun cleanupOnError() {
        logger.kotlinInfo("deleting $destinationDir on error")
        destinationDir.deleteRecursively()
    }

    private fun processCompilerExitCode(exitCode: ExitCode) {
        if (exitCode != ExitCode.OK) {
            cleanupOnError()
        }

        throwGradleExceptionIfError(exitCode)
    }

    // override setSource to track source directory sets and files (for generated android folders)
    override fun setSource(sources: Any?) {
        sourceRootsContainer.set(sources)
        super.setSource(sources)
    }

    // override source to track source directory sets and files (for generated android folders)
    override fun source(vararg sources: Any?): SourceTask? {
        sourceRootsContainer.add(*sources)
        return super.source(*sources)
    }
}

@CacheableTask
open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {
    private val kotlinOptionsImpl = KotlinJsOptionsImpl()

    override val kotlinOptions: KotlinJsOptions
            get() = kotlinOptionsImpl

    private val defaultOutputFile: File
        get() = File(destinationDir, "$moduleName.js")

    @Suppress("unused")
    @get:OutputFile
    val outputFile: File
        get() = kotlinOptions.outputFile?.let(::File) ?: defaultOutputFile

    override fun findKotlinCompilerClasspath(project: Project): List<File> =
            findKotlinJsCompilerClasspath(project)

    override fun createCompilerArgs(): K2JSCompilerArguments =
            K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean) {
        args.apply { fillDefaultValues() }
        super.setupCompilerArgs(args, defaultsOnly)

        args.outputFile = outputFile.canonicalPath

        if (defaultsOnly) return

        kotlinOptionsImpl.updateArguments(args)
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource())

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val friendDependency
        get() = friendTaskName
                ?.let { project.getTasksByName(it, false).singleOrNull() as? Kotlin2JsCompile }
                ?.outputFile?.parentFile
                ?.let { if (LibraryUtils.isKotlinJavascriptLibrary(it)) it else null }
                ?.absolutePath

    override fun callCompiler(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()

        val dependencies = compileClasspath
                .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                .map { it.canonicalPath }

        args.libraries = (dependencies + listOfNotNull(friendDependency)).distinct().let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependency

        if (args.sourceMapBaseDirs == null && !args.sourceMapPrefix.isNullOrEmpty()) {
            args.sourceMapBaseDirs = project.projectDir.absolutePath
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = GradleCompilerRunner(project)

        val environment = when {
            incremental -> {
                logger.warn(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
                GradleIncrementalCompilerEnvironment(
                        computedCompilerClasspath,
                        if (hasFilesInTaskBuildDirectory()) changedFiles else ChangedFiles.Unknown(),
                        taskBuildDirectory,
                        messageCollector,
                        outputItemCollector,
                        args,
                        multiModuleICSettings = multiModuleICSettings
                )
            }
            else -> {
                GradleCompilerEnvironment(computedCompilerClasspath, messageCollector, outputItemCollector, args)
            }
        }

        val exitCode = compilerRunner.runJsCompiler(sourceRoots.kotlinSourceFiles, commonSourceSet.toList(), args, environment)
        throwGradleExceptionIfError(exitCode)
    }
}

private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

private fun filterModuleName(moduleName: String): String =
    moduleName.replace(invalidModuleNameCharactersRegex, "_")

private fun Task.getGradleVersion(): ParsedGradleVersion? {
    val gradleVersion = project.gradle.gradleVersion
    val result = ParsedGradleVersion.parse(gradleVersion)
    if (result == null) {
        project.logger.kotlinDebug("Could not parse gradle version: $gradleVersion")
    }
    return result
}

internal class GradleMessageCollector(val logger: Logger) : MessageCollector {
    private var hasErrors = false

    override fun hasErrors() = hasErrors

    override fun clear() {
        // Do nothing
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        fun formatMsg(prefix: String) =
            buildString {
                append("$prefix: ")

                location?.apply {
                    append("$path: ")
                    if (line > 0 && column > 0) {
                        append("($line, $column): ")
                    }
                }

                append(message)
            }

        when (severity) {
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.EXCEPTION ->  {
                hasErrors = true
                logger.error(formatMsg("e"))
            }

            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.STRONG_WARNING -> {
                logger.warn(formatMsg("w"))
            }
            CompilerMessageSeverity.INFO -> {
                logger.info(formatMsg("i"))
            }
            CompilerMessageSeverity.LOGGING,
            CompilerMessageSeverity.OUTPUT -> {
                logger.debug(formatMsg("v"))
            }
        }!! // !! is used to force compile-time exhaustiveness
    }
}
