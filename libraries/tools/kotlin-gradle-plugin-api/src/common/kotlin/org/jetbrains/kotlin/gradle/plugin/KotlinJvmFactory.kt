/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsDeprecated
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig
import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.Kapt
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** An API used by third-party plugins to integration with the Kotlin Gradle plugin. */
interface KotlinJvmFactory {
    /** Instance of DSL object that should be used to configure KAPT stub generation and annotation processing tasks.*/
    val kaptExtension: KaptExtensionConfig

    /** Instance of DSL object that should be used to configure Kotlin compilation pipeline. */
    val kotlinExtension: KotlinTopLevelExtensionConfig

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
        replaceWith = ReplaceWith("registerKotlinJvmCompileTask(taskName: String, moduleName: String)")
    )
    fun registerKotlinJvmCompileTask(taskName: String): TaskProvider<out KotlinJvmCompile>


    /**
     * Creates a Kotlin JVM compile task.
     *
     * @param taskName The name of the task to be created.
     * @param moduleName The name of the module for which the task is being created.
     * @return The task provider for the Kotlin JVM compile task.
     */
    fun registerKotlinJvmCompileTask(taskName: String, moduleName: String): TaskProvider<out KotlinJvmCompile>

    /** Creates a stub generation task which creates Java sources stubs from Kotlin sources. */
    fun registerKaptGenerateStubsTask(taskName: String): TaskProvider<out KaptGenerateStubs>

    /** Creates a KAPT task which runs annotation processing. */
    fun registerKaptTask(taskName: String): TaskProvider<out Kapt>

    /** Adds a compiler plugin dependency to this project. This can be e.g a Maven coordinate or a project included in the build. */
    fun addCompilerPluginDependency(dependency: Provider<Any>)

    /** Returns a [FileCollection] that contains all compiler plugins classpath for this project. */
    fun getCompilerPlugins(): FileCollection
}