/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.project.model.refinesClosure
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

open class VariantSourcesProvider {
    open fun getSourcesFromRefinesClosure(variant: KotlinGradleVariant): FileCollection {
        val project = variant.containingModule.project
        return project.files(project.provider {
            variant.refinesClosure.map { it.kotlinSourceRoots }
        })
    }

    open fun getCommonSourcesFromRefinesClosure(variant: KotlinGradleVariant): FileCollection {
        val containingModule = variant.containingModule
        val project = containingModule.project
        return project.files(project.provider {
            variant.refinesClosure.filter {
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