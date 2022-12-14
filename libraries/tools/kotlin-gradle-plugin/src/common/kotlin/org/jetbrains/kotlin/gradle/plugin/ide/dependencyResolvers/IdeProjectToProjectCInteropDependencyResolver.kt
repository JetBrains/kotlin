/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver

object IdeProjectToProjectCInteropDependencyResolver : IdeDependencyResolver {
    private val platformDependencyResolver = IdePlatformDependencyResolver(
        resolvedArtifactHandler = handler@{ artifact, context ->
            val identifier = artifact.id.componentIdentifier as? ProjectComponentIdentifier
                ?: return@handler null

            val projectArtifact = context.allArtifacts.filter { otherArtifact ->
                val otherIdentifier = otherArtifact.id.componentIdentifier
                otherIdentifier is ProjectComponentIdentifier && otherIdentifier.projectName == identifier.projectName
            }

            null
        }
    )

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return platformDependencyResolver.resolve(sourceSet)
    }
}