/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.targets.native.internal.cinteropMetadataDependencyTransformationForIdeTaskName
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropMetadataDependencyClasspathForIde
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal object IdeCInteropMetadataDependencyClasspathResolver : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()

        val project = sourceSet.project
        project.locateDependencyTask(sourceSet) ?: return emptySet()

        val cinteropFiles = project.createCInteropMetadataDependencyClasspathForIde(sourceSet)
        return project.resolveCInteropDependencies(cinteropFiles)
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return project.multiplatformExtension.sourceSets
            .filterIsInstance<DefaultKotlinSourceSet>()
            .filter { it.commonizerTarget.getOrThrow() is SharedCommonizerTarget}
            .mapNotNull { project.locateDependencyTask(it) }
    }

    private fun Project.locateDependencyTask(sourceSet: DefaultKotlinSourceSet): TaskProvider<*>? =
        locateTask<CInteropMetadataDependencyTransformationTask>(sourceSet.cinteropMetadataDependencyTransformationForIdeTaskName)
}
