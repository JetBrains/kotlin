/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal object IdeDependsOnDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return sourceSet.internal.dependsOnClosure.map { dependsOnSourceSet ->
            IdeaKotlinSourceDependency(
                type = IdeaKotlinSourceDependency.Type.DependsOn,
                coordinates = IdeaKotlinSourceCoordinates(
                    buildId = dependsOnSourceSet.internal.project.currentBuildId().name,
                    projectPath = dependsOnSourceSet.internal.project.path,
                    projectName = dependsOnSourceSet.internal.project.name,
                    sourceSetName = dependsOnSourceSet.name
                ),
                extras = mutableExtrasOf()
            )
        }.toSet()
    }
}