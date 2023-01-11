/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.metadataCompilationRegistryByModuleId
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

private typealias SourceSetName = String
private typealias ProjectPath = String

internal fun ProjectMetadataProvider(
    dependencyProject: Project,
    moduleIdentifier: KpmModuleIdentifier
): ProjectMetadataProvider {
    return ProjectMetadataProviderImpl(dependencyProject, moduleIdentifier)
}

internal fun Project.collectAllProjectsSourceSetMetadataOutputs(): Map<ProjectPath, Map<SourceSetName, SourceSetMetadataOutputs>> {
    return rootProject.allprojects.associateBy { it.path }.mapValues { (_, subProject) ->
        subProject.collectSourceSetMetadataOutputs()
    }
}

internal fun Project.collectSourceSetMetadataOutputs(): Map<SourceSetName, SourceSetMetadataOutputs> {
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

private fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Map<KotlinSourceSet, FileCollection> {
    val commonTarget = targets.withType<KotlinMetadataTarget>().singleOrNull() ?: return emptyMap()

    val compilations = commonTarget.compilations

    return sourceSets.mapNotNull { sourceSet ->
        val compilation = compilations.findByName(sourceSet.name)
            ?: return@mapNotNull null // given source set is not shared

        val destination = when (compilation) {
            is KotlinCommonCompilation -> compilation.output.classesDirs
            is KotlinSharedNativeCompilation -> compilation.output.classesDirs
            else -> error("Unexpected compilation type: $compilation")
        }

        Pair(sourceSet, destination)
    }.toMap()
}

private fun KotlinMultiplatformExtension.cInteropMetadataOfSourceSets(
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

internal fun ProjectMetadataProvider(
    sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>
): ProjectMetadataProvider = ProjectMetadataProviderImpl2(sourceSetMetadataOutputs)

internal class SourceSetMetadataOutputs(
    val metadata: FileCollection,
    val cinterop: CInterop?
) {
    class CInterop(
        val forCli: FileCollection,
        val forIde: FileCollection
    )
}

private class ProjectMetadataProviderImpl2(
    private val sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>
): ProjectMetadataProvider() {
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

private class ProjectMetadataProviderImpl(
    private val dependencyProject: Project,
    private val moduleIdentifier: KpmModuleIdentifier
) : ProjectMetadataProvider() {
    override fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection {
        val projectExtension = dependencyProject.topLevelExtensionOrNull
        return when {
            dependencyProject.pm20ExtensionOrNull != null -> {
                val moduleId = moduleIdentifier
                val module = dependencyProject.pm20Extension.modules.single { it.moduleIdentifier == moduleId }
                val metadataCompilationRegistry = dependencyProject.metadataCompilationRegistryByModuleId.getValue(moduleId)
                metadataCompilationRegistry.getForFragmentOrNull(module.fragments.getByName(sourceSetName))?.output?.classesDirs
                    ?: dependencyProject.files()
            }
            projectExtension is KotlinMultiplatformExtension ->
                projectExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME).compilations
                    .firstOrNull { it.name == sourceSetName }
                    ?.output?.classesDirs ?: dependencyProject.files()
            else -> error("unexpected top-level Kotlin extension $projectExtension")
        }
    }

    override fun getSourceSetCInteropMetadata(sourceSetName: String, consumer: MetadataConsumer): FileCollection? {
        val multiplatformExtension = dependencyProject.topLevelExtension as? KotlinMultiplatformExtension
            ?: return dependencyProject.files()

        val commonizeCInteropTask = when (consumer) {
            MetadataConsumer.Ide -> dependencyProject.copyCommonizeCInteropForIdeTask ?: return null
            MetadataConsumer.Cli -> dependencyProject.commonizeCInteropTask ?: return null
        }

        val sourceSet = multiplatformExtension.sourceSets.findByName(sourceSetName) ?: return null
        val dependent = CInteropCommonizerDependent.from(sourceSet) ?: return null
        return commonizeCInteropTask.get().commonizedOutputLibraries(dependent)
    }
}
