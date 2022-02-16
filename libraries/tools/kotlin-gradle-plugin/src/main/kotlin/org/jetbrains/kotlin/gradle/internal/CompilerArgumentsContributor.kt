/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileArgumentsProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompilerArgumentsProvider
import org.jetbrains.kotlin.gradle.utils.toPathsArray
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
    taskProvider: KotlinCompileArgumentsProvider<out AbstractKotlinCompile<T>>
) : CompilerArgumentsContributor<T> {

    private val coroutines = taskProvider.coroutines
    protected val logger = taskProvider.logger
    private val isMultiplatform = taskProvider.isMultiplatform
    private val pluginClasspath = taskProvider.pluginClasspath
    private val pluginOptions = taskProvider.pluginOptions

    override fun contributeArguments(
        args: T,
        flags: Collection<CompilerArgumentsConfigurationFlag>
    ) {
        if (logger.isDebugEnabled) {
            args.verbose = true
        }

        args.multiPlatform = isMultiplatform

        setupPlugins(args)
    }

    internal fun setupPlugins(compilerArgs: T) {
        compilerArgs.pluginClasspaths = pluginClasspath.toPathsArray()
        compilerArgs.pluginOptions = pluginOptions.arguments.toTypedArray()
    }
}

internal open class KotlinJvmCompilerArgumentsContributor(
    // Don't save this reference into a property! That would be hostile to Gradle instant execution. Only map it to the task properties.
    taskProvider: KotlinJvmCompilerArgumentsProvider
) : AbstractKotlinCompileArgumentsContributor<K2JVMCompilerArguments>(taskProvider) {

    private val moduleName = taskProvider.moduleName
    private val friendPaths = taskProvider.friendPaths
    private val compileClasspath = taskProvider.compileClasspath
    private val destinationDir = taskProvider.destinationDir
    private val kotlinOptions = taskProvider.kotlinOptions

    override fun contributeArguments(
        args: K2JVMCompilerArguments,
        flags: Collection<CompilerArgumentsConfigurationFlag>
    ) {
        args.fillDefaultValues()

        super.contributeArguments(args, flags)

        args.moduleName = moduleName
        logger.kotlinDebug { "args.moduleName = ${args.moduleName}" }

        args.friendPaths = friendPaths.files.map { it.absolutePath }.toTypedArray()
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
