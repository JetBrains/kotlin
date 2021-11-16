/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.filesProvider

class KotlinExternalTargetCompilationHandle internal constructor(
    val target: KotlinExternalTargetHandle,
    internal val compilation: KotlinJvmExternalCompilation,
    internal val compilationTask: TaskProvider<out KotlinCompile>
) {
    val project = target.project

    val defaultSourceSet = compilation.defaultSourceSet

    val classesDirs = target.project.filesProvider(compilationTask) {
        compilation.output.classesDirs
    }

    fun addCompileDependenciesFiles(files: FileCollection) {
        compilation.compileDependencyFiles = compilation.compileDependencyFiles + files
    }

    fun setCompileDependencyFilesConfiguration(configuration: Configuration) {
        compilation.compileDependencyFiles = configuration
        project.addExtendsFromRelation(configuration.name, compilation.compileDependencyConfigurationName)
    }

    fun setRuntimeDependencyFilesConfiguration(configuration: Configuration) {
        compilation.runtimeDependencyFiles = configuration
        project.addExtendsFromRelation(configuration.name, compilation.runtimeDependencyConfigurationName)
    }

    val runtimeOnlyConfiguration
        get() = project.configurations.getByName(compilation.runtimeOnlyConfigurationName)

    val implementationConfiguration
        get() = project.configurations.getByName(compilation.implementationConfigurationName)

    val apiConfiguration
        get() = project.configurations.getByName(compilation.apiConfigurationName)

    val compileDependencyConfiguration: Configuration?
        get() = project.configurations.findByName(compilation.compileDependencyConfigurationName)

    val runtimeDependencyConfiguration: Configuration?
        get() = project.configurations.findByName(compilation.runtimeDependencyConfigurationName)

}

