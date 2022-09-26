/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.metadataTransformation

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.JarMppDependencyProjectStructureMetadataExtractor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata

internal class ProjectStructureMetadataExtractor(
    private val projectStructureMetadataByProjectPath: Map<String, Provider<KotlinProjectStructureMetadata?>>,
) {

    fun extract(metadataArtifact: ResolvedArtifactResult): KotlinProjectStructureMetadata? {
        val moduleId = metadataArtifact.variant.owner

        // Fast finish if project
        if (moduleId is ProjectComponentIdentifier && moduleId.build.isCurrentBuild) {
            return projectStructureMetadataByProjectPath[moduleId.projectPath]?.get() ?: error("PSM for project ${moduleId.projectPath} not found")
        }

        return JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file).getProjectStructureMetadata()
    }
}