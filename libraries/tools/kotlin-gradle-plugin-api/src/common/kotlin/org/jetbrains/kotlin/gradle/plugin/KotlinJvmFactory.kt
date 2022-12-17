/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsDeprecated
import org.jetbrains.kotlin.gradle.tasks.*

/** An API used by third-party plugins to integration with the Kotlin Gradle plugin. */
interface KotlinJvmFactory : KotlinFactory {
    /** Instance of DSL object that should be used to configure KAPT stub generation and annotation processing tasks.*/
    val kaptExtension: KaptExtensionConfig

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

    /** Creates a Kotlin compile task. */
    fun registerKotlinJvmCompileTask(taskName: String): TaskProvider<out KotlinJvmCompile>

    /** Creates a Kotlin compile task that accept java-only inputs. */
    fun registerKotlinJvmCompileWithJavaTask(taskName: String): TaskProvider<out KotlinJvmCompile>

    /** Creates a stub generation task which creates Java sources stubs from Kotlin sources. */
    fun registerKaptGenerateStubsTask(taskName: String): TaskProvider<out KaptGenerateStubs>

    /** Creates a KAPT task which runs annotation processing. */
    fun registerKaptTask(taskName: String): TaskProvider<out Kapt>
}