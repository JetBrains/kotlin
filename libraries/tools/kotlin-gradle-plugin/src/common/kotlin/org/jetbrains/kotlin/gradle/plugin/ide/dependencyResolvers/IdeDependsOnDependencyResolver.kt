/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependency
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeSourceCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.IdeSourceDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.tooling.core.emptyExtras

internal object IdeDependsOnDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeDependency> {
        return sourceSet.internal.dependsOnClosure.map { dependsOnSourceSet ->
            IdeSourceDependency(
                type = IdeSourceDependency.Type.DependsOn,
                coordinates = IdeSourceCoordinates(
                    buildId = dependsOnSourceSet.internal.project.currentBuildId().name,
                    projectPath = dependsOnSourceSet.internal.project.path,
                    projectName = dependsOnSourceSet.internal.project.name,
                    sourceSetName = dependsOnSourceSet.name
                ),
                extras = emptyExtras()
            )
        }.toSet()
    }
}