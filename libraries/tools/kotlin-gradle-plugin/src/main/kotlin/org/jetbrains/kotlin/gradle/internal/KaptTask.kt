package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.File

open class KaptTask : AbstractCompile() {
    internal val pluginOptions = CompilerPluginOptions()
    internal lateinit var kotlinCompileTask: KotlinCompile

    fun isInsideDestinationDirs(file: File): Boolean {
        return FileUtil.isAncestor(destinationDir, file, /* strict = */ false)
                || FileUtil.isAncestor(classesDir, file, /* strict = */ false)
    }

    lateinit var classesDir: File
    lateinit var stubsDir: File

    @TaskAction
    override fun compile() {
        /** Delete everything inside generated sources and classes output directory
         * (annotation processing is not incremental) */
        destinationDir.clearDirectory()
        classesDir.clearDirectory()

        val sourceRootsFromKotlin = kotlinCompileTask.sourceRootsContainer.sourceRoots
        val rawSourceRoots = FilteringSourceRootsContainer(sourceRootsFromKotlin, { !isInsideDestinationDirs(it) })
        val sourceRoots = SourceRoots.ForJvm.create(kotlinCompileTask.source, rawSourceRoots)

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

    private fun File.clearDirectory() {
        deleteRecursively()
        mkdirs()
    }
}