/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.targets.metadata.findMetadataCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.*

private typealias SourceSetName = String

internal fun ProjectMetadataProvider(
    sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>
): ProjectMetadataProvider {
    return ProjectMetadataProviderImpl(sourceSetMetadataOutputs)
}

internal class SourceSetMetadataOutputs(
    val metadata: FileCollection,
    val cinterop: CInterop?
) {
    class CInterop(
        val forCli: FileCollection,
        val forIde: FileCollection
    )
}

private class ProjectMetadataProviderImpl(
    private val sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>
) : ProjectMetadataProvider() {

    override fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection =
        sourceSetMetadataOutputs[sourceSetName]?.metadata ?: error("Unexpected source set '$sourceSetName'")

    override fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection? {
        val metadataOutputs = sourceSetMetadataOutputs[sourceSetName] ?: error("Unexpected source set '$sourceSetName'")
        val cinteropMetadata = metadataOutputs.cinterop ?: return null
        return when (consumer) {
            MetadataConsumer.Ide -> cinteropMetadata.forIde
            MetadataConsumer.Cli -> cinteropMetadata.forCli
        }
    }
}

internal suspend fun Project.collectSourceSetMetadataOutputs(): Map<SourceSetName, SourceSetMetadataOutputs> {
    val multiplatformExtension = multiplatformExtensionOrNull ?: return emptyMap()

    val sourceSetMetadata = multiplatformExtension.sourceSetsMetadataOutputs()
    val sourceSetCInteropMetadata = multiplatformExtension.cInteropMetadataOfSourceSets(sourceSetMetadata.keys)

    return sourceSetMetadata.mapValues { (sourceSet, metadata) ->
        val cinteropMetadataOutput = sourceSetCInteropMetadata[sourceSet]
        SourceSetMetadataOutputs(
            metadata = metadata,
            cinterop = cinteropMetadataOutput
        )
    }.mapKeys { it.key.name }
}

private suspend fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Map<KotlinSourceSet, FileCollection> {
    KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()

    return sourceSets.mapNotNull { sourceSet ->
        val destination = when (val compilation = project.findMetadataCompilation(sourceSet) ?: return@mapNotNull null) {
            is KotlinCommonCompilation -> compilation.output.classesDirs
            is KotlinSharedNativeCompilation -> compilation.output.classesDirs
            else -> error("Unexpected compilation type: $compilation")
        }

        Pair(sourceSet, destination)
    }.toMap()
}

private suspend fun KotlinMultiplatformExtension.cInteropMetadataOfSourceSets(
    sourceSets: Iterable<KotlinSourceSet>
): Map<KotlinSourceSet, SourceSetMetadataOutputs.CInterop?> {
    val taskForCLI = project.commonizeCInteropTask ?: return emptyMap()
    val taskForIde = project.copyCommonizeCInteropForIdeTask ?: return emptyMap()

    return sourceSets.associateWith { sourceSet ->
        val dependent = CInteropCommonizerDependent.from(sourceSet) ?: return@associateWith null
        SourceSetMetadataOutputs.CInterop(
            forCli = taskForCLI.get().commonizedOutputLibraries(dependent),
            forIde = taskForIde.get().commonizedOutputLibraries(dependent)
        )
    }
}