/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.project.model.LanguageSettings
import java.util.*

abstract class AbstractKotlinCompilation<T : KotlinCommonOptions>(
    internal open val compilationDetails: CompilationDetails<T>,
) : InternalKotlinCompilation<T>,
    HasKotlinDependencies,
    KotlinCompilationData<T> {

    //region HasKotlinDependencies delegation: delegate members manually for better control and prevention of accidental overrides
    private val kotlinDependenciesHolder get() = compilationDetails.kotlinDependenciesHolder
    override val apiConfigurationName: String get() = kotlinDependenciesHolder.apiConfigurationName
    override val implementationConfigurationName: String get() = kotlinDependenciesHolder.implementationConfigurationName
    override val compileOnlyConfigurationName: String get() = kotlinDependenciesHolder.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String get() = kotlinDependenciesHolder.runtimeOnlyConfigurationName
    final override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = kotlinDependenciesHolder.dependencies(configure)
    final override fun dependencies(configure: Action<KotlinDependencyHandler>) = dependencies { configure.execute(this) }
    //endregion

    //region KotlinCompilationData delegation: delegate members manually for better control and prevention of accidental overrides
    private val compilationData get() = compilationDetails.compilationData
    override val compileAllTaskName: String get() = compilationData.compileAllTaskName
    final override val compilationPurpose: String get() = compilationData.compilationPurpose
    final override val compilationClassifier: String? get() = compilationData.compilationClassifier
    final override val languageSettings: LanguageSettings get() = compilationData.languageSettings
    final override val ownModuleName: String get() = compilationData.ownModuleName
    final override val moduleName: String get() = compilationData.moduleName
    final override val friendPaths: Iterable<FileCollection> get() = compilationData.friendPaths
    final override val platformType: KotlinPlatformType get() = compilationData.platformType
    final override val output: KotlinCompilationOutput get() = compilationData.output
    final override val compileKotlinTaskName: String get() = compilationData.compileKotlinTaskName

    override val compilerOptions: HasCompilerOptions<*>
        get() = compilationData.compilerOptions

    @Suppress("DEPRECATION")
    @Deprecated("Replaced by compilerOptions", replaceWith = ReplaceWith("compilerOptions.options"))
    final override val kotlinOptions: T get() = compilationData.kotlinOptions
    final override val kotlinSourceDirectoriesByFragmentName get() = compilationData.kotlinSourceDirectoriesByFragmentName
    //endregion

    override val target: KotlinTarget get() = compilationDetails.target

    final override val compileDependencyConfigurationName: String
        get() = compilationDetails.compileDependencyFilesHolder.dependencyConfigurationName

    final override var compileDependencyFiles: FileCollection
        get() = compilationDetails.compileDependencyFilesHolder.dependencyFiles
        set(value) {
            compilationDetails.compileDependencyFilesHolder.dependencyFiles = value
        }

    final override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = compilationDetails.directlyIncludedKotlinSourceSets

    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = compilationDetails.allKotlinSourceSets

    override val defaultSourceSet: KotlinSourceSet get() = compilationDetails.defaultSourceSet

    final override val compilationName: String get() = compilationDetails.compilationData.compilationPurpose

    @Suppress("DEPRECATION")
    @Deprecated("Replaced by compilerOptions.configure { }", replaceWith = ReplaceWith("compilerOptions.configure(configure)"))
    override fun kotlinOptions(configure: T.() -> Unit) =
        configure(kotlinOptions)

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: KotlinCompile<T>
        get() = compileKotlinTaskProvider.get()

    @Suppress("DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<T>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate  task $compileKotlinTaskName")

    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate task $compileKotlinTaskName")

    private val attributeContainer by lazy { HierarchyAttributeContainer(target.attributes) }

    override fun getAttributes(): AttributeContainer = attributeContainer

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = defaultSourceSet.configure()

    override fun source(sourceSet: KotlinSourceSet) {
        compilationDetails.source(sourceSet)
    }

    override fun toString(): String = "compilation '$name' ($target)"

    override fun associateWith(other: KotlinCompilation<*>) {
        compilationDetails.associateWith((other as AbstractKotlinCompilation<*>).compilationDetails)
    }

    override val associateWith: List<KotlinCompilation<*>>
        get() = Collections.unmodifiableList(compilationDetails.associateCompilations.map { it.compilation })

    override val project: Project
        get() = target.project

    override val owner: KotlinTarget
        get() = target

    override val relatedConfigurationNames: List<String>
        get() = compilationDetails.kotlinDependenciesHolder.relatedConfigurationNames + compileDependencyConfigurationName
}