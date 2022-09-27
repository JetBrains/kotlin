/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.GradleKpmDependencyFilesHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.newDependencyFilesHolder
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

open class NativeCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    @Suppress("DEPRECATION")
    createCompilerOptions: DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>.() -> HasCompilerOptions<CompilerCommonOptions>,
    @Suppress("DEPRECATION")
    createKotlinOptions: DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>.() -> KotlinCommonOptions
) : DefaultCompilationDetails<KotlinCommonOptions, CompilerCommonOptions>(
    target,
    compilationPurpose,
    defaultSourceSet,
    createCompilerOptions,
    createKotlinOptions
) {
    override val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder = project.newDependencyFilesHolder(
        lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }.orEmpty(),
            "compileKlibraries"
        )
    )

    override val compileAllTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilationPurpose, "klibrary")

    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        compileDependencyFilesHolder.dependencyFiles +=
            other.output.classesDirs + project.filesProvider { other.compileDependencyFiles }

        target.project.configurations.named(compilation.implementationConfigurationName).configure { configuration ->
            configuration.extendsFrom(target.project.configurations.findByName(other.implementationConfigurationName))
        }
    }

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        addSourcesToKotlinNativeCompileTask(project, compileKotlinTaskName, { sourceSet.kotlin }, addAsCommonSources)
    }
}