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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.annotation.AnnotationFileUpdaterImpl
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
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
import java.rmi.Remote
import java.util.*
import kotlin.properties.Delegates

const val ANNOTATIONS_PLUGIN_NAME = "org.jetbrains.kotlin.kapt"
const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_EXPERIMENTAL_INCREMENTAL_MESSAGE = "Using experimental kotlin incremental compilation"

abstract class AbstractKotlinCompile<T : CommonCompilerArguments>() : AbstractCompile(), CompilerArgumentAware {
    abstract protected fun populateCompilerArguments(): T

    override val serializedCompilerArguments: List<String>
        get() {
            val arguments = populateCompilerArguments()
            arguments.setupCommonCompilerArgs()
            return ArgumentUtils.convertArgumentsToStringList(arguments)
        }

    private val kotlinExt: KotlinProjectExtension
            get() = project.extensions.findByType(KotlinProjectExtension::class.java)!!

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

    internal var coroutinesFromGradleProperties: Coroutines? = null
    // Input is needed to force rebuild even if source files are not changed
    @get:Input
    internal val coroutinesStr: String
            get() = coroutines.name

    private val coroutines: Coroutines
        get() = kotlinExt.experimental.coroutines
                ?: coroutinesFromGradleProperties
                ?: Coroutines.DEFAULT

    var compilerJarFile: File? = null
    internal val compilerJar: File
            get() = compilerJarFile
                    ?: findKotlinCompilerJar(project)
                    ?: throw IllegalStateException("Could not find Kotlin Compiler jar. Please specify $name.compilerJarFile")
    protected abstract fun findKotlinCompilerJar(project: Project): File?

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
        args.setupCommonCompilerArgs()

        compilerCalled = true
        callCompiler(args, sourceRoots, ChangedFiles(inputs))
    }

    internal abstract fun getSourceRoots(): SourceRoots
    internal abstract fun callCompiler(args: T, sourceRoots: SourceRoots, changedFiles: ChangedFiles)

    private fun CommonCompilerArguments.setupCommonCompilerArgs() {
        coroutines.let {
            coroutinesEnable = it == Coroutines.ENABLE
            coroutinesWarn =   it == Coroutines.WARN
            coroutinesError =  it == Coroutines.ERROR
        }

        if (project.logger.isDebugEnabled) {
            verbose = true
        }
    }
}

open class KotlinCompile : AbstractKotlinCompile<K2JVMCompilerArguments>(), KotlinJvmCompile {
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

    override fun findKotlinCompilerJar(project: Project): File? =
            findKotlinJvmCompilerJar(project)

    override fun populateCompilerArguments(): K2JVMCompilerArguments {
        val args = K2JVMCompilerArguments().apply { fillDefaultValues() }

        handleKaptProperties()
        args.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        args.pluginOptions = pluginOptions.arguments.toTypedArray()
        args.moduleName = moduleName
        args.addCompilerBuiltIns = true

        val gradleVersion = getGradleVersion()
        if (gradleVersion == null || gradleVersion >= ParsedGradleVersion(3, 2)) {
            args.loadBuiltInsFromDependencies = true
        }

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
        val compilerRunner = GradleCompilerRunner(project)
        val reporter = GradleICReporter(project.rootProject.projectDir)

        val environment = when {
            !incremental -> GradleCompilerEnvironment(compilerJar, messageCollector, outputItemCollector, args)
            else -> {
                logger.warn(USING_EXPERIMENTAL_INCREMENTAL_MESSAGE)
                GradleIncrementalCompilerEnvironment(compilerJar, changedFiles, reporter, taskBuildDirectory,
                        messageCollector, outputItemCollector, kaptAnnotationsFileUpdater,
                        artifactDifferenceRegistryProvider,
                        artifactFile, args)
            }
        }

        if (!incremental) {
            logger.kotlinDebug { "Removing all kotlin classes in $destinationDir" }
            destinationDir.deleteRecursively()
            destinationDir.mkdirs()
        }

        try {
            val exitCode = compilerRunner.runJvmCompiler(sourceRoots.kotlinSourceFiles, sourceRoots.javaSourceRoots, args, environment)
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

    private fun handleKaptProperties() {
        kaptOptions.annotationsFile?.let { kaptAnnotationsFile ->
            if (incremental) {
                kaptAnnotationsFileUpdater = AnnotationFileUpdaterImpl(kaptAnnotationsFile)
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

open class Kotlin2JsCompile() : AbstractKotlinCompile<K2JSCompilerArguments>(), KotlinJsCompile {
    private val kotlinOptionsImpl = KotlinJsOptionsImpl()
    override val kotlinOptions: KotlinJsOptions
            get() = kotlinOptionsImpl

    private val defaultOutputFile: File
            get() = File(destinationDir, "$moduleName.js")

    @Suppress("unused")
    val outputFile: String
        get() = kotlinOptions.outputFile ?: defaultOutputFile.canonicalPath

    init {
        @Suppress("LeakingThis")
        outputs.file(MethodClosure(this, "getOutputFile"))
    }

    override fun findKotlinCompilerJar(project: Project): File? =
            findKotlinJsCompilerJar(project)

    override fun populateCompilerArguments(): K2JSCompilerArguments {
        val args = K2JSCompilerArguments().apply { fillDefaultValues() }
        args.outputFile = outputFile

        val friendDependency = friendTaskName
                ?.let { project.getTasksByName(it, false).singleOrNull() as? Kotlin2JsCompile }
                ?.outputFile
                ?.let { File(it).parentFile }
                ?.let { if (LibraryUtils.isKotlinJavascriptLibrary(it)) it else null }
                ?.absolutePath

        val dependencies = project.configurations.getByName("compile")
                .filter { LibraryUtils.isKotlinJavascriptLibrary(it) }
                .map { it.canonicalPath }

        args.libraries = (dependencies + listOfNotNull(friendDependency)).let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        kotlinOptionsImpl.updateArguments(args)
        return args
    }

    override fun getSourceRoots() = SourceRoots.KotlinOnly.create(getSource())

    override fun callCompiler(args: K2JSCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        sourceRoots as SourceRoots.KotlinOnly

        logger.debug("Calling compiler")
        destinationDir.mkdirs()
        args.freeArgs = args.freeArgs + sourceRoots.kotlinSourceFiles.map { it.absolutePath }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()

        val compilerRunner = GradleCompilerRunner(project)
        val environment = GradleCompilerEnvironment(compilerJar, messageCollector, outputItemCollector, args)
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

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
        val text = with(StringBuilder()) {
            append(when (severity) {
                in CompilerMessageSeverity.VERBOSE -> "v"
                in CompilerMessageSeverity.ERRORS -> {
                    hasErrors = true
                    "e"
                }
                CompilerMessageSeverity.INFO -> "i"
                CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> "w"
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
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> logger.warn(text)
            else -> throw IllegalArgumentException("Unknown CompilerMessageSeverity: $severity")
        }
    }
}
