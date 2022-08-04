/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.JarMppDependencyProjectStructureMetadataExtractor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.utils.ResolvedDependencyGraph

internal class PSMExtractor(
    private val psmByProjectPath: Map<String, Provider<KotlinProjectStructureMetadata?>>,
    //private val allCompileDependencies: ResolvedDependencyGraph,
) {
    fun extract(dependency: ResolvedDependencyResult, metadataArtifact: ResolvedArtifactResult?): KotlinProjectStructureMetadata? {
        val moduleId = dependency.selected.id

        // Fast finish if project
        if (moduleId is ProjectComponentIdentifier) {
            return psmByProjectPath[moduleId.projectPath]?.get()
        }

        if (moduleId !is ModuleComponentIdentifier) return null

        // Make sure that resolved dependency is "metadata" one.
        // require(dependency.resolvedVariant.attributes)
        if (metadataArtifact == null) return null
        return JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file).getProjectStructureMetadata()
    }
}