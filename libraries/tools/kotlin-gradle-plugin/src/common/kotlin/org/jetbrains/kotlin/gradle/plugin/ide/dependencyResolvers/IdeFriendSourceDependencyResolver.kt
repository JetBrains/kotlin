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
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

object IdeFriendSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return getVisibleSourceSetsFromAssociateCompilations(sourceSet).map { friendSourceSet ->
            IdeaKotlinSourceDependency(
                type = IdeaKotlinSourceDependency.Type.Friend,
                extras = mutableExtrasOf(),
                coordinates = IdeaKotlinSourceCoordinates(
                    buildId = friendSourceSet.project.currentBuildId().toString(),
                    projectPath = friendSourceSet.project.path,
                    projectName = friendSourceSet.project.name,
                    sourceSetName = friendSourceSet.name
                )
            )
        }.toSet()
    }
}