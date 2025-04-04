/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportLogger
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class IdeProjectToProjectCInteropDependencyResolver(
    private val errorReporter: IdeMultiplatformImportLogger,
) : IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()

        val compilation = sourceSet.internal.compilations.singleOrNull { it.platformType != KotlinPlatformType.common }
            ?.safeAs<KotlinNativeCompilation>() ?: return emptySet()

        // We can't resolve Apple-specific CInterops on non-apple host-machines
        if (!compilation.konanTarget.enabledOnCurrentHostForBinariesCompilation) return emptySet()

        val project = sourceSet.project
        val configuration = project.locateOrCreateCInteropDependencyConfiguration(compilation)

        val cinteropFiles = configuration.incoming.artifactView {
            it.isLenient = true
            it.componentFilter { identifier -> identifier is ProjectComponentIdentifier }
        }.files

        return project.resolveCInteropDependencies(errorReporter, cinteropFiles)
    }

    override fun dependencies(project: Project): Iterable<Any> {
        val extension = project.multiplatformExtensionOrNull ?: return emptySet()
        return extension.targets.filterIsInstance<KotlinNativeTarget>().flatMap { it.compilations }.mapNotNull { compilation ->
            project.locateOrCreateCInteropDependencyConfiguration(compilation).incoming.artifactView { view ->
                view.isLenient = true
            }.files
        }
    }
}
