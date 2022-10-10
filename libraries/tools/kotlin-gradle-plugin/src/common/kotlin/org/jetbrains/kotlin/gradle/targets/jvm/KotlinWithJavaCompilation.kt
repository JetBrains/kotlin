/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

abstract class KotlinWithJavaCompilation<KotlinOptionsType : KotlinCommonOptions, CO : KotlinCommonCompilerOptions> @Inject constructor(
    target: KotlinWithJavaTarget<KotlinOptionsType, CO>,
    name: String,
    override val defaultSourceSet: KotlinSourceSet,
    compilerOptions: HasCompilerOptions<CO>,
    kotlinOptions: KotlinOptionsType
) : AbstractKotlinCompilationToRunnableFiles<KotlinOptionsType>(
    WithJavaCompilationDetails<KotlinOptionsType, CO>(target, name, defaultSourceSet, { compilerOptions }, { kotlinOptions })
), KotlinCompilationWithResources<KotlinOptionsType> {
    lateinit var javaSourceSet: SourceSet

    override val processResourcesTaskName: String
        get() = javaSourceSet.processResourcesTaskName

    override val runtimeOnlyConfigurationName: String
        get() = javaSourceSet.runtimeOnlyConfigurationName

    override val implementationConfigurationName: String
        get() = javaSourceSet.implementationConfigurationName

    override val apiConfigurationName: String
        get() = javaSourceSet.apiConfigurationName

    override val compileOnlyConfigurationName: String
        get() = javaSourceSet.compileOnlyConfigurationName

    override val compileAllTaskName: String
        get() = javaSourceSet.classesTaskName

    fun source(javaSourceSet: SourceSet) {
        with(target.project) {
            afterEvaluate {
                tasks.named<AbstractKotlinCompile<*>>(compileKotlinTaskName).configure {
                    it.setSource(javaSourceSet.java)
                }
            }
        }
    }

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>
        get() = target.project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)
}
