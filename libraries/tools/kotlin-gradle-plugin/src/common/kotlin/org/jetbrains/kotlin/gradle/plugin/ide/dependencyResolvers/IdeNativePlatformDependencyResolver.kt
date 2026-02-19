/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget

internal object IdeNativePlatformDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val project = sourceSet.project
        val commonizerTarget = sourceSet.commonizerTarget.getOrThrow() as? LeafCommonizerTarget ?: return emptySet()
        val konanPlatformLibsCacheService = project.ideKonanDistributionLibsService().get()

        return konanPlatformLibsCacheService.ideDependenciesOfLeafTarget(commonizerTarget)
    }
}
