/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.intellij.openapi.util.text.StringUtil
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.GradleMessageCollector
import org.jetbrains.kotlin.gradle.tasks.clearOutputDirectories
import org.jetbrains.kotlin.gradle.tasks.throwGradleExceptionIfError
import org.jetbrains.kotlin.gradle.utils.toSortedPathsArray

open class KaptWithKotlincTask : KaptTask(), CompilerArgumentAwareWithInput<K2JVMCompilerArguments> {
    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()

    @get:Classpath
    @get:InputFiles
    @Suppress("unused")
    internal val kotlinTaskPluginClasspaths get() = kotlinCompileTask.pluginClasspath

    @get:Classpath
    @get:InputFiles
    val pluginClasspath: FileCollection
        get() = project.configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)

    override fun createCompilerArgs(): K2JVMCompilerArguments = K2JVMCompilerArguments()

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean) {
        kotlinCompileTask.setupCompilerArgs(args)

        args.pluginClasspaths = pluginClasspath.toSortedPathsArray()

        val pluginOptionsWithKapt: CompilerPluginOptions = pluginOptions.withWrappedKaptOptions(withApClasspath = kaptClasspath)
        args.pluginOptions = (pluginOptionsWithKapt.arguments + args.pluginOptions!!).toTypedArray()

        args.verbose = project.hasProperty("kapt.verbose") && project.property("kapt.verbose").toString().toBoolean() == true
    }

    @TaskAction
    fun compile() {
        logger.debug("Running kapt annotation processing using the Kotlin compiler")

        /** Delete everything inside generated sources and classes output directory
         * (annotation processing is not incremental) */
        clearOutputDirectories()

        val args = prepareCompilerArguments()

        val messageCollector = GradleMessageCollector(logger)
        val outputItemCollector = OutputItemsCollectorImpl()
        val environment = GradleCompilerEnvironment(compilerClasspath, messageCollector, outputItemCollector, args)
        if (environment.toolsJar == null && !isAtLeastJava9) {
            throw GradleException("Could not find tools.jar in system classpath, which is required for kapt to work")
        }

        val compilerRunner = GradleCompilerRunner(project)
        val exitCode = compilerRunner.runJvmCompiler(
            sourcesToCompile = emptyList(),
            commonSources = emptyList(),
            javaSourceRoots = javaSourceRoots,
            javaPackagePrefix = kotlinCompileTask.javaPackagePrefix,
            args = args,
            environment = environment
        )
        throwGradleExceptionIfError(exitCode)
    }

    private val isAtLeastJava9: Boolean
        get() = StringUtil.compareVersionNumbers(getJavaRuntimeVersion(), "9") >= 0

    private fun getJavaRuntimeVersion(): String {
        val rtVersion = System.getProperty("java.runtime.version")
        return if (Character.isDigit(rtVersion[0])) rtVersion else System.getProperty("java.version")
    }
}
