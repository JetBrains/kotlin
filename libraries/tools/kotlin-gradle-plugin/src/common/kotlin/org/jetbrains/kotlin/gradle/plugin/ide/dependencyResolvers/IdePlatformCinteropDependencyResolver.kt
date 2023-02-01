/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.getPlatformCinteropDependenciesOrEmpty

internal object IdePlatformCinteropDependencyResolver : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        val project = sourceSet.project
        val cinteropFiles = project.getPlatformCinteropDependenciesOrEmpty(sourceSet)
        return project.resolveCInteropDependencies(cinteropFiles)
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return project.kotlinExtension.sourceSets.mapNotNull { sourceSet ->
            if (sourceSet !is DefaultKotlinSourceSet) return@mapNotNull null
            if (!IdeMultiplatformImport.SourceSetConstraint.isNative(sourceSet)) return@mapNotNull null
            project.getPlatformCinteropDependenciesOrEmpty(sourceSet)
        }
    }
}
