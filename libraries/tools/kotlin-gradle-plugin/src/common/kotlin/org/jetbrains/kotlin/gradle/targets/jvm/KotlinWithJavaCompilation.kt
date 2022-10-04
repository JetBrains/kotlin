/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "UNCHECKED_CAST") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

open class KotlinWithJavaCompilation<KotlinOptionsType : KotlinCommonOptions, CO : CompilerCommonOptions> @Inject internal constructor(
    private val compilation: KotlinCompilationImpl,
    val javaSourceSet: SourceSet,
) : InternalKotlinCompilation<KotlinOptionsType> by compilation as InternalKotlinCompilation<KotlinOptionsType>,
    KotlinCompilationWithResources<KotlinOptionsType>,
    KotlinCompilationToRunnableFiles<KotlinOptionsType> {

    @Suppress("UNCHECKED_CAST")
    override val compilerOptions: HasCompilerOptions<CO> =
        compilation.compilerOptions as HasCompilerOptions<CO>

    @Suppress("UNCHECKED_CAST")
    override val kotlinOptions: KotlinOptionsType
        get() = compilation.kotlinOptions as KotlinOptionsType

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>
        get() = target.project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)

    override val runtimeDependencyConfigurationName: String
        get() = compilation.runtimeDependencyConfigurationName ?: error("Missing 'runtimeDependencyConfigurationName'")

    override var runtimeDependencyFiles: FileCollection = compilation.runtimeDependencyFiles ?: error("Missing 'runtimeDependencyFiles'")

    override val processResourcesTaskName: String
        get() = compilation.processResourcesTaskName ?: error("Missing 'processResourcesTaskName'")

    override val relatedConfigurationNames: List<String>
        get() = compilation.relatedConfigurationNames

    fun source(javaSourceSet: SourceSet) {
        with(target.project) {
            afterEvaluate {
                tasks.named<AbstractKotlinCompile<*>>(compileKotlinTaskName).configure {
                    it.setSource(javaSourceSet.java)
                }
            }
        }
    }
}
