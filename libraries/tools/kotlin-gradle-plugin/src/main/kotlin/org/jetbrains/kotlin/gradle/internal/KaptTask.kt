package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.File

open class KaptTask : AbstractCompile() {
    private val rawSourceRoots = FilteringSourceRootsContainer({ !it.isInsideDestinationDir() })
    private val args = K2JVMCompilerArguments().apply { fillDefaultValues() }

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

    @TaskAction
    override fun compile() {
        /** Delete everything inside the [destinationDir] */
        destinationDir.deleteRecursively()
        destinationDir.mkdirs()

        val compiler = K2JVMCompiler()
        val sourceRoots = SourceRoots.ForJvm.create(getSource(), rawSourceRoots)
        val compileClasspath = classpath.filter(File::exists)

        args.moduleName = kotlinCompileTask.moduleName
        args.pluginClasspaths = pluginOptions.classpath.toTypedArray()
        args.pluginOptions = pluginOptions.arguments.toTypedArray()
        kotlinCompileTask.friendTaskName?.let { kotlinCompileTask.addFriendPathForTestTask(it, args) }
        kotlinCompileTask.parentKotlinOptionsImpl?.updateArguments(args)
        KotlinJvmOptionsImpl().updateArguments(args)

        val exitCode = compileJvmNotIncrementally(compiler, logger,
                sourceRoots.kotlinSourceFiles, sourceRoots.javaSourceRoots, compileClasspath,
                destinationDir, args)
        throwGradleExceptionIfError(exitCode)
    }
}