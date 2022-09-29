/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import javax.inject.Inject

open class KotlinJvmCompilation @Inject internal constructor(
    private val compilation: KotlinCompilationImpl
) : InternalKotlinCompilation<KotlinJvmOptions> by compilation.castKotlinOptionsType(),
    KotlinCompilationWithResources<KotlinJvmOptions>,
    KotlinCompilationToRunnableFiles<KotlinJvmOptions> {

    override val target: KotlinJvmTarget = compilation.target as KotlinJvmTarget

    override val compilerOptions: HasCompilerOptions<CompilerJvmOptions> =
        compilation.compilerOptions.castCompilerOptionsType()

    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
        get() = compilation.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = compilation.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<CompilerJvmOptions>>
        get() = compilation.compileTaskProvider as TaskProvider<KotlinCompilationTask<CompilerJvmOptions>>

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>?
        get() = if (target.withJavaEnabled) {
            val project = target.project
            val javaSourceSets = project.gradle.variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSets
            val javaSourceSet = javaSourceSets.getByName(compilationName)
            project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)
        } else null

    override val runtimeDependencyConfigurationName: String
        get() = compilation.runtimeDependencyConfigurationName ?: error("Missing 'runtimeDependencyConfigurationName'")

    override var runtimeDependencyFiles: FileCollection = compilation.runtimeDependencyFiles ?: error("Missing 'runtimeDependencyFiles'")

    override val processResourcesTaskName: String
        get() = compilation.processResourcesTaskName ?: error("Missing 'processResourcesTaskName'")

    override val relatedConfigurationNames: List<String>
        get() = compilation.relatedConfigurationNames
}

//TODO SEB: Find a good spot for this function
internal inline fun <reified T : KotlinCommonOptions> InternalKotlinCompilation<*>.castKotlinOptionsType(): InternalKotlinCompilation<T> {
    this.kotlinOptions as T
    @Suppress("UNCHECKED_CAST")
    return this as InternalKotlinCompilation<T>
}

//TODO SEB: Find home for function
internal inline fun <reified T : CompilerCommonOptions> HasCompilerOptions<*>.castCompilerOptionsType(): HasCompilerOptions<T> {
    this.options as T
    @Suppress("UNCHECKED_CAST")
    return this as HasCompilerOptions<T>
}
