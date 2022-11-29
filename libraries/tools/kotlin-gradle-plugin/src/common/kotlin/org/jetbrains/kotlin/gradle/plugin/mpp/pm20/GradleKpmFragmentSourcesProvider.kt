/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

typealias SourceRoots = Iterable<File>
typealias SourceRootsProvider = Provider<out SourceRoots>
typealias MultipleSourceRootsProvider = Provider<out Iterable<SourceRootsProvider>>
typealias SourceRootsProvidersByFragment = Map<GradleKpmFragment, SourceRootsProvider>

/** Note: the API is [Provider]-based rather than FileCollection-based because FileCollection API erases the internal structure of the
 * file sets, and this internal structure is currently needed for correctly inferring Java source roots from the sources added to the
 * JVM compilations (it is important to pass sources in SourceDirectorySets) */
open class GradleKpmFragmentSourcesProvider {
    protected open fun getSourcesFromFragmentsAsMap(
        fragments: Iterable<GradleKpmFragment>
    ): SourceRootsProvidersByFragment =
        fragments.associateWith { it.project.provider { it.kotlinSourceRoots } }

    open fun getFragmentOwnSources(fragment: GradleKpmFragment): SourceRootsProvider =
        getSourcesFromFragmentsAsMap(listOf(fragment)).values.single()

    open fun getAllFragmentSourcesAsMap(module: GradleKpmModule): SourceRootsProvidersByFragment =
        getSourcesFromFragmentsAsMap(module.fragments)

    open fun getSourcesFromRefinesClosureAsMap(fragment: GradleKpmFragment): SourceRootsProvidersByFragment =
        getSourcesFromFragmentsAsMap(fragment.withRefinesClosure)

    open fun getSourcesFromRefinesClosure(fragment: GradleKpmFragment): MultipleSourceRootsProvider =
        fragment.project.provider { getSourcesFromRefinesClosureAsMap(fragment).values }

    open fun getCommonSourcesFromRefinesClosure(fragment: GradleKpmFragment): MultipleSourceRootsProvider {
        val containingModule = fragment.containingModule
        val project = containingModule.project
        getSourcesFromRefinesClosureAsMap(fragment)
        return project.provider {
            fragment.withRefinesClosure.filter {
                // Every fragment refined by some other fragment should be considered common, even if it is included in just one variant
                it.containingVariants != setOf(it)
            }.map { project.provider { it.kotlinSourceRoots } }
        }
    }
}

open class LifecycleTasksManager(private val project: Project) {
    open fun registerClassesTask(compilationData: GradleKpmCompilationData<*>) = with(project) {
        val classesTaskName = compilationData.compileAllTaskName
        val classesTask = project.tasks.register(classesTaskName) { classesTask ->
            classesTask.inputs.files(compilationData.output.allOutputs)
        }
        project.tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(classesTask) }
    }
}
