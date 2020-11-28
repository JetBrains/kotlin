 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

 class KotlinWithJavaCompilation<KotlinOptionsType : KotlinCommonOptions>(
    target: KotlinWithJavaTarget<KotlinOptionsType>,
    name: String,
    override val kotlinOptions: KotlinOptionsType
) : AbstractKotlinCompilationToRunnableFiles<KotlinOptionsType>(target, name), KotlinCompilationWithResources<KotlinOptionsType> {
    lateinit var javaSourceSet: SourceSet

    override val output: KotlinCompilationOutput by lazy { KotlinWithJavaCompilationOutput(this) }

    override val processResourcesTaskName: String
        get() = javaSourceSet.processResourcesTaskName

    override var runtimeDependencyFiles: FileCollection
        get() = javaSourceSet.runtimeClasspath
        set(value) {
            javaSourceSet.runtimeClasspath = value
        }

    override val runtimeDependencyConfigurationName: String
        get() = javaSourceSet.runtimeClasspathConfigurationName

    override val compileDependencyConfigurationName: String
        get() = javaSourceSet.compileClasspathConfigurationName

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

    override var compileDependencyFiles: FileCollection
        get() = javaSourceSet.compileClasspath
        set(value) {
            javaSourceSet.compileClasspath = value
        }

     override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
         if (name != SourceSet.TEST_SOURCE_SET_NAME || other.name != SourceSet.MAIN_SOURCE_SET_NAME) {
             super.addAssociateCompilationDependencies(other)
         } // otherwise, do nothing: the Java Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
     }

    fun source(javaSourceSet: SourceSet) {
        with(target.project) {
            afterEvaluate {
                (tasks.getByName(compileKotlinTaskName) as AbstractKotlinCompile<*>).source(javaSourceSet.java)
            }
        }
    }

    val compileJavaTaskProvider: TaskProvider<out JavaCompile>
        get() = target.project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)
}
