package org.jetbrains.kotlin.gradle.internal

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil.compareVersionNumbers
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.File

@CacheableTask
open class KaptTask : ConventionTask(), CompilerArgumentAwareWithInput<K2JVMCompilerArguments> {

    init {
        cacheOnlyIfEnabledForKotlin()

        if (isBuildCacheSupported()) {
            val reason = "Caching is disabled by default for kapt because of arbitrary behavior of external " +
                         "annotation processors. You can enable it by adding 'kapt.useBuildCache = true' to the build script."
            outputs.cacheIf(reason) { useBuildCache }
        }
    }

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:Internal
    internal lateinit var stubsDir: File

    private fun isInsideDestinationDirs(file: File): Boolean {
        return FileUtil.isAncestor(destinationDir, file, /* strict = */ false)
                || FileUtil.isAncestor(classesDir, file, /* strict = */ false)
    }

    @get:Classpath @get:InputFiles
    val kaptClasspath: FileCollection
        get() = project.files(*kaptClasspathConfigurations.toTypedArray())

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>

    @get:OutputDirectory
    internal lateinit var classesDir: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @get:OutputDirectory
    lateinit var kotlinSourcesDestinationDir: File

    override fun createCompilerArgs(): K2JVMCompilerArguments = K2JVMCompilerArguments()

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean) {
        kotlinCompileTask.setupCompilerArgs(args)

        args.pluginClasspaths = (pluginClasspath + args.pluginClasspaths!!).toSet().toTypedArray()

        val pluginOptionsWithKapt: CompilerPluginOptions = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true
    }

    @get:Classpath @get:InputFiles
    val classpath: FileCollection
        get() = kotlinCompileTask.classpath

    @get:Internal
    var useBuildCache: Boolean = false

    @get:Classpath @get:InputFiles @Suppress("unused")
    internal val kotlinTaskPluginClasspaths get() = kotlinCompileTask.pluginClasspath

    @get:Classpath @get:InputFiles
    internal val pluginClasspath get() = pluginOptions.classpath

    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: FileCollection
        get() {
            val sourcesFromKotlinTask = kotlinCompileTask.source
                    .filter { it.extension == "java" && !isInsideDestinationDirs(it) }

            val stubSources = project.fileTree(stubsDir)
            return sourcesFromKotlinTask + stubSources
        }

    @TaskAction
    fun compile() {
        /** Delete everything inside generated sources and classes output directory
         * (annotation processing is not incremental) */
        clearOutputDirectories()

        val sourceRootsFromKotlin = kotlinCompileTask.sourceRootsContainer.sourceRoots
        val rawSourceRoots = FilteringSourceRootsContainer(sourceRootsFromKotlin, { !isInsideDestinationDirs(it) })
        val sourceRoots = SourceRoots.ForJvm.create(kotlinCompileTask.source, rawSourceRoots)

        val args = prepareCompilerArguments()

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val environment = GradleCompilerEnvironment(kotlinCompileTask.computedCompilerClasspath, messageCollector, outputItemCollector, args)
        if (environment.toolsJar == null && !isAtLeastJava9) {
            throw GradleException("Could not find tools.jar in system classpath, which is required for kapt to work")
        }

        val compilerRunner = GradleCompilerRunner(project)
        val exitCode = compilerRunner.runJvmCompiler(
                sourceRoots.kotlinSourceFiles,
                sourceRoots.javaSourceRoots,
                kotlinCompileTask.javaPackagePrefix,
                args,
                environment)
        throwGradleExceptionIfError(exitCode)
    }

    private val isAtLeastJava9: Boolean
        get() = compareVersionNumbers(getJavaRuntimeVersion(), "9") >= 0

    private fun getJavaRuntimeVersion(): String {
        val rtVersion = System.getProperty("java.runtime.version")
        return if (Character.isDigit(rtVersion[0])) rtVersion else System.getProperty("java.version")
    }
}