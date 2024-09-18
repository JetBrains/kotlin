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
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsDeprecated
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.Kapt
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * @suppress TODO: KT-58858 add documentation
 * An API used by third-party plugins to integrate with the Kotlin Gradle plugin.
 */
interface KotlinJvmFactory {
    /** Instance of DSL object that should be used to configure KAPT stub generation and annotation processing tasks.*/
    val kaptExtension: KaptExtensionConfig

    /** Instance of DSL object that should be used to configure Kotlin compilation pipeline. */
    val kotlinExtension: KotlinTopLevelExtensionConfig

    val providerFactory: ProviderFactory

    /**
     * Creates instance of DSL object that should be used to configure JVM/android specific compilation.
     *
     * Note: [CompilerJvmOptions] instance inside [KotlinJvmOptions] is not the same as returned by [createCompilerJvmOptions]
     */
    @Deprecated(
        message = "Replaced by compilerJvmOptions",
        replaceWith = ReplaceWith("createCompilerJvmOptions()")
    )
    fun createKotlinJvmOptions(): KotlinJvmOptionsDeprecated

    fun createCompilerJvmOptions(): KotlinJvmCompilerOptions

    /**
     * Creates a Kotlin compile task.
     */
    @Deprecated(
        message = "Replaced by registerKotlinJvmCompileTask with module name",
        replaceWith = ReplaceWith("registerKotlinJvmCompileTask(taskName, TODO(), TODO())")
    )
    fun registerKotlinJvmCompileTask(taskName: String): TaskProvider<out KotlinJvmCompile>

    /**
     * Creates a Kotlin JVM compile task.
     *
     * @param taskName The name of the task to be created.
     * @param moduleName The name of the module for which the task is being created.
     * @return The task provider for the Kotlin JVM compile task.
     */
    @Deprecated(
        message = "Replaced by registerKotlinJvmCompileTask with compiler options and explicit API mode",
        replaceWith = ReplaceWith("registerKotlinJvmCompileTask(taskName, TODO(), TODO())")
    )
    fun registerKotlinJvmCompileTask(taskName: String, moduleName: String): TaskProvider<out KotlinJvmCompile>

    /**
     * Registers a new standalone Kotlin compilation task for JVM platform.
     *
     * This task will not be associated with any [KotlinTarget] or [KotlinCompilation] and executed as part of common
     * compilation pipeline.
     *
     * Example how to register a new task:
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
     * @param taskName the name of the task
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

    /** Creates a stub generation task which creates Java sources stubs from Kotlin sources. */
    fun registerKaptGenerateStubsTask(taskName: String): TaskProvider<out KaptGenerateStubs>

    /** Creates a KAPT task which runs annotation processing. */
    fun registerKaptTask(taskName: String): TaskProvider<out Kapt>

    /** Adds a compiler plugin dependency to this project. This can be e.g a Maven coordinate or a project included in the build. */
    fun addCompilerPluginDependency(dependency: Provider<Any>)

    /** Returns a [FileCollection] that contains all compiler plugins classpath for this project. */
    fun getCompilerPlugins(): FileCollection
}