/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportLogger
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropMetadataDependencyClasspathForIde
import org.jetbrains.kotlin.gradle.utils.future

internal class IdeCInteropMetadataDependencyClasspathResolver(
    private val errorReporter: IdeMultiplatformImportLogger,
) : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        val project = sourceSet.project

        val cinteropFiles = project.future { createCInteropMetadataDependencyClasspathForIde(sourceSet) }.getOrThrow()
        return project.resolveCInteropDependencies(errorReporter, cinteropFiles)
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return project.multiplatformExtension.sourceSets
            .filterIsInstance<DefaultKotlinSourceSet>()
            .filter { it.commonizerTarget.getOrThrow() is SharedCommonizerTarget }
            .map { sourceSet -> project.future { createCInteropMetadataDependencyClasspathForIde(sourceSet) }.getOrThrow() }
    }
}
