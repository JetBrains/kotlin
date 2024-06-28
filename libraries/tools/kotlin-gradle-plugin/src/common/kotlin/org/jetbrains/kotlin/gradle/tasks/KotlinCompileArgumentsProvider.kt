/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import java.io.File

open class KotlinCompileArgumentsProvider<T : AbstractKotlinCompile<out CommonCompilerArguments>>(taskProvider: T) {
    val logger: Logger = taskProvider.logger
    val isMultiplatform: Boolean = taskProvider.multiPlatformEnabled.get()
    private val pluginData = taskProvider.kotlinPluginData?.orNull
    val pluginClasspath: FileCollection = listOfNotNull(taskProvider.pluginClasspath, pluginData?.classpath).reduce(FileCollection::plus)
    val pluginOptions: CompilerPluginOptions = taskProvider.pluginOptions.toSingleCompilerPluginOptions() + pluginData?.options
}

class KotlinJvmCompilerArgumentsProvider
    (taskProvider: KotlinCompile) : KotlinCompileArgumentsProvider<KotlinCompile>(taskProvider) {
    val taskName: String = taskProvider.name
    val friendPaths: FileCollection = taskProvider.friendPaths
    val compileClasspath: Iterable<File> = taskProvider.libraries
    val destinationDir: File = taskProvider.destinationDirectory.get().asFile
    @Suppress("DEPRECATION")
    val taskModuleName: String? = taskProvider.moduleName.orNull
    val nagTaskModuleNameUsage: Boolean = taskProvider.nagTaskModuleNameUsage.get()
    internal val compilerOptions: KotlinJvmCompilerOptions = taskProvider.compilerOptions
}
