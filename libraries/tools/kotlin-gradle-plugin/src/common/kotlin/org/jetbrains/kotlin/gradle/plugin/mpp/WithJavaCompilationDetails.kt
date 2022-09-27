/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder

internal class WithJavaCompilationDetails<T : KotlinCommonOptions, CO : CompilerCommonOptions>(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    createCompilerOptions: DefaultCompilationDetails<T, CO>.() -> HasCompilerOptions<CO>,
    createKotlinOptions: DefaultCompilationDetails<T, CO>.() -> T
) : DefaultCompilationDetailsWithRuntime<T, CO>(target, compilationPurpose, defaultSourceSet, createCompilerOptions, createKotlinOptions) {
    @Suppress("UNCHECKED_CAST")
    override val compilation: KotlinWithJavaCompilation<T, CO>
        get() = super.compilation as KotlinWithJavaCompilation<T, CO>

    val javaSourceSet: SourceSet
        get() = compilation.javaSourceSet

    override val output: KotlinCompilationOutput by lazy { KotlinWithJavaCompilationOutput(compilation) }

    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = object : GradleKpmDependencyFilesHolder {
            override val dependencyConfigurationName: String by javaSourceSet::compileClasspathConfigurationName
            override var dependencyFiles: FileCollection by javaSourceSet::compileClasspath
        }

    override val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
        get() = object : GradleKpmDependencyFilesHolder {
            override val dependencyConfigurationName: String by javaSourceSet::runtimeClasspathConfigurationName
            override var dependencyFiles: FileCollection by javaSourceSet::runtimeClasspath
        }

    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        if (compilationPurpose != SourceSet.TEST_SOURCE_SET_NAME || other.name != SourceSet.MAIN_SOURCE_SET_NAME) {
            super.addAssociateCompilationDependencies(other)
        } // otherwise, do nothing: the Java Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
    }
}