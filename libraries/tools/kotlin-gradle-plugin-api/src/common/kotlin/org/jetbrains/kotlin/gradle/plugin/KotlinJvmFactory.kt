/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.Kapt
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Provides factory methods to create a custom Kotlin compilation pipeline for the JVM platform.
 */
interface KotlinJvmFactory {

    /**
     * Creates a new instance of [KaptExtensionConfig].
     *
     * Use an instance of this DSL object to configure kapt stub generation and annotation processing tasks.
     *
     * @since 1.7.0
     */
    val kaptExtension: KaptExtensionConfig

    /**
     * Creates a new instance of [KotlinTopLevelExtensionConfig].
     *
     * Use an instance of this DSL object to configure a Kotlin compilation pipeline.
     *
     * @since 1.7.0
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use API to create specific Kotlin extensions such as 'createKotlinJvmExtension()' or 'createKotlinAndroidExtension()'")
    val kotlinExtension: KotlinTopLevelExtensionConfig

    /**
     * Provides an instance of [ProviderFactory].
     *
     * @since 2.1.0
     */
    val providerFactory: ProviderFactory

    /**
     * Creates a new instance of [KotlinJvmExtension] that can be used to configure JVM compilation tasks.
     *
     * Note that wiring the extension configuration with tasks should be done manually.
     *
     * @since 2.1.0
     */
    fun createKotlinJvmExtension(): KotlinJvmExtension

    /**
     * Creates a new instance of [KotlinAndroidExtension] that can be used to configure Android compilation tasks.
     *
     * Note that wiring the extension configuration with tasks should be done manually.
     *
     * @since 2.1.0
     */
    fun createKotlinAndroidExtension(): KotlinAndroidExtension

    /**
     * Creates a new instance of [KotlinJvmOptionsDeprecated] that can be used to configure JVM or Android-specific compilations.
     *
     * Note: The [KotlinJvmCompilerOptions] instance inside [KotlinJvmOptionsDeprecated] is different from what is returned
     * by [createCompilerJvmOptions].
     *
     * @since 1.8.0
     */
    @Deprecated(
        message = "Replaced by compilerJvmOptions",
        replaceWith = ReplaceWith("createCompilerJvmOptions()")
    )
    fun createKotlinJvmOptions(): KotlinJvmOptionsDeprecated

    /**
     * Creates a new instance of [KotlinJvmCompilerOptions] that can be used to configure JVM or Android-specific compilations.
     *
     * @since 1.8.0
     */
    fun createCompilerJvmOptions(): KotlinJvmCompilerOptions

    /**
     * Registers a new standalone Kotlin compilation task with the given [taskName] for the JVM platform.
     *
     * @since 1.7.0
     */
    @Deprecated(
        message = "Replaced with 'registerKotlinJvmCompileTask(taskName, compilerOptions, explicitApiMode)'",
    )
    fun registerKotlinJvmCompileTask(taskName: String): TaskProvider<out KotlinJvmCompile>

    /**
     * Registers a new standalone Kotlin compilation task for the JVM platform.
     *
     * @param taskName The name of the task to be created.
     * @param moduleName The name of the module for which the task is being created. For more information, see [KotlinJvmCompilerOptions.moduleName].
     *
     * @since 1.9.20
     */
    @Deprecated(
        message = "Replaced with 'registerKotlinJvmCompileTask(taskName, compilerOptions, explicitApiMode)'",
    )
    fun registerKotlinJvmCompileTask(taskName: String, moduleName: String): TaskProvider<out KotlinJvmCompile>

    /**
     * Registers a new standalone Kotlin compilation task for the JVM platform.
     *
     * This task is not associated with any [KotlinTarget] or [KotlinCompilation].
     * It is not executed as part of the common compilation pipeline.
     *
     * Here's an example of how to register a new task:
     * ```kt
     * project.plugins.apply<KotlinApiPlugin>()
     * val kotlinApiPlugin = project.plugins.getPlugin(KotlinApiPlugin::class)
     * val kotlinJvmOptions = kotlinApiPlugin.createCompilerJvmOptions()
     * val kotlinJvmCompileTask = kotlinApiPlugin.registerKotlinJvmCompileTask(
     *     "customKotlinCompile",
     *     kotlinJvmOptions,
     *     project.providers.provider { ExplicitApiMode.Strict }
     * )
     * ```
     *
     * @param taskName the name of the task.
     * @param compilerOptions values of this [KotlinJvmCompilerOptions] instance that are used as convention values
     * for [KotlinJvmCompilerOptions]'s inside task.
     * @param explicitApiMode desired [ExplicitApiMode] mode in the task.
     * The [Provider] can have `null` value which is the same as specify [ExplicitApiMode.Disabled].
     * By default, the value is [ExplicitApiMode.Disabled].
     *
     * @since 2.1.0
     */
    fun registerKotlinJvmCompileTask(
        taskName: String,
        compilerOptions: KotlinJvmCompilerOptions = createCompilerJvmOptions(),
        explicitApiMode: Provider<ExplicitApiMode> = providerFactory.provider { ExplicitApiMode.Disabled },
    ): TaskProvider<out KotlinJvmCompile>

    /**
     * Registers a new kapt stub generation task with the given [taskName].
     *
     * This task creates Java source stubs from Kotlin sources.
     * It is designed to be used together with the [Kapt] task. Run this task before the [Kapt] task.
     *
     * @since 1.7.0
     */
    @Deprecated("Replaced with 'registerKaptGenerateStubsTask(taskName, compileTask, kaptExtension, explicitApiMode)'")
    fun registerKaptGenerateStubsTask(taskName: String): TaskProvider<out KaptGenerateStubs>

    /**
     * Registers a new kapt generation task with the given [taskName].
     *
     * This task creates Java source stubs from Kotlin sources.
     * It is designed to be used together with the [Kapt] task. Run this task before the [Kapt] task.
     *
     * @param taskName task name to set
     * @param compileTask related [KotlinJvmCompile] task that is part of the same compilation unit
     * @param kaptExtension an instance of [KaptExtensionConfig]
     * @param explicitApiMode [ExplicitApiMode] for this task
     *
     * @since 2.1.0
     */
    fun registerKaptGenerateStubsTask(
        taskName: String,
        compileTask: TaskProvider<out KotlinJvmCompile>,
        kaptExtension: KaptExtensionConfig,
        explicitApiMode: Provider<ExplicitApiMode> = providerFactory.provider { ExplicitApiMode.Disabled },
    ): TaskProvider<out KaptGenerateStubs>

    /**
     * Registers a new kapt task with the given [taskName].
     *
     * This task runs annotation processing.
     *
     * @since 1.7.0
     */
    @Deprecated("Replaced with 'registerKaptTask(taskName, kaptExtension)'")
    fun registerKaptTask(taskName: String): TaskProvider<out Kapt>

    /**
     * Registers a new kapt task with the given [taskName].
     *
     * This task runs annotation processing.
     *
     * @param taskName task name to set
     * @param kaptExtension an instance of [KaptExtensionConfig]
     *
     * @since 2.1.0
     */
    fun registerKaptTask(
        taskName: String,
        kaptExtension: KaptExtensionConfig,
    ): TaskProvider<out Kapt>

    /**
     * Adds a compiler plugin dependency to this project.
     *
     * This can be, for example, a Maven coordinate or a project already included in the build.
     *
     * @param dependency see the [org.gradle.api.artifacts.dsl.DependencyHandler] dependency notations description for possible values.
     *
     * @since 1.7.0
     */
    fun addCompilerPluginDependency(dependency: Provider<Any>)

    /**
     *  Returns a [FileCollection] that contains the complete compiler plugins classpath for this project.
     *
     *  @since 1.7.0
     */
    fun getCompilerPlugins(): FileCollection
}