/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.gdt

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.JarMppDependencyProjectStructureMetadataExtractor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.toJson
import java.io.File

/**
 * Persisting means it is going to save metadata extractions on the disk
 */
internal class PersistingPSMExtractor(private val ids: ModuleIds) {
    private val ResolvedComponentResult.isMpp get() =
        dependents.isNotEmpty() && // filter out the root of the dependency graph, we are not interested in it
        variants.any { variant -> variant.attributes.keySet().any { it.name == KotlinPlatformType.attribute.name } }

    fun extract(
        rootComponent: ResolvedComponentResult,
        artifacts: ArtifactCollection,
        destination: File? = null
    ): Map<ResolvedComponentResult, Pair<File, KotlinProjectStructureMetadata>> {
        val result = mutableMapOf<ResolvedComponentResult, Pair<File, KotlinProjectStructureMetadata>>()

        val mppComponents = rootComponent.allComponents.filter { it.isMpp }.associateBy { it.id }
        val componentToArtifact = artifacts
            .filter { it.id.componentIdentifier in mppComponents }
            .associateBy { mppComponents.getValue(it.id.componentIdentifier) }

        for ((component, artifact) in componentToArtifact) {
            val fileName = ids.fromComponent(component).toString()
            val psm = JarMppDependencyProjectStructureMetadataExtractor(artifact.file).getProjectStructureMetadata()

            if (psm == null) {
                println("For $component there is no PSM")
                continue
            }

            if (destination != null) {
                destination.resolve("${fileName}.json").writeText(psm.toJson())
            }
            result[component] = artifact.file to psm
        }

        return result
    }
}