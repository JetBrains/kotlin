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

import org.codehaus.groovy.runtime.MethodClosure
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.annotation.SourceAnnotationsRegistry
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.kotlinInfo
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File
import java.util.*
import kotlin.properties.Delegates

const val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"
const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_EXPERIMENTAL_INCREMENTAL_MESSAGE = "Using experimental kotlin incremental compilation"

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractCompile() {
    abstract protected val compiler: CLICompiler<T>
    abstract protected fun populateCompilerArguments(): T

    // indicates that task should compile kotlin incrementally if possible
    // it's not possible when IncrementalTaskInputs#isIncremental returns false (i.e first build)
    var incremental: Boolean = false
        get() = field
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.incremental=$value" }
            System.setProperty("kotlin.incremental.compilation", value.toString())
            System.setProperty("kotlin.incremental.compilation.experimental", value.toString())
        }

    var compilerJarFile: File? = null
    internal var compilerCalled: Boolean = false
    // TODO: consider more reliable approach (see usage)
    internal var anyClassesCompiled: Boolean = false
    internal var friendTaskName: String? = null
    internal var javaOutputDir: File? = null
    internal var sourceSetName: String by Delegates.notNull()
    internal val moduleName: String
            get() = "${project.name}_$sourceSetName"

    override fun compile() {
        assert(false, { "unexpected call to compile()" })
    }

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs): Unit {
        val sourceRoots = getSourceRoots()
        val allKotlinSources = sourceRoots.kotlinSourceFiles

        logger.kotlinDebug { "all kotlin sources: ${allKotlinSources.pathsAsStringRelativeTo(project.rootProject.projectDir)}" }

        if (allKotlinSources.isEmpty()) {
            logger.kotlinDebug { "No Kotlin files found, skipping Kotlin compiler task" }
            return
        }

        sourceRoots.log(this.name, logger)
        val args = populateCompilerArguments()
        compilerCalled = true
        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }

    internal abstract fun getSourceRoots(): SourceRoots
    internal abstract fun callCompiler(args: T, sourceRoots: SourceRoots, changedFiles: ChangedFiles)
}

open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
    override val compiler = K2JVMCompiler()

    internal var parentKotlinOptionsImpl: KotlinJvmOptionsImpl? = null
    private val kotlinOptionsImpl = KotlinJvmOptionsImpl()
    override val kotlinOptions: KotlinJvmOptions
            get() = kotlinOptionsImpl
    internal val sourceRootsContainer = FilteringSourceRootsContainer()

    internal val taskBuildDirectory: File
        get() = File(File(project.buildDir, KOTLIN_BUILD_DIR_NAME), name).apply { mkdirs() }
    private val cacheVersions by lazy {
        listOf(normalCacheVersion(taskBuildDirectory),
               experimentalCacheVersion(taskBuildDirectory),
               dataContainerCacheVersion(taskBuildDirectory),
               gradleCacheVersion(taskBuildDirectory))
    }
    internal val isCacheFormatUpToDate: Boolean
        get() {
            if (!incremental) return true

            return cacheVersions.all { it.checkVersion() == CacheVersion.Action.DO_NOTHING }
        }

    private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null
    private val additionalClasspath = arrayListOf<File>()
    private val compileClasspath: Iterable<File>
            get() = (classpath + additionalClasspath)
                    .filterTo(LinkedHashSet(), File::exists)

    internal val kaptOptions = KaptOptions()
    internal val pluginOptions = CompilerPluginOptions()
    internal var artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider? = null
    internal var artifactFile: File? = null
    // created only if kapt2 is active
    internal var sourceAnnotationsRegistry: SourceAnnotationsRegistry? = null

    override fun populateCompilerArguments(): K2JVMCompilerArguments {
        val args = K2JVMCompilerArguments().apply { fillDefaultValues() }

        handleKaptProperties()
        args.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        args.pluginOptions = pluginOptions.arguments.toTypedArray()
        args.moduleName = moduleName

        friendTaskName?.let { addFriendPathForTestTask(it, args) }
        parentKotlinOptionsImpl?.updateArguments(args)
        kotlinOptionsImpl.updateArguments(args)

        logger.kotlinDebug { "$name destinationDir = $destinationDir" }
        return args
    }

    internal fun addFriendPathForTestTask(friendKotlinTaskName: String, args: K2JVMCompilerArguments) {
        val friendTask = project.getTasksByName(friendKotlinTaskName, /* recursive = */false).firstOrNull() as? KotlinCompile ?: return
        args.friendPaths = arrayOf(friendTask.javaOutputDir!!.absolutePath)
        args.moduleName = friendTask.moduleName
        logger.kotlinDebug("java destination directory for production = ${friendTask.javaOutputDir}")
    }

    override fun getSourceRoots() = SourceRoots.ForJvm.create(getSource(), sourceRootsContainer)

    override fun callCompiler(args: K2JVMCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.ForJvm

        val messageCollector = GradleMessageCollector(logger)
        args.classpathAsList = compileClasspath.toList()
        args.destinationAsFile = destinationDir
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerJar = compilerJarFile
                ?: findKotlinJvmCompilerJar(project)
                ?: throw IllegalStateException("Could not find Kotlin Compiler jar. Please specify $name.compilerJarFile")

        if (!incremental) {
            anyClassesCompiled = true
            val compilerRunner = GradleCompilerRunner(project)
            val exitCode = compilerRunner.runJvmCompiler(sourceRoots.kotlinSourceFiles, sourceRoots.javaSourceRoots, args, messageCollector,
                    outputItemCollector, compilerJar)
            processCompilerExitCode(exitCode)
            return
        }

        logger.warn(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
        val reporter = GradleIncReporter(project.rootProject.projectDir)
        val compiler = IncrementalJvmCompilerRunner(
                taskBuildDirectory,
                sourceRoots.javaSourceRoots,
                cacheVersions,
                reporter,
                kaptAnnotationsFileUpdater,
                sourceAnnotationsRegistry,
                artifactDifferenceRegistryProvider,
                artifactFile
        )
        try {
            val exitCode = compiler.compile(sourceRoots.kotlinSourceFiles, args, messageCollector, { changedFiles })
            processCompilerExitCode(exitCode)
        }
        catch (e: Throwable) {
            cleanupOnError()
            throw e
        }
        anyClassesCompiled = compiler.anyClassesCompiled
    }

    private fun cleanupOnError() {
        logger.kotlinInfo("deleting $destinationDir on error")
        destinationDir.deleteRecursively()
    }

    private fun processCompilerExitCode(exitCode: ExitCode) {
        if (exitCode != ExitCode.OK) {
            cleanupOnError()
        }

        when (exitCode) {
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
            ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
            ExitCode.OK -> {
                logger.kotlinInfo("Compilation succeeded")
            }
        }
    }

    private fun handleKaptProperties() {
        kaptOptions.annotationsFile?.let { kaptAnnotationsFile ->
            if (incremental) {
                kaptAnnotationsFileUpdater = AnnotationFileUpdater(kaptAnnotationsFile)
            }

            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "output", kaptAnnotationsFile.canonicalPath)
        }

        if (kaptOptions.generateStubs) {
            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "stubs", destinationDir.canonicalPath)
        }

        if (kaptOptions.supportInheritedAnnotations) {
            pluginOptions.addPluginArgument(ANNOTATIONS_PLUGIN_NAME, "inherited", true.toString())
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

internal fun compileJvmNotIncrementally(
        compiler: K2JVMCompiler,
        logger: Logger,
        sourcesToCompile: List<File>,
        javaSourceRoots: Iterable<File>,
        compileClasspath: Iterable<File>,
        outputDir: File,
        args: K2JVMCompilerArguments
): ExitCode {
    logger.kotlinDebug("Removing all kotlin classes in $outputDir")
    // we're free to delete all classes since only we know about that directory
    // todo: can be optimized -- compile and remove only files that were not generated
    listClassFiles(outputDir.canonicalPath).forEach { it.delete() }

    val moduleFile = makeModuleFile(
            args.moduleName,
            isTest = false,
            outputDir = outputDir,
            sourcesToCompile = sourcesToCompile,
            javaSourceRoots = javaSourceRoots,
            classpath = compileClasspath,
            friendDirs = listOf())
    args.module = moduleFile.absolutePath
    val messageCollector = GradleMessageCollector(logger)

    try {
        logger.kotlinDebug("compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}")
        logger.kotlinDebug("compiling with classpath: ${compileClasspath.toList().sorted().joinToString()}")
        return compiler.exec(messageCollector, Services.EMPTY, args)
    }
    finally {
        moduleFile.delete()
    }
}

open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {
    override val compiler = K2JSCompiler()
    private val kotlinOptionsImpl = KotlinJsOptionsImpl()
    override val kotlinOptions: KotlinJsOptions
            get() = kotlinOptionsImpl

    private val defaultOutputFile: File
            get() = File(destinationDir, "$moduleName.js")

    @Suppress("unused")
    val outputFile: String?
        get() = kotlinOptions.outputFile

    init {
        @Suppress("LeakingThis")
        outputs.file(MethodClosure(this, "getOutputFile"))
    }

    override fun populateCompilerArguments(): K2JSCompilerArguments {
        val args = K2JSCompilerArguments().apply { fillDefaultValues() }
        val friendDependency = friendTaskName
                ?.let { project.getTasksByName(it, false).singleOrNull() as? Kotlin2JsCompile }
                ?.outputFile
                ?.let { File(it).parentFile }
                ?.let { if (LibraryUtils.isKotlinJavascriptLibrary(it)) it else null }
                ?.absolutePath

        val dependencies = project.configurations.getByName("compile")
                .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                .map { it.canonicalPath }

        args.libraryFiles = when (friendDependency) {
            null -> dependencies.toTypedArray()
            else -> (dependencies + friendDependency).toTypedArray()
        }

        kotlinOptionsImpl.updateArguments(args)
        return args
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource())

    override fun callCompiler(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        val messageCollector = GradleMessageCollector(logger)
        logger.debug("Calling compiler")
        destinationDir.mkdirs()
        args.freeArgs = args.freeArgs + sourceRoots.kotlinSourceFiles.map { it.absolutePath }

        if (args.outputFile == null) {
            throw GradleException("$name.kotlinOptions.outputFile should be specified.")
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        when (exitCode) {
            ExitCode.OK -> {
                logger.kotlinInfo("Compilation succeeded")
            }
            ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
            ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
            else -> throw GradleException("Unexpected exit code: $exitCode")
        }
    }
}

internal class GradleMessageCollector(val logger: Logger) : MessageCollector {
    private var hasErrors = false

    override fun hasErrors() = hasErrors

    override fun clear() {
        // Do nothing
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val text = with(StringBuilder()) {
            append(when (severity) {
                in CompilerMessageSeverity.VERBOSE -> "v"
                in CompilerMessageSeverity.ERRORS -> {
                    hasErrors = true
                    "e"
                }
                CompilerMessageSeverity.INFO -> "i"
                CompilerMessageSeverity.WARNING -> "w"
                else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
            })
            append(": ")

            val (path, line, column) = location
            if (path != null) {
                append(path)
                append(": ")
                if (line > 0 && column > 0) {
                    append("($line, $column): ")
                }
            }

            append(message)

            toString()
        }
        when (severity) {
            in CompilerMessageSeverity.VERBOSE -> logger.debug(text)
            in CompilerMessageSeverity.ERRORS -> logger.error(text)
            CompilerMessageSeverity.INFO -> logger.info(text)
            CompilerMessageSeverity.WARNING -> logger.warn(text)
            else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
        }
    }
}