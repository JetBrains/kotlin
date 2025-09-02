/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal class KotlinCompilationImpl(
    private val params: Params,
) : InternalKotlinCompilation<KotlinAnyOptionsDeprecated> {

    //region Params

    data class Params(
        val target: KotlinTarget,
        val compilationName: String,
        val sourceSets: KotlinCompilationSourceSetsContainer,
        val dependencyConfigurations: KotlinCompilationConfigurationsContainer,
        val compilationTaskNames: KotlinCompilationTaskNamesContainer,
        val processResourcesTaskName: String?,
        val output: KotlinCompilationOutput,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR") val compilerOptions: DeprecatedHasCompilerOptions<*>,
        @Suppress("DEPRECATION_ERROR") val kotlinOptions: KotlinCommonOptions,
        val compilationAssociator: KotlinCompilationAssociator,
        val compilationFriendPathsResolver: KotlinCompilationFriendPathsResolver,
        val compilationSourceSetInclusion: KotlinCompilationSourceSetInclusion,
    )

    //endregion


    //region direct access / convenience properties

    override val project get() = params.target.project

    override val target: KotlinTarget
        get() = params.target

    override val extras: MutableExtras = mutableExtrasOf()

    val sourceSets get() = params.sourceSets

    override val configurations: KotlinCompilationConfigurationsContainer
        get() = params.dependencyConfigurations

    override val compilationName: String
        get() = params.compilationName

    override val output: KotlinCompilationOutput
        get() = params.output

    override val processResourcesTaskName: String?
        get() = params.processResourcesTaskName

    override val friendPaths: Iterable<FileCollection>
        get() = params.compilationFriendPathsResolver.resolveFriendPaths(this)

    //endregion


    //region Implement Source Set Management


    override val defaultSourceSet: KotlinSourceSet
        get() = sourceSets.defaultSourceSet

    override val allKotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = sourceSets.allKotlinSourceSets

    override val kotlinSourceSets: ObservableSet<KotlinSourceSet>
        get() = sourceSets.kotlinSourceSets

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) {
        defaultSourceSet.configure()
    }


    //endregion


    //region Dependency Configuration Management

    @Deprecated(
        "Accessing apiConfigurationName on Compilation level is deprecated, please use default source set instead",
        replaceWith = ReplaceWith("defaultSourceSet.apiConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val apiConfigurationName: String
        get() = configurations.apiConfiguration.name

    @Deprecated(
        "Accessing implementationConfigurationName on Compilation level is deprecated, please use default source set instead",
        replaceWith = ReplaceWith("defaultSourceSet.implementationConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val implementationConfigurationName: String
        get() = configurations.implementationConfiguration.name

    @Deprecated(
        "Accessing compileOnlyConfigurationName on Compilation level is deprecated, please use default source set instead",
        replaceWith = ReplaceWith("defaultSourceSet.compileOnlyConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val compileOnlyConfigurationName: String
        get() = configurations.compileOnlyConfiguration.name

    @Deprecated(
        "Accessing runtimeOnlyConfigurationName on Compilation level is deprecated, please use default source set instead",
        replaceWith = ReplaceWith("defaultSourceSet.runtimeOnlyConfigurationName"),
        level = DeprecationLevel.WARNING
    )
    override val runtimeOnlyConfigurationName: String
        get() = configurations.runtimeOnlyConfiguration.name

    override val compileDependencyConfigurationName: String
        get() = configurations.compileDependencyConfiguration.name

    override val runtimeDependencyConfigurationName: String?
        get() = configurations.runtimeDependencyConfiguration?.name

    override var compileDependencyFiles: FileCollection = configurations.compileDependencyConfiguration

    override var runtimeDependencyFiles: FileCollection? = configurations.runtimeDependencyConfiguration

    @Deprecated(
        "Declaring dependencies on Compilation level is deprecated, please declare on related source set",
        replaceWith = ReplaceWith("defaultSourceSet.dependencies"),
        level = DeprecationLevel.WARNING
    )
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
    }

    @Deprecated(
        "Declaring dependencies on Compilation level is deprecated, please declare on related source set",
        replaceWith = ReplaceWith("defaultSourceSet.dependencies"),
        level = DeprecationLevel.WARNING
    )
    override fun dependencies(configure: Action<KotlinDependencyHandler>) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
    }

    //endregion

    //region Compile Tasks

    override val compileKotlinTaskName: String
        get() = params.compilationTaskNames.compileTaskName


    override val compileAllTaskName: String
        get() = params.compilationTaskNames.compileAllTaskName

    @Suppress("deprecation_error")
    @Deprecated(
        "Accessing task instance directly is deprecated. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR,
    )
    override val compileKotlinTask: KotlinCompile<KotlinCommonOptions>
        get() = compileKotlinTaskProvider.get()

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Replaced with compileTaskProvider. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<KotlinCommonOptions>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate  task $compileKotlinTaskName")

    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate task $compileKotlinTaskName")

    //endregion

    override val archiveTaskName: String?
        get() = project.kotlinCompilationArchiveTasksOrNull?.getArchiveTaskOrNull(this)?.name
            ?: target.artifactsTaskName.takeIf { isMain() }

    //region CompilerOptions & KotlinOptions

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION_ERROR")
    override val kotlinOptions: KotlinCommonOptions
        get() = params.kotlinOptions

    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
    override val compilerOptions: DeprecatedHasCompilerOptions<*>
        get() = params.compilerOptions

    //endregion

    //region Attributes

    private val attributes by lazy { HierarchyAttributeContainer(target.attributes) }

    override fun getAttributes(): AttributeContainer = attributes

    // endregion


    private val associatedCompilationsImpl = MutableObservableSetImpl<KotlinCompilation<*>>()

    private val allAssociatedCompilationsImpl = MutableObservableSetImpl<KotlinCompilation<*>>()

    override val associatedCompilations: ObservableSet<KotlinCompilation<*>>
        get() = associatedCompilationsImpl

    override val allAssociatedCompilations: ObservableSet<KotlinCompilation<*>>
        get() = allAssociatedCompilationsImpl

    override fun associateWith(other: KotlinCompilation<*>) {
        require(other.target == target) { "Only associations between compilations of a single target are supported" }
        if (!associatedCompilationsImpl.add(other)) return
        if (!allAssociatedCompilationsImpl.add(other)) return
        other.internal.allAssociatedCompilations.forAll { compilation -> allAssociatedCompilationsImpl.add(compilation) }
    }

    //region final init

    init {
        sourceSets.allKotlinSourceSets.forAll { sourceSet ->
            params.compilationSourceSetInclusion.include(this, sourceSet)
        }

        allAssociatedCompilations.forAll { compilation ->
            params.compilationAssociator.associate(target, this, compilation.internal)
        }
    }

    //endregion

    override fun toString(): String {
        return "compilation '$name' ($target)"
    }
}
