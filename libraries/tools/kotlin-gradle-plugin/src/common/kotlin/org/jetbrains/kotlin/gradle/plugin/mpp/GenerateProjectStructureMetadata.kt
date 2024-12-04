/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.KotlinProjectCoordinatesData
import java.io.File
import javax.inject.Inject

internal const val SOURCE_SET_METADATA = "source-set-metadata-locations.json"
internal const val MULTIPLATFORM_PROJECT_METADATA_FILE_NAME = "kotlin-project-structure-metadata.xml"
internal const val MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME = "kotlin-project-structure-metadata.json"
internal const val EMPTY_PROJECT_STRUCTURE_METADATA_FILE_NAME = "empty-kotlin-project-structure-metadata"

@DisableCachingByDefault
abstract class GenerateProjectStructureMetadata : DefaultTask() {

    @get:Inject
    abstract internal val projectLayout: ProjectLayout

    @get:Internal
    internal lateinit var lazyKotlinProjectStructureMetadata: Lazy<KotlinProjectStructureMetadata>

    @get:Nested
    internal val kotlinProjectStructureMetadata: KotlinProjectStructureMetadata
        get() = lazyKotlinProjectStructureMetadata.value

    @get:Internal
    internal abstract val coordinatesOfProjectDependencies: MapProperty<String, KotlinProjectSharedDataProvider<KotlinProjectCoordinatesData>>

    /**
     * Gradle InputFiles view of [coordinatesOfProjectDependencies]
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val sourceSetDependenciesInputFiles: List<FileCollection>
        get() = coordinatesOfProjectDependencies
            .get()
            .values
            .map { it.files }

    @get:OutputFile
    val resultFile: File
        get() = projectLayout.buildDirectory.file(
            "kotlinProjectStructureMetadata/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME"
        ).get().asFile

    @get:OutputFile
    internal val sourceSetMetadataOutputsFile: Provider<RegularFile> =
        project.layout.buildDirectory.file("internal/kmp/$SOURCE_SET_METADATA")

    private val kmpIsolatedProjectsSupport: Boolean = project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled

    /**
     * @param projectCoordinates Should contain resolved configuration with [KotlinProjectCoordinatesData] in artifacts
     */
    private fun ResolvedDependencyResult.moduleDependencyIdentifier(
        projectCoordinates: KotlinProjectSharedDataProvider<KotlinProjectCoordinatesData>,
    ): ModuleDependencyIdentifier = when (selected.id) {
        is ProjectComponentIdentifier -> tryReadFromKotlinProjectCoordinatesData(projectCoordinates)
            ?: selected.moduleDependencyIdentifier()
        is ModuleComponentIdentifier -> selected.moduleDependencyIdentifier()
        else -> error("Unknown ComponentIdentifier: $selected")
    }

    private fun ResolvedDependencyResult.tryReadFromKotlinProjectCoordinatesData(
        projectCoordinates: KotlinProjectSharedDataProvider<KotlinProjectCoordinatesData>,
    ): ModuleDependencyIdentifier? = projectCoordinates.getProjectDataFromDependencyOrNull(this)?.moduleId

    private fun ResolvedComponentResult.moduleDependencyIdentifier() = ModuleDependencyIdentifier(
        groupId = moduleVersion?.group,
        moduleId = moduleVersion?.name ?: "unspecified".also { logger.warn("[Kotlin] ComponentResult $this has no name") }
    )

    @TaskAction
    fun generateMetadataXml() {
        resultFile.parentFile.mkdirs()

        val actualProjectStructureMetadata = if (kmpIsolatedProjectsSupport) {
            kotlinProjectStructureMetadata.copy(
                sourceSetModuleDependencies = coordinatesOfProjectDependencies.get().mapValues { (_, resolvedProjectCoordinates) ->
                    val directDependencies = resolvedProjectCoordinates.rootComponent.dependencies
                    val result = mutableSetOf<ModuleDependencyIdentifier>()
                    for (dependency in directDependencies) {
                        if (dependency.isConstraint) continue
                        if (dependency !is ResolvedDependencyResult) continue

                        result.add(dependency.moduleDependencyIdentifier(resolvedProjectCoordinates))
                    }

                    result
                }
            )
        } else {
            kotlinProjectStructureMetadata
        }

        val resultString = actualProjectStructureMetadata.toJson()
        resultFile.writeText(resultString)
    }
}

