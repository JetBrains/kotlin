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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinCompilationSourceDeprecation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal class KotlinCompilationImpl constructor(
    private val params: Params,
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
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") val compilerOptions: DeprecatedHasCompilerOptions<*>,
        val kotlinOptions: KotlinCommonOptions,
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

    @Deprecated("scheduled for removal with Kotlin 2.0")
    override fun source(sourceSet: KotlinSourceSet) {
        project.kotlinToolingDiagnosticsCollector.report(project, KotlinCompilationSourceDeprecation(Throwable()))
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

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
    }

    override fun dependencies(configure: Action<KotlinDependencyHandler>) {
        HasKotlinDependencies(project, configurations).dependencies(configure)
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

    override val archiveTaskName: String?
        get() = project.kotlinCompilationArchiveTasksOrNull?.getArchiveTaskOrNull(this)?.name

    //region CompilerOptions & KotlinOptions

    override val kotlinOptions: KotlinCommonOptions
        get() = params.kotlinOptions

    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
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
