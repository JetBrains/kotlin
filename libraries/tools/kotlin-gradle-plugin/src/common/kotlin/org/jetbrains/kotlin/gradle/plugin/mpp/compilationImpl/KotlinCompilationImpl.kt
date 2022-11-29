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
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.filterModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal class KotlinCompilationImpl constructor(
    private val params: Params
) : InternalKotlinCompilation<KotlinCommonOptions> {

    //region Params

    data class Params(
        val target: KotlinTarget,
        val compilationName: String,
        val sourceSets: KotlinCompilationSourceSetsContainer,
        val dependencyConfigurations: KotlinCompilationConfigurationsContainer,
        val compilationTaskNames: KotlinCompilationTaskNamesContainer,
        val processResourcesTaskName: String?,
        val output: KotlinCompilationOutput,
        val compilerOptions: HasCompilerOptions<*>,
        val kotlinOptions: KotlinCommonOptions,
        val compilationAssociator: KotlinCompilationAssociator,
        val compilationFriendPathsResolver: KotlinCompilationFriendPathsResolver,
        val compilationSourceSetInclusion: KotlinCompilationSourceSetInclusion
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

    override fun source(sourceSet: KotlinSourceSet) {
        sourceSets.source(sourceSet)
    }

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) {
        defaultSourceSet.configure()
    }


    //endregion


    //region Dependency Configuration Management

    override val apiConfigurationName: String
        get() = configurations.apiConfiguration.name

    override val implementationConfigurationName: String
        get() = configurations.implementationConfiguration.name

    override val compileOnlyConfigurationName: String
        get() = configurations.compileOnlyConfiguration.name

    override val runtimeOnlyConfigurationName: String
        get() = configurations.runtimeOnlyConfiguration.name

    override val compileDependencyConfigurationName: String
        get() = configurations.compileDependencyConfiguration.name

    override val runtimeDependencyConfigurationName: String?
        get() = configurations.runtimeDependencyConfiguration?.name

    override var compileDependencyFiles: FileCollection = configurations.compileDependencyConfiguration

    override var runtimeDependencyFiles: FileCollection? = configurations.runtimeDependencyConfiguration

    override val relatedConfigurationNames: List<String> = listOfNotNull(
        apiConfigurationName,
        implementationConfigurationName,
        compileOnlyConfigurationName,
        runtimeOnlyConfigurationName,
        compileDependencyConfigurationName,
        runtimeDependencyConfigurationName
    )

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
    }

    override fun dependencies(configure: Action<KotlinDependencyHandler>) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
    }

    //endregion


    //region Compiler Module Management

    @Suppress("OVERRIDE_DEPRECATION")
    override val moduleName: String
        get() {
            val baseName = target.project.archivesName.orNull ?: target.project.name
            val suffix = if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) "" else "_$compilationName"
            return filterModuleName("$baseName$suffix")
        }

    //endregion


    //region Compile Tasks

    override val compileKotlinTaskName: String
        get() = params.compilationTaskNames.compileTaskName


    override val compileAllTaskName: String
        get() = params.compilationTaskNames.compileAllTaskName

    @Suppress("deprecation")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: KotlinCompile<KotlinCommonOptions>
        get() = compileKotlinTaskProvider.get()

    @Suppress("deprecation")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<KotlinCommonOptions>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate  task $compileKotlinTaskName")

    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<*>>
        get() = target.project.locateTask(compileKotlinTaskName) ?: throw GradleException("Couldn't locate task $compileKotlinTaskName")

    //endregion


    //region CompilerOptions & KotlinOptions

    @Deprecated(message = "Replaced by compilerOptions", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: KotlinCommonOptions
        get() = params.kotlinOptions

    override val compilerOptions: HasCompilerOptions<*>
        get() = params.compilerOptions

    //endregion

    //region Attributes

    private val attributes by lazy { HierarchyAttributeContainer(target.attributes) }

    override fun getAttributes(): AttributeContainer = attributes

    // endregion

    private val associateWithImpl = mutableSetOf<KotlinCompilation<*>>()

    override val associateWith: List<KotlinCompilation<*>>
        get() = associateWithImpl.toList()

    override fun associateWith(other: KotlinCompilation<*>) {
        require(other.target == target) { "Only associations between compilations of a single target are supported" }
        if (!associateWithImpl.add(other)) return
        params.compilationAssociator.associate(target, this, other.internal)
    }


    //region final init

    init {
        sourceSets.allKotlinSourceSets.forAll { sourceSet ->
            params.compilationSourceSetInclusion.include(this, sourceSet)
        }
    }

    //endregion

    override fun toString(): String {
        return "compilation '$name' ($target)"
    }
}
