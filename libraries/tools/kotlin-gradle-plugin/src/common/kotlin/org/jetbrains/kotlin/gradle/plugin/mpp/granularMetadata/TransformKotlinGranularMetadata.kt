/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataJar
import org.jetbrains.kotlin.gradle.internal.ResolvedDependencyGraph
import org.jetbrains.kotlin.gradle.internal.allResolvedDependencies
import java.io.File
import javax.inject.Inject

/**
 * Configuration cache friendly task to transform Multiplatform library dependency into sub-set of granular
 * source set dependencies that should be visible from a given Source Set
 *
 */
abstract class TransformKotlinGranularMetadata
@Inject internal constructor(
    private val settings: Settings,
    private val projectLayout: ProjectLayout
) : DefaultTask() {
    internal class Settings(
        val sourceSetName: String,
        val resolvedSourceSetMetadataDependencies: ResolvedDependencyGraph,
        val resolvedVariantDependencies: List<ResolvedDependencyGraph>,
        val resolvedHostSpecificDependencies: List<Provider<ResolvedDependencyGraph>>?,
        val projectsData: Map<String, ProjectData>,
    )

    internal class ProjectData(
        val path: String,
        val sourceSetMetadataOutputs: Provider<Map<String, FileCollection>>,
        val projectStructureMetadata: Provider<KotlinProjectStructureMetadata?>,
        val moduleId: Provider<ModuleDependencyIdentifier>
    ) {
        override fun toString(): String {
            return "ProjectData[$path]"
        }
    }

    private val moduleDependencyIdToDependency: Map<ModuleDependencyIdentifier, ResolvedDependencyResult> by lazy {
        settings
            .resolvedSourceSetMetadataDependencies
            .allResolvedDependencies
            .associateBy { it.selected.id.toModuleDependencyIdentifier() }
    }

    private val projectStructureMetadataExtractor = ProjectStructureMetadataExtractor(
        projectStructureMetadataByProjectPath = settings.projectsData.mapValues { it.value.projectStructureMetadata }
    )

    @get:OutputDirectory
    val outputsDir: File get() = projectBuildDir.resolve("kotlinSourceSetMetadata2/${settings.sourceSetName}")

    private val librariesFile: File get() = outputsDir.resolve("${settings.sourceSetName}.libraries")

    @get:Internal
    val transformedLibraries: List<File> get() = librariesFile
        .takeIf { it.exists() }
        ?.readLines()
        .orEmpty()
        .map { File(projectBuildDir, it).normalize() }

    private val projectBuildDir: File get() = projectLayout.buildDirectory.asFile.get()

    @TaskAction
    fun transform() {
        if (outputsDir.isDirectory) {
            outputsDir.deleteRecursively()
        }
        librariesFile.delete()
        outputsDir.mkdirs()

        val allExtractedKlibs = mutableListOf<File>()

        val directDependencies = settings.resolvedSourceSetMetadataDependencies
            .root
            .dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            // Filter out constraints. They look like regular dependencies
            // and only take effect on dependencies in the resolved graph.
            // Keeping them can cause incorrect results for transitive dependencies transformation
            .filterNot { it.isConstraint }

        val queue = ArrayDeque<ResolvedDependencyResult>(directDependencies)
        val visited = mutableSetOf<ResolvedDependencyResult>()

        while(queue.isNotEmpty()) {
            val dependency = queue.removeFirst()
            visited.add(dependency)

            if (!dependency.isMpp) {
                continue
            }

            val klibArtifact = settings
                .resolvedSourceSetMetadataDependencies
                .dependencyArtifacts(dependency)
                .singleOrNull()
                ?: error("Expected only one Metadata klib for dependency $dependency")
            val variants = findVariantsOf(dependency)
            val projectStructureMetadata = projectStructureMetadataFrom(klibArtifact)
            if (projectStructureMetadata == null) {
                allExtractedKlibs += settings.resolvedSourceSetMetadataDependencies.dependencyArtifacts(dependency).map { it.file }
                continue
            }

            val visibleSourceSets = inferVisibleSourceSets(projectStructureMetadata, variants)
            val extractedKlibs = extractSourceSetsMetadata(dependency, klibArtifact, projectStructureMetadata, visibleSourceSets)
            allExtractedKlibs += extractedKlibs

            // It is important that only dependencies from visible source sets are included in further transformation process
            // This covers the case when for example libB.nativeMain dependsOn libA
            // And then libC.commonMain dependsOn libB. In this case libC.commonMain should not see symbols from libA.commonMain
            queue.addAll(projectStructureMetadata.dependenciesOfSourceSets(visibleSourceSets).filterNot { it in visited })
        }

        writeToLibrariesFile(allExtractedKlibs)
    }

    private fun writeToLibrariesFile(libraries: List<File>) {
        val librariesFileContent = libraries.joinToString("\n") { it.toRelativeString(projectBuildDir) }
        librariesFile.writeText(librariesFileContent)
    }

    private fun ComponentIdentifier.toModuleDependencyIdentifier(): ModuleDependencyIdentifier {
        return when (this) {
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(group, module)
            is ProjectComponentIdentifier -> settings.projectsData[projectPath]?.moduleId?.get()
                ?: error("Cant find project Module ID by $projectPath")
            else -> error("Unknown ComponentIdentifier: $this")
        }
    }

    private fun KotlinProjectStructureMetadata.dependenciesOfSourceSets(sourceSets: Iterable<String>): List<ResolvedDependencyResult> {
        return sourceSets.flatMap { sourceSetModuleDependencies[it] ?: emptySet() }.mapNotNull { moduleDependencyIdToDependency[it] }
    }

    private fun hostSpecificMetadataArtifacts(
        componentId: ComponentIdentifier,
        projectStructureMetadata: KotlinProjectStructureMetadata
    ): Map<String, File> {
        if (projectStructureMetadata.hostSpecificSourceSets.isEmpty()) return emptyMap()

        val graph = settings.resolvedHostSpecificDependencies?.first()?.get() ?: return emptyMap()
        val dependency = graph.allResolvedDependencies.find { it.selected.id == componentId } ?: error("Dependency by $componentId not found")

        val artifacts = graph.dependencyArtifactsOrNull(dependency) ?: return emptyMap()
        val artifactFile = artifacts.singleOrNull()?.file ?: return emptyMap()

        return projectStructureMetadata.hostSpecificSourceSets.associateWith { artifactFile }
    }

    private fun ResolvedDependencyResult.requestedModuleId(): ModuleIdentifier? {
        val requestedComponent = (requested as? ModuleComponentSelector) ?: return null
        return requestedComponent.moduleIdentifier
    }

    private fun findVariantsOf(dependency: ResolvedDependencyResult): Set<String> {
        val id = dependency.selected.id
        val variants = settings
            .resolvedVariantDependencies
            .mapNotNull { dependencies -> dependencies.allResolvedDependencies.find { it.selected.id == id } }
            .map { it.resolvedVariant.displayName.let(::kotlinVariantNameFromPublishedVariantName) }

        return variants.toSet()
    }

    private fun projectStructureMetadataFrom(artifact: ResolvedArtifactResult) = projectStructureMetadataExtractor.extract(artifact)

    private fun inferVisibleSourceSets(projectStructureMetadata: KotlinProjectStructureMetadata, variants: Set<String>): Set<String> {
        return variants
            .map { projectStructureMetadata.sourceSetNamesByVariantName[it]!! }
            .takeIf { it.isNotEmpty() }
            ?.reduce { visibleSourceSets, platformSourceSets -> visibleSourceSets intersect platformSourceSets }
            ?: emptySet()
    }

    private fun extractSourceSetsMetadata(
        dependency: ResolvedDependencyResult,
        artifact: ResolvedArtifactResult,
        projectStructureMetadata: KotlinProjectStructureMetadata,
        sourceSets: Set<String>
    ): List<File> {
        val id = artifact.variant.owner
        return when {
            id is ProjectComponentIdentifier && id.build.isCurrentBuild -> {
                val projectPath = id.projectPath
                val projectData = settings.projectsData[projectPath] ?: error("Unknown project $projectPath")
                val allProjectMetadata = projectData.sourceSetMetadataOutputs.get()
                sourceSets.flatMap { allProjectMetadata[it]?.files ?: error("Can't get metadata for sourceset $it in $projectPath") }
            }
            id is ProjectComponentIdentifier && !id.build.isCurrentBuild -> {
                val moduleId = dependency.requestedModuleId() ?: error("Unknown requested module ID $dependency")
                extractSourceSetsMetadataFromJar(
                    id = moduleId,
                    dependency = dependency,
                    projectStructureMetadata = projectStructureMetadata,
                    jar = artifact.file,
                    sourceSets = sourceSets
                )
            }
            id is ModuleComponentIdentifier -> extractSourceSetsMetadataFromJar(
                id = id.moduleIdentifier,
                dependency = dependency,
                projectStructureMetadata = projectStructureMetadata,
                jar = artifact.file,
                sourceSets = sourceSets
            )
            else -> error("unknown module component identifier")
        }
    }

    private fun extractSourceSetsMetadataFromJar(
        id: ModuleIdentifier,
        dependency: ResolvedDependencyResult,
        projectStructureMetadata: KotlinProjectStructureMetadata,
        jar: File,
        sourceSets: Set<String>
    ): List<File> {
        val hostSpecificMetadataArtifactBySourceSet = hostSpecificMetadataArtifacts(dependency.selected.id, projectStructureMetadata)
        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "${id.group}-${id.name}",
            projectStructureMetadata = projectStructureMetadata,
            primaryArtifactFile = jar,
            hostSpecificArtifactsBySourceSet = hostSpecificMetadataArtifactBySourceSet
        )

        return sourceSets.map { sourceSet ->
            metadataJar.getSourceSetCompiledMetadata(sourceSet, outputsDir, materializeFile = true)
        }
    }

    private val ResolvedDependencyResult.isMpp
        get(): Boolean {
            val attributes = selected
                .variants
                .firstOrNull()
                ?.attributes
                ?: return false

            return attributes.doesContainMultiplatformAttributes
        }
}

internal val AttributeContainer.doesContainMultiplatformAttributes: Boolean get() =
    keySet().any { it.name == KotlinPlatformType.attribute.name }