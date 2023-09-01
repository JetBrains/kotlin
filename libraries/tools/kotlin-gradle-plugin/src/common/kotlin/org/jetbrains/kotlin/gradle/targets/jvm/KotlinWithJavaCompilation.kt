/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "UNCHECKED_CAST") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

open class KotlinWithJavaCompilation<KotlinOptionsType : KotlinCommonOptions, CO : KotlinCommonCompilerOptions> @Inject internal constructor(
    compilation: KotlinCompilationImpl,
    val javaSourceSet: SourceSet,
) : AbstractKotlinCompilationToRunnableFiles<KotlinOptionsType>(compilation),
    KotlinCompilationWithResources<KotlinOptionsType> {

    @Suppress("UNCHECKED_CAST")
    override val compilerOptions: HasCompilerOptions<CO> =
        compilation.compilerOptions as HasCompilerOptions<CO>

    fun compilerOptions(configure: CO.() -> Unit) {
        compilerOptions.configure(configure)
    }

    fun compilerOptions(configure: Action<CO>) {
        configure.execute(compilerOptions.options)
    }

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>
        get() = target.project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)

    override val processResourcesTaskName: String
        get() = compilation.processResourcesTaskName ?: error("Missing 'processResourcesTaskName'")

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
