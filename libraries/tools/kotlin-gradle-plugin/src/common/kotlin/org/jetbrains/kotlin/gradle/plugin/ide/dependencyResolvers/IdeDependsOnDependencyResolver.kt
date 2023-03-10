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
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object IdeDependsOnDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return sourceSet.internal.dependsOnClosure.map { dependsOnSourceSet ->
            IdeaKotlinSourceDependency(
                type = IdeaKotlinSourceDependency.Type.DependsOn,
                coordinates = IdeaKotlinSourceCoordinates(
                    project = IdeaKotlinProjectCoordinates(dependsOnSourceSet.project),
                    sourceSetName = dependsOnSourceSet.name
                )
            )
        }.toSet()
    }
}