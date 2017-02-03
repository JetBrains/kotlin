package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset

open class KaptTask : AbstractCompile() {
    private val rawSourceRoots = FilteringSourceRootsContainer({ !it.isInsideDestinationDir() })

    internal val pluginOptions = CompilerPluginOptions()
    internal lateinit var kotlinCompileTask: KotlinCompile

    override fun setSource(sources: Any?) {
        val filteredSources = rawSourceRoots.set(sources)
        super.setSource(filteredSources)
    }

    override fun source(vararg sources: Any?): SourceTask? {
        val filteredSources = rawSourceRoots.add(*sources)
        return super.source(filteredSources)
    }

    private fun File.isInsideDestinationDir(): Boolean {
        return FileUtil.isAncestor(destinationDir, this, /* strict = */ false)
    }

    lateinit var classesDir: File

    @TaskAction
    override fun compile() {
        /** Delete everything inside the [destinationDir] */
        destinationDir.deleteRecursively()
        destinationDir.mkdirs()

        classesDir.deleteRecursively()
        classesDir.mkdirs()

        val sourceRoots = SourceRoots.ForJvm.create(getSource(), rawSourceRoots)

        val args = K2JVMCompilerArguments()
        kotlinCompileTask.setupCompilerArgs(args)

        args.pluginClasspaths = (pluginOptions.classpath + args.pluginClasspaths).toSet().toTypedArray()
        args.pluginOptions = (pluginOptions.arguments + args.pluginOptions).toTypedArray()
        args.verbose = project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val environment = GradleCompilerEnvironment(kotlinCompileTask.compilerJar, messageCollector, outputItemCollector, args)
        if (environment.toolsJar == null) {
            throw GradleException("Could not find tools.jar in system classpath, which is required for kapt to work")
        }

        val compilerRunner = GradleCompilerRunner(project)
        val exitCode = compilerRunner.runJvmCompiler(sourceRoots.kotlinSourceFiles, sourceRoots.javaSourceRoots, args, environment)
        throwGradleExceptionIfError(exitCode)
    }
}