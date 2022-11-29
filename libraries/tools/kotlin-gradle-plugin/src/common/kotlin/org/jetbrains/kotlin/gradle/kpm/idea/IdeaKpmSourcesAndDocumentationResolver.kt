/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ArtifactResolutionResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.component.Artifact
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmResolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.resolvableMetadataConfigurationName

internal class IdeaKpmSourcesAndDocumentationResolver : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        if (fragment is GradleKpmVariant) {
            return resolve(fragment.project, fragment.compileDependenciesConfiguration)
        }

        val metadataDependencies = fragment.project.configurations.getByName(fragment.containingModule.resolvableMetadataConfigurationName)
        return resolve(fragment.project, metadataDependencies)
    }

    private fun resolve(project: Project, configuration: Configuration): Set<IdeaKpmDependency> {
        val resolutionResult = project.dependencies.createArtifactResolutionQuery()
            .forComponents(configuration.incoming.resolutionResult.allComponents.map { it.id })
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java, JavadocArtifact::class.java)
            .execute()

        return resolve(resolutionResult, SourcesArtifact::class.java, IdeaKpmDependency.SOURCES_BINARY_TYPE) +
                resolve(resolutionResult, JavadocArtifact::class.java, IdeaKpmDependency.DOCUMENTATION_BINARY_TYPE)
    }

    fun resolve(
        resolutionResult: ArtifactResolutionResult, artifactType: Class<out Artifact>, binaryType: String
    ): Set<IdeaKpmDependency> {
        return resolutionResult.resolvedComponents.flatMap { resolved ->
            resolved.getArtifacts(artifactType)
                .filterIsInstance<ResolvedArtifactResult>()
                .mapNotNull { artifact ->
                    val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
                    IdeaKpmResolvedBinaryDependencyImpl(
                        coordinates = IdeaKpmBinaryCoordinatesImpl(group = id.group, module = id.module, version = id.version),
                        binaryType = binaryType,
                        binaryFile = artifact.file
                    )
                }
        }.toSet()
    }
}
