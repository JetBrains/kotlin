/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.toSortedPathsArray
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile

internal interface CompilerArgumentsContributor<in T : CommonToolArguments> {
    fun contributeArguments(
        args: T,
        flags: Collection<CompilerArgumentsConfigurationFlag>
    )
}

internal interface CompilerArgumentsConfigurationFlag

internal object DefaultsOnly : CompilerArgumentsConfigurationFlag
internal object IgnoreClasspathResolutionErrors : CompilerArgumentsConfigurationFlag

internal fun compilerArgumentsConfigurationFlags(defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) =
    mutableSetOf<CompilerArgumentsConfigurationFlag>().apply {
        if (defaultsOnly) add(DefaultsOnly)
        if (ignoreClasspathResolutionErrors) add(IgnoreClasspathResolutionErrors)
    }

/** The primary purpose of this class is to encapsulate compiler arguments setup done by the AbstractKotlinCompiler tasks,
 * but outside the tasks, so that this state & logic can be reused without referencing the task directly. */
internal open class AbstractKotlinCompileArgumentsContributor<T : CommonCompilerArguments>(
    // Don't save this reference into a property! That would be hostile to Gradle instant execution
    taskProvider: TaskProvider<out AbstractKotlinCompile<T>>
) : CompilerArgumentsContributor<T> {
    private val coroutines by taskProvider.map { it.coroutines }

    protected val logger by taskProvider.map { it.logger }

    private val isMultiplatform = taskProvider.map { it.isMultiplatform }.get()

    private val pluginClasspath by taskProvider.map { it.pluginClasspath }
    private val pluginOptions by taskProvider.map { it.pluginOptions }

    override fun contributeArguments(
        args: T,
        flags: Collection<CompilerArgumentsConfigurationFlag>
    ) {
        args.coroutinesState = when (coroutines.get()) {
            Coroutines.ENABLE -> CommonCompilerArguments.ENABLE
            Coroutines.WARN -> CommonCompilerArguments.WARN
            Coroutines.ERROR -> CommonCompilerArguments.ERROR
            Coroutines.DEFAULT -> CommonCompilerArguments.DEFAULT
        }

        logger.kotlinDebug { "args.coroutinesState=${args.coroutinesState}" }

        if (logger.isDebugEnabled) {
            args.verbose = true
        }

        args.multiPlatform = isMultiplatform

        setupPlugins(args)
    }

    internal fun setupPlugins(compilerArgs: T) {
        compilerArgs.pluginClasspaths = pluginClasspath.toSortedPathsArray()
        compilerArgs.pluginOptions = pluginOptions.arguments.toTypedArray()
    }
}

internal open class KotlinJvmCompilerArgumentsContributor(
    // Don't save this reference into a property! That would be hostile to Gradle instant execution. Only map it to the task properties.
    taskProvider: TaskProvider<out KotlinCompile>
) : AbstractKotlinCompileArgumentsContributor<K2JVMCompilerArguments>(taskProvider) {

    private val moduleName by taskProvider.map { it.moduleName }

    //TODO
    //objects.fileCollection
    // .from(taskData.compilation.output.classesDir, taskData.compilation.friendArtifacts)
    // .elements
    // .map { it.asFile.canonicalPath }
    private val friendPaths by taskProvider.map { it.friendPaths }

    private val compileClasspath by taskProvider.map { it.compileClasspath }

    private val destinationDir by taskProvider.map { it.destinationDir }

    private val kotlinOptions by taskProvider.map {
        listOfNotNull(
            it.parentKotlinOptionsImpl as KotlinJvmOptionsImpl?,
            it.kotlinOptions as KotlinJvmOptionsImpl
        )
    }

    override fun contributeArguments(
        args: K2JVMCompilerArguments,
        flags: Collection<CompilerArgumentsConfigurationFlag>
    ) {
        args.fillDefaultValues()

        super.contributeArguments(args, flags)

        args.moduleName = moduleName
        logger.kotlinDebug { "args.moduleName = ${args.moduleName}" }

        args.friendPaths = friendPaths
        logger.kotlinDebug { "args.friendPaths = ${args.friendPaths?.joinToString() ?: "[]"}" }

        if (DefaultsOnly in flags) return

        args.allowNoSourceFiles = true
        args.classpathAsList = try {
            compileClasspath.toList().filter { it.exists() }
        } catch (e: Exception) {
            if (IgnoreClasspathResolutionErrors in flags) emptyList() else throw(e)
        }
        args.destinationAsFile = destinationDir

        kotlinOptions.forEach { it.updateArguments(args) }
    }
}