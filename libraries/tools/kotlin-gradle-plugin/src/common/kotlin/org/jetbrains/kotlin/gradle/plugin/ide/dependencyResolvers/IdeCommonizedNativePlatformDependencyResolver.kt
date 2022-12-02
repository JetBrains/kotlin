/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.resolveCommonizedDirectory
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isCommonized
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeNativeDistributionTask
import org.jetbrains.kotlin.gradle.targets.native.internal.getCommonizerTarget
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION

internal object IdeCommonizedNativePlatformDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val project = sourceSet.project
        val commonizerTarget = getCommonizerTarget(sourceSet) as? SharedCommonizerTarget ?: return emptySet()
        val commonizerTask = project.commonizeNativeDistributionTask?.get() ?: return emptySet()
        val outputDirectory = resolveCommonizedDirectory(commonizerTask.rootOutputDirectory, commonizerTarget)

        return outputDirectory.listFiles().orEmpty()
            .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { libraryFile -> project.resolveNativeDistributionLibraryForIde(libraryFile, commonizerTarget, project.logger) }
            .onEach { dependency -> dependency.isCommonized = true }
            .toSet()
    }
}
