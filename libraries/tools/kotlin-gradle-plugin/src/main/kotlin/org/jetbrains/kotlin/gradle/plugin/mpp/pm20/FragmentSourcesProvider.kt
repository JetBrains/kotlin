/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

open class FragmentSourcesProvider {
    protected open fun getSourcesFromFragmentsAsMap(
        fragments: Iterable<KotlinGradleFragment>
    ): Map<KotlinGradleFragment, FileCollection> =
        fragments.associateWith { it.project.files(it.project.provider { it.kotlinSourceRoots }) }

    open fun getFragmentOwnSources(fragment: KotlinGradleFragment): FileCollection =
        getSourcesFromFragmentsAsMap(listOf(fragment)).values.single()

    open fun getAllFragmentSourcesAsMap(module: KotlinGradleModule): Map<KotlinGradleFragment, FileCollection> =
        getSourcesFromFragmentsAsMap(module.fragments)

    open fun getSourcesFromRefinesClosureAsMap(fragment: KotlinGradleFragment): Map<KotlinGradleFragment, FileCollection> =
        getSourcesFromFragmentsAsMap(fragment.refinesClosure)

    open fun getSourcesFromRefinesClosure(fragment: KotlinGradleFragment): FileCollection =
        fragment.project.files(fragment.project.provider { getSourcesFromRefinesClosureAsMap(fragment).values })

    open fun getCommonSourcesFromRefinesClosure(fragment: KotlinGradleFragment): FileCollection {
        val containingModule = fragment.containingModule
        val project = containingModule.project
        getSourcesFromRefinesClosureAsMap(fragment)
        return project.files(project.provider {
            fragment.refinesClosure.filter {
                // Every fragment refined by some other fragment should be considered common, even if it is included in just one variant
                containingModule.variantsContainingFragment(it).toSet() != setOf(it)
            }.map { it.kotlinSourceRoots }
        })
    }
}

open class LifecycleTasksManager(private val project: Project) {
    open fun registerClassesTask(compilationData: KotlinCompilationData<*>) = with(project) {
        val classesTaskName = compilationData.compileAllTaskName
        val classesTask = project.tasks.register(classesTaskName) { classesTask ->
            classesTask.dependsOn(compilationData.output.allOutputs)
        }
        project.tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(classesTask) }
    }
}