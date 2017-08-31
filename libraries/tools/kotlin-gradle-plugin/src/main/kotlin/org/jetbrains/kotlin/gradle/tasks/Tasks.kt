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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.annotation.AnnotationFileUpdaterImpl
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.kotlinInfo
import org.jetbrains.kotlin.gradle.utils.ParsedGradleVersion
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistry
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File
import java.util.*
import kotlin.properties.Delegates

const val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"
const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin incremental compilation"
const val USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE = "Using experimental Kotlin/JS incremental compilation"

abstract class AbstractKotlinCompileTool<T : CommonToolArguments>() : AbstractCompile() {
    var compilerJarFile: File? = null
    internal val compilerJar: File
        get() = compilerJarFile
                ?: findKotlinCompilerJar(project)
                ?: throw IllegalStateException("Could not find Kotlin Compiler jar. Please specify $name.compilerJarFile")
    protected abstract fun findKotlinCompilerJar(project: Project): File?
}

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractKotlinCompileTool<T>(), CompilerArgumentAware {
    internal val taskBuildDirectory: File
        get() = File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name).apply { mkdirs() }

    private val cacheVersions: List<CacheVersion>
        get() =
            listOf(normalCacheVersion(taskBuildDirectory, enabled = incremental),
                   dataContainerCacheVersion(taskBuildDirectory, enabled = incremental),
                   gradleCacheVersion(taskBuildDirectory, enabled = incremental))

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    var incremental: Boolean = false
        get() = field
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
        }

    internal val isCacheFormatUpToDate: Boolean
        get() {
            if (!incremental) return true

            return cacheVersions.all { it.checkVersion() == CacheVersion.Action.DO_NOTHING }
        }

    abstract protected fun createCompilerArgs(): T

    internal val pluginOptions = CompilerPluginOptions()

    protected val additionalClasspath = arrayListOf<File>()
    protected val compileClasspath: Iterable<File>
        get() = (classpath + additionalClasspath)
                .filterTo(LinkedHashSet(), File::exists)

    override val serializedCompilerArguments: List<String>
        get() {
            val arguments = createCompilerArgs()
            setupCompilerArgs(arguments)
            return ArgumentUtils.convertArgumentsToStringList(arguments)
        }

    override val defaultSerializedCompilerArguments: List<String>
        get() {
            val arguments = createCompilerArgs()
            setupCompilerArgs(arguments, true)
            return ArgumentUtils.convertArgumentsToStringList(arguments)
        }

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

    internal var coroutinesFromGradleProperties: Coroutines? = null
    // Input is needed to force rebuild even if source files are not changed
    @get:Input
    internal val coroutinesStr: String
            get() = coroutines.name

    private val coroutines: Coroutines
        get() = kotlinExt.experimental.coroutines
                ?: coroutinesFromGradleProperties
                ?: Coroutines.DEFAULT

    internal var compilerCalled: Boolean = false
    // TODO: consider more reliable approach (see usage)
    internal var anyClassesCompiled: Boolean = false
    internal var friendTaskName: String? = null
    internal var javaOutputDir: File? = null
    internal var sourceSetName: String by Delegates.notNull()

    @get:Input
    internal val moduleName: String
        get() {
            val baseName = project.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
                    ?: project.name
            val suffix = if (sourceSetName == "main") "" else "_$sourceSetName"
            return "$baseName$suffix"
        }

    @Suppress("UNCHECKED_CAST")
    protected val friendTask: AbstractKotlinCompile<T>?
            get() = friendTaskName?.let { project.tasks.findByName(it) } as? AbstractKotlinCompile<T>

    /** Classes directories that are not produced by this task but should be consumed by
     * other tasks that have this one as a [friendTask]. */
    private val attachedClassesDirs: MutableList<Lazy<File?>> = mutableListOf()

    /** Registers the directory provided by the [provider] as attached, meaning that the directory should
     * be consumed as a friend classes directory by other tasks that have this task as a [friendTask]. */
    fun attachClassesDir(provider: () -> File?) {
        attachedClassesDirs += lazy(provider)
    }

    var friendPaths: Lazy<Array<String>?> = lazy {
        friendTask?.let { friendTask ->
            mutableListOf<String>().apply {
                add((friendTask.javaOutputDir ?: friendTask.destinationDir).absolutePath)
                addAll(friendTask.attachedClassesDirs.mapNotNull { it.value?.absolutePath })
            }.toTypedArray()
        }
    }

    override fun compile() {
        assert(false, { "unexpected call to compile()" })
    }

    @TaskAction
    open fun execute(inputs: IncrementalTaskInputs): Unit {
        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "all kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (allKotlinSources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = createCompilerArgs()
        setupCompilerArgs(args)

        compilerCalled = true
        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }

    internal abstract fun getSourceRoots(): SourceRoots
    internal abstract fun callCompiler(args: T, sourceRoots: SourceRoots, changedFiles: ChangedFiles)

    open fun setupCompilerArgs(args: T, defaultsOnly: Boolean = false) {
        args.coroutinesState = when (coroutines) {
            Coroutines.ENABLE -> CommonCompilerArguments.ENABLE
            Coroutines.WARN -> CommonCompilerArguments.WARN
            Coroutines.ERROR -> CommonCompilerArguments.ERROR
        }

        logger.kotlinDebug { "args.coroutinesState=${args.coroutinesState}" }

        if (project.logger.isDebugEnabled) {
            args.verbose = true
        }

        setupPlugins(args)
    }

    open fun setupPlugins(compilerArgs: T) {
        compilerArgs.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        compilerArgs.pluginOptions = pluginOptions.arguments.toTypedArray()
    }
}

open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
    internal var parentKotlinOptionsImpl: KotlinJvmOptionsImpl? = null
    private val kotlinOptionsImpl = KotlinJvmOptionsImpl()
    override val kotlinOptions: KotlinJvmOptions
            get() = kotlinOptionsImpl
    internal open val sourceRootsContainer = FilteringSourceRootsContainer()

    private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null

    val kaptOptions = KaptOptions()

    /** A package prefix that is used for locating Java sources in a directory structure with non-full-depth packages.
     *
     * Example: a Java source file with `package com.example.my.package` is located in directory `src/main/java/my/package`.
     * Then, for the Kotlin compilation to locate the source file, use package prefix `"com.example"` */
    var javaPackagePrefix: String? = null

    @get:Input
    internal val javaPackagePrefixInputString get() = javaPackagePrefix ?: ""

    internal var artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider? = null
    internal var artifactFile: File? = null

    init {
        incremental = true
    }

    override fun findKotlinCompilerJar(project: Project): File? =
            findKotlinJvmCompilerJar(project)

    override fun createCompilerArgs(): K2JVMCompilerArguments =
            K2JVMCompilerArguments()

    override fun setupPlugins(compilerArgs: K2JVMCompilerArguments) {
        compilerArgs.pluginClasspaths = pluginOptions.classpath.toTypedArray()

        val kaptPluginOptions = getKaptPluginOptions()
        compilerArgs.pluginOptions = (pluginOptions.arguments + kaptPluginOptions.arguments).toTypedArray()
    }

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean) {
        super.setupCompilerArgs(args, defaultsOnly)
        args.apply { fillDefaultValues() }

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

    override fun getSourceRoots() = SourceRoots.ForJvm.create(getSource(), sourceRootsContainer)

    override fun callCompiler(args: K2JVMCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.ForJvm

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = GradleCompilerRunner(project)
        val reporter = GradleICReporter(project.rootProject.projectDir)

        val environment = when {
            !incremental -> GradleCompilerEnvironment(compilerJar, messageCollector, outputItemCollector, args)
            else -> {
                logger.warn(USING_INCREMENTAL_COMPILATION_MESSAGE)
                GradleIncrementalCompilerEnvironment(compilerJar, changedFiles, reporter, taskBuildDirectory,
                        messageCollector, outputItemCollector, args, kaptAnnotationsFileUpdater,
                        artifactDifferenceRegistryProvider,
                        artifactFile)
            }
        }

        if (!incremental) {
            logger.kotlinDebug { "Removing all kotlin classes in $destinationDir" }
            destinationDir.deleteRecursively()
            destinationDir.mkdirs()
        }

        try {
            val exitCode = compilerRunner.runJvmCompiler(
                    sourceRoots.kotlinSourceFiles,
                    sourceRoots.javaSourceRoots,
                    javaPackagePrefix,
                    args,
                    environment)

            processCompilerExitCode(exitCode)
            artifactDifferenceRegistryProvider?.withRegistry(reporter) {
                it.flush(true)
            }
        }
        catch (e: Throwable) {
            cleanupOnError()
            artifactDifferenceRegistryProvider?.clean()
            throw e
        }
        finally {
            artifactDifferenceRegistryProvider?.withRegistry(reporter, ArtifactDifferenceRegistry::close)
        }
        anyClassesCompiled = true
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

    private fun getKaptPluginOptions() =
            CompilerPluginOptions().apply {
                kaptOptions.annotationsFile?.let { kaptAnnotationsFile ->
                    if (incremental) {
                        kaptAnnotationsFileUpdater = AnnotationFileUpdaterImpl(kaptAnnotationsFile)
                    }

                    addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "output", kaptAnnotationsFile.canonicalPath)
                }

                if (kaptOptions.generateStubs) {
                    addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "stubs", destinationDir.canonicalPath)
                }

                if (kaptOptions.supportInheritedAnnotations) {
                    addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "inherited", true.toString())
                }
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

open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {
    private val kotlinOptionsImpl = KotlinJsOptionsImpl()
    override val kotlinOptions: KotlinJsOptions
            get() = kotlinOptionsImpl

    private val defaultOutputFile: File
            get() = File(destinationDir, "$moduleName.js")

    @Suppress("unused")
    val outputFile: String
        get() = kotlinOptions.outputFile ?: defaultOutputFile.canonicalPath

    override fun findKotlinCompilerJar(project: Project): File? =
            findKotlinJsCompilerJar(project)

    override fun createCompilerArgs(): K2JSCompilerArguments =
            K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean) {
        super.setupCompilerArgs(args, defaultsOnly)
        args.apply { fillDefaultValues() }
        args.outputFile = outputFile

        if (defaultsOnly) return

        kotlinOptionsImpl.updateArguments(args)
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource())

    override fun callCompiler(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()

        val friendDependency = friendTaskName
                ?.let { project.getTasksByName(it, false).singleOrNull() as? Kotlin2JsCompile }
                ?.outputFile
                ?.let { File(it).parentFile }
                ?.let { if (LibraryUtils.isKotlinJavascriptLibrary(it)) it else null }
                ?.absolutePath

        val dependencies = compileClasspath
                .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                .map { it.canonicalPath }

        args.libraries = (dependencies + listOfNotNull(friendDependency)).let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependency

        args.sourceMapSourceRoots = source.orEmpty()
                .asSequence()
                .filterIsInstance<SourceDirectorySet>()
                .flatMap { it.srcDirs.asSequence() }
                .joinToString(File.pathSeparator) { it.absolutePath }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = GradleCompilerRunner(project)
        val reporter = GradleICReporter(project.rootProject.projectDir)

        val environment = when {
            incremental -> {
                logger.warn(USING_EXPERIMENTAL_JS_INCREMENTAL_COMPILATION_MESSAGE)
                GradleIncrementalCompilerEnvironment(
                        compilerJar, changedFiles, reporter, taskBuildDirectory,
                        messageCollector, outputItemCollector, args)
            }
            else -> {
                GradleCompilerEnvironment(compilerJar, messageCollector, outputItemCollector, args)
            }
        }

        val exitCode = compilerRunner.runJsCompiler(sourceRoots.kotlinSourceFiles, args, environment)
        throwGradleExceptionIfError(exitCode)
    }
}

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
