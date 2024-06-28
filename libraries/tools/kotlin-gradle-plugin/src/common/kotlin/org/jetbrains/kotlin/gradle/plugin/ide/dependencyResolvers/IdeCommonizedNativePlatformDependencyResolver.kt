/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isCommonized
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION

internal object IdeCommonizedNativePlatformDependencyResolver :
    IdeDependencyResolver, IdeDependencyResolver.WithBuildDependencies {

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val project = sourceSet.project
        val commonizerTarget = sourceSet.commonizerTarget.getOrThrow() as? SharedCommonizerTarget ?: return emptySet()
        val commonizerTask = project.commonizeNativeDistributionTask?.get() ?: return emptySet()
        val outputDirectory = resolveCommonizedDirectory(commonizerTask.rootOutputDirectoryProperty.get().asFile, commonizerTarget)

        return outputDirectory.listFiles().orEmpty()
            .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { libraryFile -> project.resolveNativeDistributionLibraryForIde(libraryFile, commonizerTarget, project.logger) }
            .onEach { dependency -> dependency.isCommonized = true }
            .toSet()
    }

    override fun dependencies(project: Project): Iterable<Any> {
        return listOfNotNull(project.commonizeNativeDistributionTask)
    }
}
