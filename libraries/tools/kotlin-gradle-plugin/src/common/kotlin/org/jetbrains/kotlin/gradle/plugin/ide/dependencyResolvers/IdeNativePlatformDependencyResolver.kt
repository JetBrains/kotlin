/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION

internal object IdeNativePlatformDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val project = sourceSet.project
        val commonizerTarget = sourceSet.commonizerTarget.getOrThrow() as? LeafCommonizerTarget ?: return emptySet()
        val konanTarget = commonizerTarget.konanTargetOrNull ?: return emptySet()

        return sourceSet.project.konanDistribution.platformLibsDir.resolve(konanTarget.name)
            .listFiles().orEmpty()
            .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { libraryFile -> project.resolveNativeDistributionLibraryForIde(libraryFile, commonizerTarget, project.logger) }
            .toSet()
    }

    private val Project.konanDistribution: KonanDistribution
        get() = KonanDistribution(project.file(konanHome))
}
