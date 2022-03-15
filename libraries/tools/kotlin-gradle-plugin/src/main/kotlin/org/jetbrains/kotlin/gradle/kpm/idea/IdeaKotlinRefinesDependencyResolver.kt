/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.refinesClosure

internal object IdeaKotlinRefinesDependencyResolver : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> = fragment.refinesClosure
        .map { refinesDependencyFragment -> createRefinesDependency(refinesDependencyFragment) }.toSet()

    private fun createRefinesDependency(fragment: KotlinGradleFragment): IdeaKotlinDependency {
        return IdeaKotlinSourceDependencyImpl(
            type = IdeaKotlinSourceDependency.Type.Refines,
            coordinates = IdeaKotlinSourceCoordinatesImpl(
                buildId = fragment.project.currentBuildId().name,
                projectPath = fragment.project.path,
                projectName = fragment.project.name,
                kotlinModuleName = fragment.containingModule.name,
                kotlinModuleClassifier = fragment.containingModule.moduleClassifier,
                kotlinFragmentName = fragment.name
            )
        )
    }
}
